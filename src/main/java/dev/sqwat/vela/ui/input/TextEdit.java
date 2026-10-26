package dev.sqwat.vela.ui.input;

/** UTF-16 positions always move along Unicode code points. */
public final class TextEdit {
    public static String cleanPaste(String value){
        StringBuilder clean=new StringBuilder();value.codePoints().filter(cp->!Character.isISOControl(cp)).forEach(clean::appendCodePoint);return clean.toString();
    }
    private String text="";
    private int caret,anchor;
    private long caretChangedAt=System.currentTimeMillis();
    public boolean caretVisible(){return (System.currentTimeMillis()-caretChangedAt)%1100<650;}
    public void selectWord(){
        int left=caret,right=caret;
        while(left>0&&!Character.isWhitespace(text.codePointBefore(left)))left=text.offsetByCodePoints(left,-1);
        while(right<text.length()&&!Character.isWhitespace(text.codePointAt(right)))right=text.offsetByCodePoints(right,1);
        anchor=left;caret=right;caretChangedAt=System.currentTimeMillis();
    }
    public String text(){return text;}
    public int caret(){return caret;}
    public int start(){return Math.min(caret,anchor);}
    public int end(){return Math.max(caret,anchor);}
    public String selected(){return text.substring(start(),end());}
    public void set(String value){if(!text.equals(value)){text=value;caret=anchor=text.length();}}
    public void selectAll(){anchor=0;caret=text.length();}
    public void move(int position,boolean extend){caret=Math.max(0,Math.min(text.length(),position));if(caret>0&&caret<text.length()&&Character.isLowSurrogate(text.charAt(caret)))caret--;if(!extend)anchor=caret;caretChangedAt=System.currentTimeMillis();}
    public void arrow(int direction,boolean extend){if(!extend&&start()!=end()){move(direction<0?start():end(),false);return;}move(text.offsetByCodePoints(caret,direction<0?(caret>0?-1:0):(caret<text.length()?1:0)),extend);}
    public void insert(String value,int limit){int room=Math.max(0,limit-(text.length()-(end()-start())));if(value.length()>room)value=value.substring(0,room);if(!value.isEmpty()&&Character.isHighSurrogate(value.charAt(value.length()-1)))value=value.substring(0,value.length()-1);int at=start();text=text.substring(0,at)+value+text.substring(end());caret=anchor=at+value.length();caretChangedAt=System.currentTimeMillis();}
    public void erase(boolean forward){if(start()!=end()){insert("",Integer.MAX_VALUE);return;}if(forward){if(caret<text.length()){anchor=text.offsetByCodePoints(caret,1);insert("",Integer.MAX_VALUE);}}else if(caret>0){anchor=text.offsetByCodePoints(caret,-1);insert("",Integer.MAX_VALUE);}}
    public void eraseWord(){if(start()!=end()){erase(false);return;}int from=caret;while(from>0&&Character.isWhitespace(text.codePointBefore(from)))from=text.offsetByCodePoints(from,-1);while(from>0&&!Character.isWhitespace(text.codePointBefore(from)))from=text.offsetByCodePoints(from,-1);anchor=from;insert("",Integer.MAX_VALUE);}
}
