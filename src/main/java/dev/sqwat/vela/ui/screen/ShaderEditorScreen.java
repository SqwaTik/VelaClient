package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.module.visual.Shaders;
import dev.sqwat.vela.render.shader.CustomSkyProgram;
import dev.sqwat.vela.render.shader.CustomSkySource;
import dev.sqwat.vela.ui.animation.FrameClock;
import dev.sqwat.vela.ui.animation.Motion;
import dev.sqwat.vela.ui.input.CodeBuffer;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * The sky GLSL editor, drawn entirely in Vela's own Inter text: a gutter with line numbers, GLSL colouring, the current
 * line, compile errors marked on their lines, and the editing keys of a code editor (see {@link CodeBuffer}). Code
 * compiles only on Apply; a failed compile never replaces the working shader.
 */
public final class ShaderEditorScreen extends VelaScreen {
    private record Action(Rect rect,Runnable run){}
    private static final float CODE=10f,NUMBERS=8.5f;
    private static final int LINE=15;
    private static final VelaDraw.Weight FACE=VelaDraw.Weight.REGULAR;

    private final VelaScreen parent;
    private final VelaServices services;
    private final Shaders settings;
    private final UiControls controls=new UiControls();
    private final FrameClock clock=new FrameClock();
    private final CodeBuffer buffer=new CodeBuffer(CustomSkySource.LIMIT);
    private final List<Action> actions=new ArrayList<>();
    private List<GlslSyntax.Line> syntax=List.of();
    private float[][] offsets=new float[0][];
    private int syntaxRevision=-1;
    private String status="Код запускается только по кнопке «Применить».",log="";
    private boolean error,closing,selecting,draggingScroll;
    private BitSet errorLines=new BitSet();
    private float reveal,scroll,targetScroll,scrollX;
    private boolean followCaret=true;
    private Rect code=new Rect(0,0,0,0),gutter=code,scrollTrack=code,scrollThumb=code;
    private double scrollGrab;
    private int gutterWidth;

    public ShaderEditorScreen(VelaScreen parent,VelaServices services,Shaders settings){
        super(Component.literal(dev.sqwat.vela.ui.Lang.tr("Редактор GLSL")));this.parent=parent;this.services=services;this.settings=settings;
        buffer.set(settings.editorDraft());
    }
    @Override protected float interfaceScale(){return services.configs().current().uiScale;}

    private boolean applied(){return buffer.text().equals(settings.skyCode())&&CustomSkyProgram.accepted(buffer.text());}

    private void refreshSyntax(){
        if(syntaxRevision==buffer.revision())return;
        int count=buffer.lineCount();
        var lines=new ArrayList<GlslSyntax.Line>(count);
        offsets=new float[count][];
        boolean comment=false;
        for(int i=0;i<count;i++){var line=GlslSyntax.line(buffer.line(i),comment);lines.add(line);comment=line.inComment();}
        syntax=lines;syntaxRevision=buffer.revision();
    }
    private float[] offsets(int line){
        float[] cached=offsets[line];
        if(cached==null)cached=offsets[line]=VelaDraw.literalOffsets(buffer.line(line),FACE,CODE);
        return cached;
    }
    private static int color(GlslSyntax.Kind kind){
        return switch(kind){
            case KEYWORD->0xFFC4A7FF;
            case TYPE->VelaTheme.CYAN;
            case BUILTIN->0xFF8FB8FF;
            case EDITOR->VelaTheme.POSITIVE;
            case NUMBER->0xFFFFBE7A;
            case COMMENT->0xFF66728A;
            case PREPROCESSOR->0xFFFF93C4;
            case PUNCTUATION->VelaTheme.TEXT_SECONDARY;
            case PLAIN->VelaTheme.TEXT;
        };
    }

