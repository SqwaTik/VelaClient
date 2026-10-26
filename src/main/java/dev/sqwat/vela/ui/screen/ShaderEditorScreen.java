package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.module.visual.Shaders;
import dev.sqwat.vela.render.shader.CustomSkyProgram;
import dev.sqwat.vela.render.shader.CustomSkySource;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Uses native multiline caret/selection/clipboard handling inside the existing Vela editor. */
public final class ShaderEditorScreen extends VelaScreen {
    private final VelaScreen parent;
    private final VelaServices services;
    private final Shaders settings;
    private final UiControls controls=new UiControls();
    private MultiLineEditBox code;
    private Rect close=new Rect(0,0,0,0),apply=close,copy=close;
    private String status="Локальный GLSL запускается только по кнопке «Применить».";
    private boolean error;
    public ShaderEditorScreen(VelaScreen parent,VelaServices services,Shaders settings){
        super(Component.literal("Редактор GLSL"));this.parent=parent;this.services=services;this.settings=settings;
    }
    @Override protected float interfaceScale(){return services.configs().current().uiScale;}
    @Override protected void init(){
        String draft=code==null?settings.editorDraft():code.getValue();
        code=MultiLineEditBox.builder().setTextColor(VelaTheme.TEXT).setCursorColor(VelaTheme.PRIMARY)
            .setTextShadow(false).setShowBackground(false).setShowDecorations(false)
            .build(font,300,160,Component.literal("GLSL: mainImage"));
        code.setCharacterLimit(CustomSkySource.LIMIT);code.setValue(draft);addWidget(code);setInitialFocus(code);
    }
    @Override protected void renderVela(GuiGraphicsExtractor g,int mx,int my,float delta){
        parent.renderAsBackdrop(g,width,height,delta);g.nextStratum();g.fill(0,0,width,height,0xD005070A);
        int w=Math.max(160,Math.min(620,width-20)),h=Math.max(180,Math.min(420,height-16)),x=(width-w)/2,y=(height-h)/2;
        VelaDraw.roundedRect(g,x,y,w,h,8,VelaTheme.WINDOW);
        VelaDraw.text(g,"Редактор GLSL · Небо",x+12,y+12,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,11);
        close=new Rect(x+w-31,y+6,24,24);controls.iconButton(g,"shader-close",close,"x",false,close.contains(mx,my));
        VelaDraw.hairline(g,x+12,y+34,w-24,VelaTheme.BORDER);
        int codeHeight=Math.max(48,h-143);
        VelaDraw.roundedRect(g,x+10,y+43,w-20,codeHeight,5,VelaTheme.SURFACE_DEEP);
        code.setRectangle(w-28,codeHeight-8,x+14,y+47);code.extractRenderState(g,mx,my,delta);
        int helpY=y+49+codeHeight;
        VelaDraw.text(g,"mainImage · iTime · iResolution · skyDirection · clientColor",x+12,helpY,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
        var lines=font.split(Component.literal(status),Math.max(40,w-24));
        for(int i=0;i<Math.min(3,lines.size());i++)g.text(font,lines.get(i),x+12,helpY+15+i*10,error?0xFFFF8793:VelaTheme.TEXT,false);
        apply=new Rect(x+w-126,y+h-32,114,23);copy=new Rect(x+12,y+h-32,138,23);
        controls.button(g,"shader-apply",apply,"Применить · Ctrl+Enter",apply.contains(mx,my));
        controls.button(g,"shader-copy",copy,"Копировать диагностику",copy.contains(mx,my));
    }
    private void apply(){
        String result=CustomSkyProgram.apply(code.getValue());
        error=result!=null;
        if(error){status=result;return;}
        settings.skyCode(code.getValue());settings.editorDraft(code.getValue());settings.useCustomSky();services.configs().requestSave();
        status="Применено. Ошибки нового кода не заменяют рабочий шейдер.";
    }
    @Override protected boolean clickVela(MouseButtonEvent e,boolean doubled){
        if(e.button()==0){
            if(close.contains(e.x(),e.y())){onClose();return true;}
            if(apply.contains(e.x(),e.y())){apply();return true;}
            if(copy.contains(e.x(),e.y())){minecraft.keyboardHandler.setClipboard(status);return true;}
        }
        code.setFocused(code.isMouseOver(e.x(),e.y()));return code.mouseClicked(e,doubled);
    }
    @Override protected boolean dragVela(MouseButtonEvent e,double dx,double dy){return code.mouseDragged(e,dx,dy);}
    @Override protected boolean releaseVela(MouseButtonEvent e){return code.mouseReleased(e);}
    @Override protected boolean scrollVela(double x,double y,double horizontal,double vertical){return code.mouseScrolled(x,y,horizontal,vertical);}
    @Override public boolean keyPressed(KeyEvent e){
        if(e.key()==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}
        if(e.key()==GLFW.GLFW_KEY_ENTER&&(e.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0){apply();return true;}
        return code.keyPressed(e);
    }
    @Override public boolean charTyped(CharacterEvent e){return code.charTyped(e);}
    @Override public boolean preeditUpdated(PreeditEvent e){return code.preeditUpdated(e);}
    @Override public void onClose(){settings.editorDraft(code.getValue());minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
