package dev.sqwat.vela.ui.input;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.function.LongSupplier;

/**
 * Multiline text for the GLSL editor: caret and selection by UTF-16 offset, lines by '\n', four-space indents (tabs
 * become spaces on input), auto-indent after '{', '}' that dedents itself, block indent and comment toggles, and undo
 * where a run of typing is one step. Pure logic; the screen draws it and maps keys to these calls.
 */
public final class CodeBuffer {
    public static final String INDENT="    ";
    private static final int UNDO_LIMIT=200;
    private static final long MERGE_MILLIS=1000;

    private record State(String text,int caret,int anchor){}
    private enum Edit{TYPE,ERASE,OTHER}

    private final int limit;
    private final LongSupplier clock;
    private String text="";
    private int caret,anchor;
    private int preferredColumn=-1;
    private int[] lineStarts={0};
    private int lineCount=1;
    private final ArrayDeque<State> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private Edit lastEdit;
    private long lastEditAt;
    private long caretChangedAt;
    private int revision;

    public CodeBuffer(int limit){this(limit,System::currentTimeMillis);}
    CodeBuffer(int limit,LongSupplier clock){this.limit=limit;this.clock=clock;caretChangedAt=clock.getAsLong();}

    /** Replaces everything and forgets the history (opening a draft). */
    public void set(String value){
        text=clip(normalize(value));caret=anchor=0;undo.clear();redo.clear();lastEdit=null;reindex();touch();
    }
    /** Replaces everything as one undoable step (reset to the example). */
    public void replaceAll(String value){
        remember(Edit.OTHER);text=clip(normalize(value));caret=anchor=0;reindex();touch();
    }
    static String normalize(String value){return value==null?"":value.replace("\r\n","\n").replace('\r','\n').replace("\t",INDENT);}
    private String clip(String value){return value.length()>limit?value.substring(0,limit):value;}

    public String text(){return text;}
    public int caret(){return caret;}
    public int anchor(){return anchor;}
    public int start(){return Math.min(caret,anchor);}
    public int end(){return Math.max(caret,anchor);}
    public boolean hasSelection(){return caret!=anchor;}
    public String selected(){return text.substring(start(),end());}
    /** Changes on every edit; lets the screen cache per-line work. */
    public int revision(){return revision;}
    public boolean caretVisible(){return (clock.getAsLong()-caretChangedAt)%1060<620;}

    public int lineCount(){return lineCount;}
    public int lineStart(int line){return lineStarts[line];}
    public int lineEnd(int line){return line+1<lineCount?lineStarts[line+1]-1:text.length();}
    public String line(int line){return text.substring(lineStart(line),lineEnd(line));}
    public int lineOf(int offset){
        int found=Arrays.binarySearch(lineStarts,0,lineCount,offset);
        return found>=0?found:-found-2;
    }
    public int columnOf(int offset){return offset-lineStart(lineOf(offset));}
    public int caretLine(){return lineOf(caret);}
    public int caretColumn(){return columnOf(caret);}

    private void reindex(){
        int count=1;
        for(int i=0;i<text.length();i++)if(text.charAt(i)=='\n')count++;
        if(lineStarts.length<count)lineStarts=new int[Math.max(count,lineStarts.length*2)];
        lineStarts[0]=0;int line=1;
        for(int i=0;i<text.length();i++)if(text.charAt(i)=='\n')lineStarts[line++]=i+1;
        lineCount=count;revision++;
    }
    private void touch(){caretChangedAt=clock.getAsLong();}

    // Caret movement.