    @Override protected void renderVela(GuiGraphicsExtractor g,int mx,int my,float delta){
        parent.renderAsBackdrop(g,width,height,delta);
        float dt=clock.step();reveal=Motion.approach(reveal,closing?0:1,18,dt);
        if(closing&&reveal<.01f){minecraft.gui.setScreen(parent);return;}
        refreshSyntax();
        int w=Math.max(300,Math.min(760,width-24)),h=Math.max(220,Math.min(480,height-16)),x=(width-w)/2,y=(height-h)/2;
        g.nextStratum();g.fill(0,0,width,height,(Math.round(0xC0*reveal)<<24)|0x05070A);
        g.pose().pushMatrix();g.pose().translate(0,(1-reveal)*8);
        actions.clear();
        UiClip.enable(g,x-1,y-1,x+w+1,y+h+1);
        VelaDraw.roundedOutline(g,x,y,w,h,10,VelaTheme.BORDER,VelaTheme.WINDOW);

        // Header: icon tile, title, what is being edited, the compile state and close.
        VelaDraw.roundedRect(g,x+12,y+11,22,22,6,VelaTheme.PRIMARY_SOFT);
        VelaIcons.draw(g,"visuals",x+17,y+16,12,VelaTheme.PRIMARY);
        VelaDraw.text(g,"Редактор GLSL",x+42,y+11,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,10);
        VelaDraw.text(g,"Небо · void mainImage(out vec4 color, in vec2 pixel)",x+42,y+24,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7.5f);
        Rect close=new Rect(x+w-30,y+12,20,20);controls.iconButton(g,"shader-close",close,"x",false,close.contains(mx,my));actions.add(new Action(close,this::onClose));
        String state=error?"Ошибка":applied()?"Применён":"Не применён";
        int stateColor=error?VelaTheme.DANGER:applied()?VelaTheme.POSITIVE:VelaTheme.TEXT_SECONDARY;
        int pillWidth=Math.round(VelaDraw.textWidth(state,VelaDraw.Weight.MEDIUM,7.5f))+22;
        Rect pill=new Rect(close.x()-8-pillWidth,y+14,pillWidth,16);
        VelaDraw.roundedRect(g,pill.x(),pill.y(),pill.width(),pill.height(),8,(stateColor&0xFFFFFF)|0x1E000000);
        VelaDraw.roundedRect(g,pill.x()+7,pill.y()+6,4,4,2,stateColor);
        VelaDraw.text(g,state,pill.x()+15,pill.y()+4,stateColor,VelaDraw.Weight.MEDIUM,7.5f);
        VelaDraw.hairline(g,x,y+43,w,VelaTheme.BORDER);

        // Code area.
        Rect editor=new Rect(x+12,y+54,w-24,Math.max(60,h-54-76));
        drawEditor(g,editor,mx,my,dt);

        // Caret position under the code, on the right.
        int infoY=editor.y()+editor.height()+8;
        String position=dev.sqwat.vela.ui.Lang.pick("Ln ","Стр ")+(buffer.caretLine()+1)+dev.sqwat.vela.ui.Lang.pick(", Col ",", Кол ")+(buffer.caretColumn()+1)
            +"  ·  "+buffer.text().length()+" / "+CustomSkySource.LIMIT;
        VelaDraw.literalText(g,position,x+w-12-VelaDraw.literalWidth(position,VelaDraw.Weight.REGULAR,7.5f),infoY+4,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7.5f);

        // Footer: status on the left, actions on the right.
        int by=y+h-30;
        String applyLabel="Применить";
        int aw=Math.round(VelaDraw.textWidth(applyLabel,VelaDraw.Weight.SEMIBOLD,8.5f)+VelaDraw.literalWidth("Ctrl+Enter",VelaDraw.Weight.REGULAR,7))+36;
        Rect apply=new Rect(x+w-12-aw,by,aw,20);
        float ah=controls.animate("apply-hover",apply.contains(mx,my)?1:0);
        VelaDraw.roundedRect(g,apply.x(),apply.y(),apply.width(),apply.height(),6,VelaTheme.mix(VelaTheme.PRIMARY,0xFFFFFFFF,ah*.12f));
        VelaDraw.text(g,applyLabel,apply.x()+12,apply.y()+6,VelaTheme.onPrimary(),VelaDraw.Weight.SEMIBOLD,8.5f);
        VelaDraw.literalText(g,"Ctrl+Enter",apply.x()+apply.width()-12-VelaDraw.literalWidth("Ctrl+Enter",VelaDraw.Weight.REGULAR,7),apply.y()+7,(VelaTheme.onPrimary()&0xFFFFFF)|0xA0000000,VelaDraw.Weight.REGULAR,7);
        actions.add(new Action(apply,this::apply));
        int left=apply.x()-6;
        if(error)left=secondary(g,left,by,"Копировать ошибку",mx,my,()->minecraft.keyboardHandler.setClipboard(log));
        int statusWidth=left-8-(x+12);
        var lines=UiKit.wrap(status,statusWidth,VelaDraw.Weight.REGULAR,7.5f);
        int statusColor=error?0xFFFF8793:VelaTheme.TEXT_SECONDARY;
        if(lines.size()==1)VelaDraw.text(g,lines.get(0),x+12,by+6,statusColor,VelaDraw.Weight.REGULAR,7.5f);
        else for(int i=0;i<Math.min(2,lines.size());i++){
            String text=lines.get(i);
            if(i==1&&lines.size()>2)text=VelaDraw.ellipsize(text+" …",statusWidth,VelaDraw.Weight.REGULAR,7.5f);
            VelaDraw.text(g,text,x+12,by+1+i*10,statusColor,VelaDraw.Weight.REGULAR,7.5f);
        }
        g.disableScissor();
        g.pose().popMatrix();
    }

