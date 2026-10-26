package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.function.Function;

/** Small modal with independent text state; search remains a search field. */
public final class VelaFormScreen extends VelaScreen {
    private final VelaScreen parent;
    private final String title,caption,action;
    private final Function<String,String> submit;
    private String value="",error="";
    private Rect panel,input,cancel,confirm;
    private boolean selectedAll,focused;
    private final dev.sqwat.vela.ui.animation.FrameClock motionClock=new dev.sqwat.vela.ui.animation.FrameClock();
    private float entrance;
    public VelaFormScreen(VelaScreen parent,String title,String caption,String action,Function<String,String> submit){
        super(Component.literal(title));this.parent=parent;this.title=title;this.caption=caption;this.action=action;this.submit=submit;
    }
    @Override protected float interfaceScale(){return parent.interfaceScale();}
    @Override protected void renderVela(GuiGraphicsExtractor g,int mx,int my,float delta){
        parent.renderAsBackdrop(g,width,height,delta);g.nextStratum();g.fill(0,0,width,height,0xB805070A);
        entrance=dev.sqwat.vela.ui.animation.Motion.approach(entrance,1,18,motionClock.step());
        panel=new Rect((width-232)/2,(height-154)/2+Math.round((1-entrance)*10),232,154);
        VelaDraw.roundedOutline(g,panel.x(),panel.y(),232,154,8,VelaTheme.BORDER,VelaTheme.SURFACE);
        VelaDraw.text(g,title,panel.x()+17,panel.y()+17,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,10);
        VelaDraw.text(g,caption,panel.x()+17,panel.y()+46,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.MEDIUM,8);
        input=new Rect(panel.x()+17,panel.y()+64,198,29);
        VelaDraw.roundedRect(g,input.x(),input.y(),198,29,6,VelaTheme.SURFACE_DEEP);
        String display=value.isEmpty()?(focused?"":"Введите значение"):value;
        if(selectedAll&&!value.isEmpty())VelaDraw.roundedRect(g,input.x()+8,input.y()+7,Math.min(182,VelaDraw.textWidth(value,VelaDraw.Weight.REGULAR)+4),15,2,VelaTheme.PRIMARY_SOFT);
        VelaDraw.text(g,VelaDraw.ellipsize(display,178,VelaDraw.Weight.REGULAR),input.x()+10,input.y()+10,value.isEmpty()?VelaTheme.TEXT_MUTED:VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);
        if(focused)VelaDraw.roundedRect(g,input.x()+10+Math.round(Math.min(178,VelaDraw.textWidth(value,VelaDraw.Weight.REGULAR,8))),input.y()+8,1,9,0,VelaDraw.caretColor());
        if(!error.isEmpty())VelaDraw.text(g,VelaDraw.ellipsize(error,198,VelaDraw.Weight.REGULAR),panel.x()+17,panel.y()+98,VelaTheme.DANGER,VelaDraw.Weight.REGULAR,7);
        cancel=new Rect(panel.x()+17,panel.y()+112,95,29);confirm=new Rect(panel.x()+119,panel.y()+112,96,29);
        drawButton(g,cancel,"Отмена",false);drawButton(g,confirm,action,true);
    }
    private void drawButton(GuiGraphicsExtractor g,Rect r,String text,boolean active){VelaDraw.roundedOutline(g,r.x(),r.y(),r.width(),r.height(),6,VelaTheme.BORDER,active?VelaTheme.PRIMARY:VelaTheme.SURFACE_DEEP);VelaDraw.label(g,text,r,active?VelaTheme.onPrimary():VelaTheme.TEXT,VelaDraw.Weight.MEDIUM);}
    private void save(){error=submit.apply(value.strip());if(error==null||error.isEmpty())onClose();}
    @Override protected boolean clickVela(MouseButtonEvent e,boolean twice){if(e.button()!=0)return true;focused=input!=null&&input.contains(e.x(),e.y());if(cancel!=null&&(cancel.contains(e.x(),e.y())||!panel.contains(e.x(),e.y())))onClose();else if(confirm!=null&&confirm.contains(e.x(),e.y()))save();return true;}
    @Override public boolean keyPressed(KeyEvent e){
        if(e.key()==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}
        if(e.key()==GLFW.GLFW_KEY_ENTER){save();return true;}
        if(!focused)return true;
        if(e.key()==GLFW.GLFW_KEY_BACKSPACE&&(e.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0){var edit=new dev.sqwat.vela.ui.TextEdit();edit.set(value);if(selectedAll)edit.selectAll();edit.eraseWord();value=edit.text();selectedAll=false;return true;}
        if((e.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0){
            if(e.key()==GLFW.GLFW_KEY_A){selectedAll=true;return true;}
            if(e.key()==GLFW.GLFW_KEY_V){String pasted=minecraft.keyboardHandler.getClipboard().replaceAll("[\\p{Cntrl}]","");value=(selectedAll?"":value)+pasted;value=value.substring(0,Math.min(value.length(),32));selectedAll=false;return true;}
        }
        if(e.key()==GLFW.GLFW_KEY_BACKSPACE){value=selectedAll||value.isEmpty()?"":value.substring(0,value.offsetByCodePoints(value.length(),-1));selectedAll=false;}
        return true;
    }
    @Override public boolean charTyped(CharacterEvent e){if(focused&&e.isAllowedChatCharacter()){if(selectedAll)value="";selectedAll=false;if(value.codePointCount(0,value.length())<32)value+=e.codepointAsString();}return true;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