    public void move(int position,boolean extend){
        caret=Math.max(0,Math.min(text.length(),position));
        if(caret>0&&caret<text.length()&&Character.isLowSurrogate(text.charAt(caret)))caret--;
        if(!extend)anchor=caret;
        preferredColumn=-1;lastEdit=null;touch();
    }
    public void moveTo(int line,int column,boolean extend){
        line=Math.max(0,Math.min(lineCount-1,line));
        move(lineStart(line)+Math.max(0,Math.min(column,lineEnd(line)-lineStart(line))),extend);
    }
    public void horizontal(int direction,boolean extend,boolean word){
        if(!extend&&hasSelection()&&!word){move(direction<0?start():end(),false);return;}
        int target=caret;
        if(word)target=direction<0?wordLeft(caret):wordRight(caret);
        else if(direction<0&&caret>0)target=text.offsetByCodePoints(caret,-1);
        else if(direction>0&&caret<text.length())target=text.offsetByCodePoints(caret,1);
        move(target,extend);
    }
    /** Up/down by {@code lines}, keeping the column the caret had before the first vertical move. */
    public void vertical(int lines,boolean extend){
        int column=preferredColumn>=0?preferredColumn:caretColumn();
        int line=caretLine()+lines;
        if(line<0)move(0,extend);
        else if(line>=lineCount)move(text.length(),extend);
        else moveTo(line,column,extend);
        preferredColumn=column;
    }
    /** Home goes to the first non-space character, and from there to the line start. */
    public void home(boolean extend){
        int line=caretLine(),begin=lineStart(line),first=begin+leading(line(line)).length();
        move(caret==first?begin:first,extend);
    }
    public void end(boolean extend){move(lineEnd(caretLine()),extend);}
    public void selectAll(){anchor=0;caret=text.length();preferredColumn=-1;lastEdit=null;touch();}
    public void selectWord(){
        if(text.isEmpty())return;
        int at=Math.min(caret,text.length()-1);
        if(caret>0&&(caret==text.length()||!identifier(text.charAt(caret)))&&identifier(text.charAt(caret-1)))at=caret-1;
        if(!identifier(text.charAt(at))){anchor=at;caret=at+1;touch();return;}
        int left=at,right=at;
        while(left>0&&identifier(text.charAt(left-1)))left--;
        while(right<text.length()&&identifier(text.charAt(right)))right++;
        anchor=left;caret=right;preferredColumn=-1;lastEdit=null;touch();
    }
    public void selectLine(int line){
        line=Math.max(0,Math.min(lineCount-1,line));
        anchor=lineStart(line);caret=line+1<lineCount?lineStart(line+1):text.length();lastEdit=null;touch();
    }

    private static boolean identifier(char c){return Character.isLetterOrDigit(c)||c=='_';}
    private int wordLeft(int from){
        int i=from;
        while(i>0&&text.charAt(i-1)==' ')i--;
        if(i>0&&identifier(text.charAt(i-1))){while(i>0&&identifier(text.charAt(i-1)))i--;}
        else if(i>0&&i==from)i--;
        return i;
    }
    private int wordRight(int from){
        int i=from,n=text.length();
        if(i<n&&identifier(text.charAt(i))){while(i<n&&identifier(text.charAt(i)))i++;}
        else if(i<n&&text.charAt(i)!=' ')i++;
        while(i<n&&text.charAt(i)==' ')i++;
        return i;
    }
    private static String leading(String line){int i=0;while(i<line.length()&&line.charAt(i)==' ')i++;return line.substring(0,i);}

    // Editing.

    private void remember(Edit kind){
        long now=clock.getAsLong();
        boolean merge=kind!=Edit.OTHER&&kind==lastEdit&&now-lastEditAt<MERGE_MILLIS&&!undo.isEmpty();
        if(!merge){undo.push(new State(text,caret,anchor));while(undo.size()>UNDO_LIMIT)undo.removeLast();}
        redo.clear();lastEdit=kind;lastEditAt=now;
    }
    private void replace(int from,int to,String value,int newCaret){
        text=text.substring(0,from)+value+text.substring(to);
        caret=anchor=Math.max(0,Math.min(text.length(),newCaret));
        preferredColumn=-1;reindex();touch();
    }
    private int room(){return Math.max(0,limit-(text.length()-(end()-start())));}