    private int secondary(GuiGraphicsExtractor g,int right,int y,String label,int mx,int my,Runnable run){
        int bw=Math.round(VelaDraw.textWidth(label,VelaDraw.Weight.MEDIUM,8.5f))+24;
        Rect r=new Rect(right-bw,y,bw,20);
        float hover=controls.animate("button-"+label,r.contains(mx,my)?1:0);
        VelaDraw.roundedOutline(g,r.x(),r.y(),r.width(),r.height(),6,VelaTheme.mix(VelaTheme.BORDER,VelaTheme.BORDER_HOVER,hover),VelaTheme.mix(VelaTheme.SURFACE_DEEP,VelaTheme.SURFACE_HOVER,hover));
        VelaDraw.centeredText(g,label,r.x()+r.width()/2f,r.y()+6,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8.5f);
        actions.add(new Action(r,run));
        return r.x()-6;
    }

    private void drawEditor(GuiGraphicsExtractor g,Rect editor,int mx,int my,float dt){
        int count=buffer.lineCount();
        String widest=Integer.toString(Math.max(99,count));
        gutterWidth=Math.round(VelaDraw.literalWidth(widest,FACE,NUMBERS))+20;
        VelaDraw.roundedOutline(g,editor.x(),editor.y(),editor.width(),editor.height(),7,VelaTheme.BORDER,VelaTheme.SURFACE_DEEP);
        gutter=new Rect(editor.x()+1,editor.y()+1,gutterWidth,editor.height()-2);
        code=new Rect(gutter.x()+gutterWidth+8,editor.y()+6,editor.width()-gutterWidth-8-14,editor.height()-12);
        g.fill(gutter.x()+gutterWidth,editor.y()+1,gutter.x()+gutterWidth+1,editor.y()+editor.height()-1,0xFF1A2130);

        int caretLine=buffer.caretLine();
        int content=count*LINE;
        int limit=Math.max(0,content-code.height()+LINE/2);
        if(followCaret){
            int top=caretLine*LINE,bottom=top+LINE;
            if(top<targetScroll)targetScroll=top;
            if(bottom>targetScroll+code.height())targetScroll=bottom-code.height();
            float caretX=offsets(caretLine)[Math.min(buffer.caretColumn(),buffer.line(caretLine).length())];
            if(caretX<scrollX)scrollX=Math.max(0,caretX-24);
            if(caretX>scrollX+code.width()-6)scrollX=caretX-code.width()+30;
            followCaret=false;
        }
        targetScroll=Math.clamp(targetScroll,0,limit);
        scroll=draggingScroll?targetScroll:Motion.scroll(Math.clamp(scroll,0,limit),targetScroll,dt);

        int first=Math.max(0,(int)Math.floor(scroll/LINE)),last=Math.min(count-1,(int)Math.ceil((scroll+code.height())/LINE));
        boolean selection=buffer.hasSelection();
        int selStart=buffer.start(),selEnd=buffer.end();

        // Row backgrounds span gutter and code: the current line and lines with compile errors.
        UiClip.enable(g,editor.x()+1,code.y(),editor.x()+editor.width()-1,code.y()+code.height());
        for(int line=first;line<=last;line++){
            float rowY=code.y()+line*LINE-scroll;
            if(errorLines.get(line)){
                g.fill(editor.x()+1,Math.round(rowY),editor.x()+editor.width()-1,Math.round(rowY)+LINE,0x1FFF6178);
                g.fill(editor.x()+1,Math.round(rowY),editor.x()+3,Math.round(rowY)+LINE,VelaTheme.DANGER);
            }else if(line==caretLine&&!selection)g.fill(editor.x()+1,Math.round(rowY),editor.x()+editor.width()-1,Math.round(rowY)+LINE,0x0CFFFFFF);
            String number=Integer.toString(line+1);
            int numberColor=errorLines.get(line)?VelaTheme.DANGER:line==caretLine?VelaTheme.TEXT:0xFF4A5468;
            VelaDraw.literalText(g,number,gutter.x()+gutterWidth-8-VelaDraw.literalWidth(number,FACE,NUMBERS),rowY+3.5f,numberColor,FACE,NUMBERS);
        }
        g.disableScissor();

        UiClip.enable(g,code.x()-2,code.y(),code.x()+code.width(),code.y()+code.height());
        float originX=code.x()-scrollX;
        int selectionColor=(VelaTheme.PRIMARY&0xFFFFFF)|0x55000000;
        for(int line=first;line<=last;line++){
            float rowY=code.y()+line*LINE-scroll;
            String text=buffer.line(line);
            float[] at=offsets(line);
            int begin=buffer.lineStart(line),end=begin+text.length();
            if(selection&&selStart<=end&&selEnd>begin){
                int a=Math.max(selStart,begin)-begin,b=Math.min(selEnd,end)-begin;
                float x0=originX+at[a],x1=originX+at[b]+(selEnd>end?4:0);
                if(x1>x0)g.fill(Math.round(x0),Math.round(rowY),Math.round(x1),Math.round(rowY)+LINE,selectionColor);
            }
            var spans=line<syntax.size()?syntax.get(line).spans():List.<GlslSyntax.Span>of();
            for(var span:spans){
                float sx=originX+at[span.start()];
                if(sx>code.x()+code.width())break;
                if(originX+at[span.end()]<code.x()-2)continue;
                VelaDraw.literalText(g,text.substring(span.start(),span.end()),sx,rowY+2.5f,color(span.kind()),FACE,CODE);
            }
        }
        if(buffer.caretVisible()&&!closing){
            float caretX=originX+offsets(caretLine)[Math.min(buffer.caretColumn(),buffer.line(caretLine).length())];
            float caretY=code.y()+caretLine*LINE-scroll;
            g.fill(Math.round(caretX),Math.round(caretY)+1,Math.round(caretX)+1,Math.round(caretY)+LINE-1,VelaTheme.PRIMARY);
        }
        g.disableScissor();

        scrollTrack=new Rect(editor.x()+editor.width()-9,editor.y()+5,6,editor.height()-10);
        if(limit>0){
            int thumb=Math.max(18,scrollTrack.height()*code.height()/Math.max(1,content));
            scrollThumb=new Rect(scrollTrack.x(),scrollTrack.y()+Math.round((scrollTrack.height()-thumb)*scroll/limit),scrollTrack.width(),thumb);
            float hover=controls.animate("editor-scroll",scrollTrack.contains(mx,my)||draggingScroll?1:0);
            VelaDraw.roundedRect(g,scrollThumb.x()+2,scrollThumb.y(),2,thumb,1,VelaTheme.mix(0xFF3A4458,VelaTheme.TEXT_MUTED,hover));
        }else scrollThumb=new Rect(0,0,0,0);
    }

