package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.module.visual.ViewModel;
import dev.sqwat.vela.module.setting.NumberSetting;
import dev.sqwat.vela.ui.input.TextEdit;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Live world preview. Only the existing module settings are persisted. */
public final class ViewModelEditorScreen extends VelaScreen {
    private final VelaScreen parent;
    private final VelaServices services;
    private final ViewModel model;
    private final UiControls controls=new UiControls();
    private final TextEdit input=new TextEdit();
    private List<NumberSetting> values;
    private final List<Rect> fields=new ArrayList<>(),tracks=new ArrayList<>(),hands=new ArrayList<>();
    private Rect panel=new Rect(0,0,0,0),close=panel,reset=panel,done=panel,collapse=panel;
    private boolean collapsed;
    private int focus=-1,slider=-1;
    private boolean dragging;
    private double dragX,dragY,startX,startY;
    private double dragUnitX,dragUnitY;
    private String error="";
    public ViewModelEditorScreen(VelaScreen parent,VelaServices services,ViewModel model){
        super(Component.literal("ViewModel"));this.parent=parent;this.services=services;this.model=model;values=model.transforms();
        model.preview(true);
    }
    @Override protected float interfaceScale(){return services.configs().current().uiScale;}
    @Override protected void renderVela(GuiGraphicsExtractor g,int mx,int my,float delta){
        float reveal=controls.animate("panel-collapse",collapsed?1:0,12);
        keepVisible();
        int x=Math.round(12-12*reveal),h=Math.round(352-304*reveal),y=Math.round(Math.max(12,(height-352)/2f)*(1-reveal)+Math.max(12,height*.25f)*reveal),w=Math.round(236-208*reveal);
        panel=new Rect(x,y,w,h);fields.clear();tracks.clear();hands.clear();
        g.fillGradient(0,height/3,width,height,VelaTheme.PRIMARY&0xFFFFFF,0x66000000|(VelaTheme.PRIMARY&0xFFFFFF));
        VelaDraw.roundedOutline(g,x,y,w,h,9,VelaTheme.BORDER,VelaTheme.WINDOW);
        collapse=new Rect(x+Math.round(180-177*reveal),y+Math.round(8+5*reveal),22,22);
        UiClip.enable(g,x,y,x+w,y+h);
        if(reveal<.99f){
        VelaIcons.draw(g,"move",x+12,y+12,14,VelaTheme.PRIMARY);
        VelaDraw.text(g,"ViewModel",x+32,y+14,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,11);
        close=new Rect(x+206,y+8,22,22);controls.iconButton(g,"close",close,"x",false,!collapsed&&close.contains(mx,my));
        VelaDraw.hairline(g,x,y+36,w,VelaTheme.BORDER);
        String[] handLabels={"Обе","Основная","Вторая"};
        for(int i=0;i<3;i++){
            Rect r=new Rect(x+12+i*72,y+46,68,24);hands.add(r);
            float selected=controls.animate("hand-selected"+i,model.hand().value().equals(handLabels[i])?1:0);
            float hovered=controls.animate("hand-hover"+i,r.contains(mx,my)?1:0);
            VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),6,VelaTheme.mix(VelaTheme.SURFACE_DEEP,VelaTheme.readableAccentFill(),Math.max(selected,hovered*.6f)));
            VelaDraw.label(g,handLabels[i],r,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM);
        }
        String[] labels={"X · горизонталь","Y · вертикаль","Z · глубина","Наклон","Разворот","Крен","Размер"};
        for(int i=0;i<values.size();i++){
            var setting=values.get(i);int row=y+82+i*36;
            VelaDraw.text(g,labels[i],x+13,row,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
            Rect track=new Rect(x+14,row+13,132,12),field=new Rect(x+160,row-1,64,27);
            tracks.add(track);fields.add(field);
            controls.slider(g,"value"+i,track,(float)setting.fraction());
            int neutral=track.x()+(int)Math.round((setting.defaultValue()-setting.min())/(setting.max()-setting.min())*track.width());
            VelaDraw.roundedRect(g,neutral-1,track.y()+2,2,8,1,VelaTheme.TEXT_SECONDARY);
            VelaDraw.roundedOutline(g,field.x(),field.y(),field.width(),field.height(),5,VelaTheme.mix(VelaTheme.BORDER,VelaTheme.PRIMARY,controls.animate("focus"+i,focus==i?1:0)),VelaTheme.SURFACE_DEEP);
            Rect text=new Rect(field.x()+7,field.y()+7,50,14);
            if(focus==i)TextFieldDraw.draw(g,input,text,true,"");
            else VelaDraw.text(g,String.format(Locale.ROOT,"%.2f",setting.value()),text.x(),text.y(),VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8);
        }
        if(!error.isEmpty())VelaDraw.text(g,error,x+12,y+337,VelaTheme.PRIMARY,VelaDraw.Weight.REGULAR,8);
        // Contents fade into the panel during the first part of the collapse (and back in when expanding),
        // instead of staying fully visible until the panel is almost closed.
        float fade=Math.clamp(reveal*1.7f,0,1);
        if(fade>0)VelaDraw.roundedRect(g,x+1,y+1,w-2,h-2,8,(Math.round(fade*fade*(3-2*fade)*255)<<24)|(VelaTheme.WINDOW&0xFFFFFF));
        }
        g.disableScissor();
        float hover=controls.animate("collapse-hover",collapse.contains(mx,my)?1:0);
        VelaDraw.roundedRect(g,collapse.x(),collapse.y(),22,22,6,VelaTheme.mix(0x00141923,VelaTheme.SURFACE_HOVER,hover));
        g.pose().pushMatrix();g.pose().translate(collapse.x()+11,collapse.y()+11);g.pose().rotate((float)(Math.PI*reveal));
        VelaIcons.draw(g,"back",-6,-6,12,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.PRIMARY,hover));g.pose().popMatrix();
        if(reveal>.02f){fields.clear();tracks.clear();hands.clear();close=new Rect(0,0,0,0);}
        float factor=2f/(Math.max(1,minecraft.getWindow().getGuiScale())*renderScale());
        reset=new Rect(width-Math.round(129*factor),Math.round(12*factor),Math.round(57*factor),Math.round(20*factor));
        done=new Rect(width-Math.round(69*factor),Math.round(12*factor),Math.round(56*factor),Math.round(20*factor));
        dragButton(g,"reset",reset,"Сбросить",factor,mx,my);
        dragButton(g,"exit",done,"Выйти",factor,mx,my);
        String[] help={"ЛКМ на руке — перемещение · колесо — размер","Shift + колесо — крен · Ctrl + колесо — глубина","Esc — выйти"};
        g.pose().pushMatrix();g.pose().translate(width-8*factor,height-8*factor);g.pose().scale(factor);
        for(int i=0;i<help.length;i++)VelaDraw.text(g,help[i],-VelaDraw.textWidth(help[i],VelaDraw.Weight.REGULAR,7),-32+i*11,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,7);
        g.pose().popMatrix();
        if(minecraft.level==null)VelaDraw.text(g,"Предпросмотр доступен в мире",x+w+18,20,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,9);
    }
    private void dragButton(GuiGraphicsExtractor g,String id,Rect rect,String text,float factor,int mx,int my){
        float hover=controls.animate(id,rect.contains(mx,my)?1:0);
        g.pose().pushMatrix();g.pose().translate(rect.x(),rect.y());g.pose().scale(factor);
        var local=new Rect(0,0,Math.round(rect.width()/factor),Math.round(rect.height()/factor));
        VelaDraw.roundedRect(g,0,0,local.width(),local.height(),5,VelaTheme.mix(0xFF0B0E14,VelaTheme.readableAccentFill(),hover));
        VelaDraw.label(g,text,local,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.TEXT,hover),VelaDraw.Weight.MEDIUM);
        g.pose().popMatrix();
    }
    private void keepVisible(){
        if(minecraft.player==null)return;
        var projection=((dev.sqwat.vela.mixin.entity.HandProjectionAccessor)minecraft.gameRenderer).vela$handProjection().getMatrix(new org.joml.Matrix4f());
        for(var hand:net.minecraft.world.InteractionHand.values()){
            boolean main=hand==net.minecraft.world.InteractionHand.MAIN_HAND;
            boolean right=(main?minecraft.player.getMainArm():minecraft.player.getMainArm().getOpposite())==net.minecraft.world.entity.HumanoidArm.RIGHT;
            model.keepVisible(hand,right,projection.m00(),projection.m11());
        }
    }
    private void save(){services.configs().requestSave();}
    private boolean commit(){
        if(focus<0)return true;
        try{model.setTransform(focus,Double.parseDouble(input.text().strip().replace(',','.')));focus=-1;error="";save();return true;}
        catch(IllegalArgumentException ex){error="Введите конечное число";return false;}
    }
    @Override protected boolean clickVela(MouseButtonEvent e,boolean doubled){
        if(e.button()!=0)return true;
        if(!commit())return true;
        if(collapse.contains(e.x(),e.y())){collapsed=!collapsed;dragging=false;slider=-1;return true;}
        if(close.contains(e.x(),e.y())||done.contains(e.x(),e.y())){onClose();return true;}
        if(reset.contains(e.x(),e.y())){model.resetTransforms();save();return true;}
        for(int i=0;i<hands.size();i++)if(hands.get(i).contains(e.x(),e.y())){model.selectHand(model.hand().options().get(i));values=model.transforms();dragging=false;slider=-1;save();return true;}
        for(int i=0;i<fields.size();i++){
            if(fields.get(i).contains(e.x(),e.y())){focus=i;input.set(String.format(Locale.ROOT,"%.2f",values.get(i).value()));input.selectAll();return true;}
            if(tracks.get(i).contains(e.x(),e.y())){slider=i;updateSlider(e.x());return true;}
        }
        dragging=!panel.contains(e.x(),e.y())&&minecraft.level!=null;
        if(dragging){
            captureHand(e.x(),e.y());
            dragX=dragY=0;startX=values.get(0).value();startY=values.get(1).value();
            var projection=((dev.sqwat.vela.mixin.entity.HandProjectionAccessor)minecraft.gameRenderer).vela$handProjection().getMatrix(new org.joml.Matrix4f());
            double depth=Math.max(.05,.72-values.get(2).value());
            dragUnitX=2*depth/(Math.max(.01,Math.abs(projection.m00()))*Math.max(1,width));
            dragUnitY=2*depth/(Math.max(.01,Math.abs(projection.m11()))*Math.max(1,height));
        }
        return true;
    }
    private void updateSlider(double x){var track=tracks.get(slider);var value=values.get(slider);model.setTransform(slider,value.min()+Math.clamp((x-track.x())/track.width(),0,1)*(value.max()-value.min()));}
    @Override protected boolean dragVela(MouseButtonEvent e,double dx,double dy){
        if(e.button()!=0)return true;
        if(slider>=0)updateSlider(e.x());
        else if(dragging){
            dragX+=dx;dragY+=dy;
            model.setTransform(0,startX+dragX*dragUnitX);
            model.setTransform(1,startY-dragY*dragUnitY);
        }
        return true;
    }
    @Override protected boolean releaseVela(MouseButtonEvent e){
        if(e.button()==0){dragging=false;slider=-1;save();}
        return true;
    }
    @Override protected boolean scrollVela(double x,double y,double horizontal,double vertical){
        if(panel.contains(x,y)||!commit())return true;
        if(!dragging)captureHand(x,y);
        long handle=minecraft.getWindow().handle();
        boolean shift=GLFW.glfwGetKey(handle,GLFW.GLFW_KEY_LEFT_SHIFT)==GLFW.GLFW_PRESS||GLFW.glfwGetKey(handle,GLFW.GLFW_KEY_RIGHT_SHIFT)==GLFW.GLFW_PRESS;
        boolean ctrl=GLFW.glfwGetKey(handle,GLFW.GLFW_KEY_LEFT_CONTROL)==GLFW.GLFW_PRESS||GLFW.glfwGetKey(handle,GLFW.GLFW_KEY_RIGHT_CONTROL)==GLFW.GLFW_PRESS;
        model.wheel(vertical,shift,ctrl);save();return true;
    }
    private void captureHand(double x,double y){
        if(!model.hand().value().equals("Обе")||minecraft.player==null)return;
        var projection=((dev.sqwat.vela.mixin.entity.HandProjectionAccessor)minecraft.gameRenderer).vela$handProjection().getMatrix(new org.joml.Matrix4f());
        net.minecraft.world.InteractionHand closest=net.minecraft.world.InteractionHand.MAIN_HAND;
        double best=Double.POSITIVE_INFINITY;
        for(var hand:net.minecraft.world.InteractionHand.values()){
            boolean right=(hand==net.minecraft.world.InteractionHand.MAIN_HAND?minecraft.player.getMainArm():minecraft.player.getMainArm().getOpposite())==net.minecraft.world.entity.HumanoidArm.RIGHT;
            var v=model.transforms(hand);
            var point=new org.joml.Vector4f((float)((right?.56:-.56)+v.get(0).value()),(float)(-.30+v.get(1).value()),(float)(-.72+v.get(2).value()),1).mul(projection);
            if(point.w<=.01f)continue;
            double px=(point.x/point.w*.5+.5)*width,py=(.5-point.y/point.w*.5)*height;
            double distance=(px-x)*(px-x)+(py-y)*(py-y);
            if(distance<best){best=distance;closest=hand;}
        }
        model.editHand(closest);values=model.transforms();
    }
    @Override public boolean keyPressed(KeyEvent e){
        if(e.key()==GLFW.GLFW_KEY_ESCAPE){if(focus>=0){focus=-1;error="";}else onClose();return true;}
        if(e.key()==GLFW.GLFW_KEY_ENTER){commit();return true;}
        if(e.key()==GLFW.GLFW_KEY_TAB){
            int next=Math.floorMod(focus+((e.modifiers()&GLFW.GLFW_MOD_SHIFT)!=0?-1:1),values.size());
            if(commit()){focus=next;input.set(String.format(Locale.ROOT,"%.2f",values.get(focus).value()));input.selectAll();}
            return true;
        }
        if(focus>=0)dev.sqwat.vela.ui.input.GameTextInput.key(input,e,12);
        return true;
    }
    @Override public boolean charTyped(CharacterEvent e){if(focus>=0&&e.isAllowedChatCharacter())input.insert(e.codepointAsString(),12);return true;}
    @Override public void onClose(){if(commit()){dragging=false;slider=-1;save();minecraft.gui.setScreen(parent);}}
    @Override public void removed(){dragging=false;slider=-1;model.preview(false);super.removed();}
    @Override public boolean isPauseScreen(){return false;}
}