    /** Pastes or inserts text in place of the selection; returns false when nothing fit. */
    public boolean insert(String value){
        value=normalize(value);
        int room=room();
        if(value.length()>room)value=value.substring(0,room);
        if(!value.isEmpty()&&Character.isHighSurrogate(value.charAt(value.length()-1)))value=value.substring(0,value.length()-1);
        if(value.isEmpty()&&!hasSelection())return false;
        remember(Edit.OTHER);
        int at=start();replace(at,end(),value,at+value.length());
        lastEdit=null;
        return true;
    }
    /** A typed character. A '}' typed into a line's indent closes one level. */
    public void type(int codepoint){
        String value=Character.toString(codepoint=='\t'?' ':codepoint);
        if(room()<value.length())return;
        remember(Edit.TYPE);
        int from=start(),to=end();
        if(codepoint=='}'&&from==to){
            int begin=lineStart(lineOf(from));
            String before=text.substring(begin,from);
            if(before.length()>=INDENT.length()&&before.isBlank())from-=INDENT.length();
        }
        replace(from,to,value,from+value.length());
    }
    /** Enter: keeps the line's indent, adds one after '{', and splits "{}" into an indented block. */
    public void newline(){
        int line=lineOf(start()),begin=lineStart(line);
        String indent=leading(text.substring(begin,start()));
        String before=text.substring(begin,start()).stripTrailing();
        boolean opens=before.endsWith("{")||before.endsWith("(");
        boolean closes=end()<text.length()&&(text.charAt(end())=='}'||text.charAt(end())==')');
        String inner="\n"+indent+(opens?INDENT:"");
        String value=opens&&closes?inner+"\n"+indent:inner;
        if(room()<value.length())return;
        remember(Edit.OTHER);
        int at=start();replace(at,end(),value,at+inner.length());
    }
    /** Tab: spaces to the next indent stop, or (with a multi-line selection or Shift) indent/outdent the lines. */
    public void tab(boolean outdent){
        int first=lineOf(start()),last=lineOf(Math.max(start(),end()-(hasSelection()&&end()==lineStart(lineOf(end()))?1:0)));
        if(!outdent&&first==last&&!(hasSelection()&&start()==lineStart(first)&&end()>=lineEnd(last))){
            int spaces=INDENT.length()-columnOf(start())%INDENT.length();
            if(room()<spaces)return;
            remember(Edit.OTHER);int at=start();replace(at,end(),INDENT.substring(0,spaces),at+spaces);return;
        }
        remember(Edit.OTHER);
        int selStart=start(),selEnd=end(),caretAt=caret;
        boolean caretFirst=caret<anchor;
        StringBuilder out=new StringBuilder(text.length()+INDENT.length()*(last-first+1));
        out.append(text,0,lineStart(first));
        int shiftStart=0,shiftEnd=0;
        for(int line=first;line<=last;line++){
            String content=line(line);
            int delta;
            if(outdent){int remove=Math.min(INDENT.length(),leading(content).length());content=content.substring(remove);delta=-remove;}
            else if(content.isEmpty()){delta=0;}
            else{content=INDENT+content;delta=INDENT.length();}
            out.append(content);
            if(line<last||lineEnd(last)<text.length())out.append('\n');
            if(line==first)shiftStart=Math.max(delta,-(selStart-lineStart(first)));
            shiftEnd+=delta;
        }
        if(lineEnd(last)<text.length())out.append(text,lineEnd(last)+1,text.length());
        if(out.length()>limit){undo.pop();return;}
        text=out.toString();reindex();
        int newStart=Math.max(lineStart(first),selStart+shiftStart),newEnd=Math.max(newStart,selEnd+shiftEnd);
        if(selStart==selEnd){caret=anchor=Math.max(lineStart(first),caretAt+shiftStart);}
        else if(caretFirst){caret=newStart;anchor=newEnd;}else{anchor=newStart;caret=newEnd;}
        preferredColumn=-1;lastEdit=null;touch();
    }
    /** Backspace. In a line's indent it removes back to the previous indent stop. */
    public void backspace(boolean word){
        if(hasSelection()){remember(Edit.OTHER);int at=start();replace(at,end(),"",at);lastEdit=null;return;}
        if(caret==0)return;
        int from;
        if(word)from=wordLeft(caret);
        else{
            int begin=lineStart(caretLine());
            String before=text.substring(begin,caret);
            if(!before.isEmpty()&&before.isBlank()){int column=before.length();from=caret-((column-1)%INDENT.length()+1);}
            else from=text.offsetByCodePoints(caret,-1);
        }
        remember(Edit.ERASE);replace(from,caret,"",from);
    }
    public void delete(boolean word){
        if(hasSelection()){remember(Edit.OTHER);int at=start();replace(at,end(),"",at);lastEdit=null;return;}
        if(caret>=text.length())return;
        int to=word?wordRight(caret):text.offsetByCodePoints(caret,1);
        remember(Edit.ERASE);replace(caret,to,"",caret);
    }
    /** Ctrl+D: copies the current line (or the selected lines) below. */
    public void duplicateLines(){
        int first=lineOf(start()),last=lineOf(end());
        String block=text.substring(lineStart(first),lineEnd(last));
        if(room()<block.length()+1)return;
        remember(Edit.OTHER);
        int at=lineEnd(last),offset=caret-lineStart(first),span=end()-start();
        text=text.substring(0,at)+"\n"+block+text.substring(at);reindex();
        int base=at+1;
        if(span==0)caret=anchor=base+offset;else{anchor=base+(start()-lineStart(first));caret=anchor+span;}
        lastEdit=null;touch();
    }
    /** Ctrl+/: comments out the lines with "// ", or uncomments them when all already are. */
    public void toggleComment(){
        int first=lineOf(start()),last=lineOf(Math.max(start(),end()-(hasSelection()&&end()==lineStart(lineOf(end()))?1:0)));
        boolean all=true;
        for(int line=first;line<=last;line++){String c=line(line);if(!c.isBlank()&&!c.stripLeading().startsWith("//"))all=false;}
        int indent=Integer.MAX_VALUE;
        for(int line=first;line<=last;line++){String c=line(line);if(!c.isBlank())indent=Math.min(indent,leading(c).length());}
        if(indent==Integer.MAX_VALUE)return;
        remember(Edit.OTHER);
        StringBuilder out=new StringBuilder(text.length()+3*(last-first+1));
        out.append(text,0,lineStart(first));
        int caretShift=0;
        for(int line=first;line<=last;line++){
            String c=line(line);
            if(c.isBlank()){out.append(c);}
            else if(all){
                int at=c.indexOf("//");int cut=c.startsWith("// ",at)?3:2;out.append(c,0,at).append(c.substring(at+cut));
                if(line==first)caretShift=-cut;
            }else{out.append(c,0,indent).append("// ").append(c.substring(indent));if(line==first)caretShift=3;}
            if(line<last||lineEnd(last)<text.length())out.append('\n');
        }
        if(lineEnd(last)<text.length())out.append(text,lineEnd(last)+1,text.length());
        if(out.length()>limit){undo.pop();return;}
        int oldCaret=caret,oldAnchor=anchor,firstStart=lineStart(first);
        int delta=out.length()-text.length();
        text=out.toString();reindex();
        if(oldCaret==oldAnchor){caret=anchor=Math.max(firstStart,Math.min(text.length(),oldCaret+caretShift));}
        else if(oldCaret<oldAnchor){caret=Math.max(firstStart,oldCaret+caretShift);anchor=Math.min(text.length(),oldAnchor+delta);}
        else{anchor=Math.max(firstStart,oldAnchor+caretShift);caret=Math.min(text.length(),oldCaret+delta);}
        lastEdit=null;touch();
    }
    public boolean undo(){return restore(undo,redo);}
    public boolean redo(){return restore(redo,undo);}
    private boolean restore(ArrayDeque<State> from,ArrayDeque<State> to){
        if(from.isEmpty())return false;
        to.push(new State(text,caret,anchor));
        State state=from.pop();
        text=state.text();caret=state.caret();anchor=state.anchor();
        preferredColumn=-1;lastEdit=null;reindex();touch();
        return true;
    }
}