    /** Buffer offset under a point of the code area. */
    private int offsetAt(double px,double py){
        int line=Math.max(0,Math.min(buffer.lineCount()-1,(int)Math.floor((py-code.y()+scroll)/LINE)));
        float[] at=offsets(line);
        double target=px-code.x()+scrollX;
        int column=0;
        while(column<at.length-1&&(at[column]+at[column+1])/2<target)column++;
        return buffer.lineStart(line)+column;
    }

    private void apply(){
        String source=buffer.text();
        String result=CustomSkyProgram.apply(source);
        error=result!=null;
        if(error){
            log=result;errorLines=GlslSyntax.errorLines(result,buffer.lineCount());
            String first=result.lines().filter(l->!l.isBlank()).findFirst().orElse(result).strip();
            int line=errorLines.nextSetBit(0);
            status=(line>=0?dev.sqwat.vela.ui.Lang.pick("Line ","Строка ")+(line+1)+": ":"")+first;
            if(line>=0){targetScroll=Math.max(0,line*LINE-code.height()/2f);}
            return;
        }
        log="";errorLines=new BitSet();
        settings.skyCode(source);settings.editorDraft(source);settings.useCustomSky();services.configs().requestSave();
        status="Применено. Ошибки нового кода не заменяют рабочий шейдер.";
    }

    @Override protected boolean clickVela(MouseButtonEvent e,boolean doubled){
        if(closing||reveal<.9f)return true;
        if(e.button()!=0)return true;
        if(scrollThumb.width()>0&&scrollTrack.contains(e.x(),e.y())){
            draggingScroll=true;boolean grabbed=scrollThumb.contains(e.x(),e.y());
            scrollGrab=grabbed?e.y()-scrollThumb.y():scrollThumb.height()/2d;if(grabbed)targetScroll=scroll;else scrollTo(e.y());return true;
        }
        Rect area=new Rect(gutter.x(),code.y(),code.x()+code.width()-gutter.x(),code.height());
        if(area.contains(e.x(),e.y())){
            if(e.x()<code.x()-4){
                int line=(int)Math.floor((e.y()-code.y()+scroll)/LINE);
                if(line<buffer.lineCount())buffer.selectLine(line);
                return true;
            }
            boolean shift=(e.modifiers()&GLFW.GLFW_MOD_SHIFT)!=0;
            buffer.move(offsetAt(e.x(),e.y()),shift);
            if(doubled)buffer.selectWord();
            selecting=!doubled;
            return true;
        }
        for(var action:actions)if(action.rect().contains(e.x(),e.y())){action.run().run();return true;}
        return true;
    }
    @Override protected boolean dragVela(MouseButtonEvent e,double dx,double dy){
        if(draggingScroll){scrollTo(e.y());return true;}
        if(selecting){
            buffer.move(offsetAt(e.x(),e.y()),true);
            if(e.y()<code.y())targetScroll-=LINE;else if(e.y()>code.y()+code.height())targetScroll+=LINE;
            if(e.x()>code.x()+code.width())scrollX+=8;else if(e.x()<code.x())scrollX=Math.max(0,scrollX-8);
        }
        return true;
    }
    @Override protected boolean releaseVela(MouseButtonEvent e){draggingScroll=false;selecting=false;return true;}
    private void scrollTo(double y){
        int limit=Math.max(0,buffer.lineCount()*LINE-code.height()+LINE/2);
        targetScroll=Math.clamp((float)(y-scrollTrack.y()-scrollGrab)/Math.max(1,scrollTrack.height()-scrollThumb.height()),0,1)*limit;
    }
    @Override protected boolean scrollVela(double x,double y,double horizontal,double vertical){
        boolean shift=minecraft!=null&&(GLFW.glfwGetKey(minecraft.getWindow().handle(),GLFW.GLFW_KEY_LEFT_SHIFT)==GLFW.GLFW_PRESS);
        if(shift||horizontal!=0)scrollX=Math.max(0,scrollX-(float)(horizontal!=0?horizontal:vertical)*24);
        else targetScroll-=(float)vertical*LINE*3;
        return true;
    }

    @Override public boolean keyPressed(KeyEvent e){
        if(closing)return true;
        int key=e.key();
        boolean ctrl=(e.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0,shift=(e.modifiers()&GLFW.GLFW_MOD_SHIFT)!=0;
        int page=Math.max(1,code.height()/LINE-1);
        switch(key){
            case GLFW.GLFW_KEY_ESCAPE->{onClose();return true;}
            case GLFW.GLFW_KEY_ENTER,GLFW.GLFW_KEY_KP_ENTER->{if(ctrl)apply();else buffer.newline();}
            case GLFW.GLFW_KEY_TAB->buffer.tab(shift);
            case GLFW.GLFW_KEY_BACKSPACE->buffer.backspace(ctrl);
            case GLFW.GLFW_KEY_DELETE->buffer.delete(ctrl);
            case GLFW.GLFW_KEY_LEFT->buffer.horizontal(-1,shift,ctrl);
            case GLFW.GLFW_KEY_RIGHT->buffer.horizontal(1,shift,ctrl);
            case GLFW.GLFW_KEY_UP->buffer.vertical(-1,shift);
            case GLFW.GLFW_KEY_DOWN->buffer.vertical(1,shift);
            case GLFW.GLFW_KEY_PAGE_UP->buffer.vertical(-page,shift);
            case GLFW.GLFW_KEY_PAGE_DOWN->buffer.vertical(page,shift);
            case GLFW.GLFW_KEY_HOME->{if(ctrl)buffer.move(0,shift);else buffer.home(shift);}
            case GLFW.GLFW_KEY_END->{if(ctrl)buffer.move(buffer.text().length(),shift);else buffer.end(shift);}
            case GLFW.GLFW_KEY_A->{if(!ctrl)return true;buffer.selectAll();}
            case GLFW.GLFW_KEY_C->{if(!ctrl)return true;if(buffer.hasSelection())minecraft.keyboardHandler.setClipboard(buffer.selected());return true;}
            case GLFW.GLFW_KEY_X->{if(!ctrl)return true;if(buffer.hasSelection()){minecraft.keyboardHandler.setClipboard(buffer.selected());buffer.insert("");}}
            case GLFW.GLFW_KEY_V->{if(!ctrl)return true;buffer.insert(clean(minecraft.keyboardHandler.getClipboard()));}
            case GLFW.GLFW_KEY_Z->{if(!ctrl)return true;if(shift)buffer.redo();else buffer.undo();}
            case GLFW.GLFW_KEY_Y->{if(!ctrl)return true;buffer.redo();}
            case GLFW.GLFW_KEY_D->{if(!ctrl)return true;buffer.duplicateLines();}
            case GLFW.GLFW_KEY_SLASH,GLFW.GLFW_KEY_KP_DIVIDE->{if(!ctrl)return true;buffer.toggleComment();}
            case GLFW.GLFW_KEY_S->{if(!ctrl)return true;apply();}
            default->{return true;}
        }
        followCaret=true;
        return true;
    }
    /** Pasted text keeps its lines and indentation; other control characters are dropped. */
    private static String clean(String value){
        var out=new StringBuilder(value.length());
        value.replace("\r\n","\n").replace('\r','\n').codePoints().filter(cp->cp=='\n'||cp=='\t'||!Character.isISOControl(cp)).forEach(out::appendCodePoint);
        return out.toString();
    }
    @Override public boolean charTyped(CharacterEvent e){
        if(closing||!e.isAllowedChatCharacter())return true;
        int cp=e.codepoint();
        if((GLFW.glfwGetKey(minecraft.getWindow().handle(),GLFW.GLFW_KEY_LEFT_CONTROL)==GLFW.GLFW_PRESS))return true;
        buffer.type(cp);followCaret=true;return true;
    }
    @Override public void onClose(){if(!closing){settings.editorDraft(buffer.text());closing=true;}}
    @Override public boolean isPauseScreen(){return false;}
}
