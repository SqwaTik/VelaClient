package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.ui.hud.HudLayoutService;
import dev.sqwat.vela.ui.render.Rect;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class HudPositionScreen extends Screen {
    private final Screen parent;
    private final HudLayoutService layout;
    private final List<ElementHit> elements = new ArrayList<>();
    private String dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private Rect saveHit = new Rect(0, 0, 0, 0);
    private Rect resetHit = new Rect(0, 0, 0, 0);
    private Rect exitHit = new Rect(0, 0, 0, 0);
    private String resizing;
    private String transparencyTarget;
    private Rect transparencyMenu=new Rect(0,0,0,0);
    private Rect sizeMenu=new Rect(0,0,0,0);
    private final dev.sqwat.vela.ui.animation.FrameClock menuClock=new dev.sqwat.vela.ui.animation.FrameClock();
    private float menuReveal;
    private float contextReveal;
    private final float[] sizeHover=new float[3];
    private float sizeSlide=Float.NaN;
    private final java.util.Map<String,Float> rowHover=new java.util.HashMap<>();
    private final RgbPicker picker=new RgbPicker();
    private static final float[] SIZES={.5f,.75f,1f};
    private static final int MENU_HEAD=22,SIZE_W=150,SIZE_H=44,SIZE_SEG_Y=22,SIZE_SEG_H=16;
    private final java.util.Map<String,Float> elementHover=new java.util.HashMap<>();

    public HudPositionScreen(Screen parent, HudLayoutService layout) {
        super(Component.literal("HUD positions"));
        this.parent = parent;
        this.layout = layout;
    }

    @Override public void added() { layout.setEditing(true); }
    @Override public void removed() { layout.setEditing(false); minecraft.options.keyPlayerList.setDown(false); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        layout.viewport(minecraft.getWindow().getGuiScale());
        float menuDt=menuClock.step();
        var services=dev.sqwat.vela.VelaClient.services();
        boolean immediate=services!=null&&(!services.configs().current().animations||services.configs().current().reducedMotion);
        contextReveal=immediate?1:dev.sqwat.vela.ui.animation.Motion.approach(contextReveal,1,20,menuDt);
        menuReveal=dev.sqwat.vela.ui.animation.Motion.approach(menuReveal,resizing==null?0:1,24,menuDt);
        g.fill(0, 0, width, height, 0x24000000);
        g.fillGradient(0,height/3,width,height,VelaTheme.PRIMARY&0xFFFFFF,0x38000000|(VelaTheme.PRIMARY&0xFFFFFF));
        for(int gx=0;gx<width;gx+=512)for(int gy=0;gy<height;gy+=512){int w=Math.min(512,width-gx),h=Math.min(512,height-gy);VelaDraw.blit(g,net.minecraft.resources.Identifier.fromNamespaceAndPath("velaclient","textures/gui/hud_grid.png"),gx,gy,w,h,0,0,w,h,512,512,0x183D4961);}
        drawGuides(g);


        elements.clear();
        layout.previewNative("bossbar",new Rect(width/2-91,12,182,24));
        layout.previewNative("scoreboard",new Rect(width-132,height/2-36,132,72));
        for (String id : layout.editableIds()) {
            if(HudLayoutService.isNative(id)&&!layout.nativeVisible(id)&&!id.equals("bossbar")&&!id.equals("scoreboard"))continue;
            if(id.equals("tablist")&&!minecraft.options.keyPlayerList.isDown()||id.equals("bossbar")&&minecraft.options.keyPlayerList.isDown())continue;
            if(id.equals("chat")&&GLFW.glfwGetKey(minecraft.getWindow().handle(),GLFW.GLFW_KEY_TAB)!=GLFW.GLFW_PRESS)continue;
            HudLayoutService.HudPosition position = layout.position(id, width, height);
            String label = layout.label(minecraft, id);
            HudLayoutService.RectSize size = layout.bounds(minecraft, id);
            Rect rect = new Rect(position.x(), position.y(), size.width(), size.height());
            if(HudLayoutService.isNative(id)&&!layout.nativeVisible(id)){
                VelaDraw.roundedOutline(g,rect.x(),rect.y(),rect.width(),rect.height(),6,0x70576578,0xB810141C);
                VelaDraw.centeredText(g,id.equals("bossbar")?"Bossbar":"Scoreboard",rect.x()+rect.width()/2f,rect.y()+rect.height()/2f-4,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);
            }
            boolean hovered = rect.contains(mouseX, mouseY) || id.equals(dragging);
            if(!HudLayoutService.isNative(id))layout.drawChip(g,minecraft,id,rect.x(),rect.y());
            float hover=elementHover.compute(id,(k,v)->dev.sqwat.vela.ui.animation.Motion.approach(v==null?0:v,hovered?1:0,9,menuDt));
            VelaDraw.roundedRect(g,rect.x()-1,rect.y()-1,rect.width()+2,rect.height()+2,7,VelaTheme.mix(0,VelaTheme.PRIMARY_SOFT,hover));
            var origin=layout.nativeOrigin(id);
            if(origin!=null&&id.equals(dragging)&&HudLayoutService.isNative(id)){
                VelaDraw.roundedOutline(g,origin.x(),origin.y(),size.width(),size.height(),7,0xDDAEBBD0,0x183F5068);
            }

            elements.add(new ElementHit(id, rect));
        }

        g.nextStratum();float controlScale=2f/Math.max(1,minecraft.getWindow().getGuiScale());
        int barX = width - Math.round(136*controlScale);
        resetHit = new Rect(barX+Math.round(7*controlScale),Math.round(12*controlScale),Math.round(57*controlScale),Math.round(20*controlScale));
        exitHit = new Rect(barX+Math.round(67*controlScale),Math.round(12*controlScale),Math.round(56*controlScale),Math.round(20*controlScale));
        saveHit=new Rect(0,0,0,0);
        resetHover=dev.sqwat.vela.ui.animation.Motion.approach(resetHover,resetHit.contains(mouseX,mouseY)?1:0,9,menuDt);
        exitHover=dev.sqwat.vela.ui.animation.Motion.approach(exitHover,exitHit.contains(mouseX,mouseY)?1:0,9,menuDt);
        button(g, resetHit, "Сбросить", resetHover);
        button(g, exitHit, "Выйти", exitHover);

        g.pose().pushMatrix();
        g.pose().translate(width-8*controlScale,height-8*controlScale);
        g.pose().scale(controlScale);
        String[] help={"ЛКМ — переместить","СКМ — настройки · ПКМ — размер"};
        for(int i=0;i<help.length;i++)VelaDraw.text(g,help[i],-VelaDraw.textWidth(help[i],VelaDraw.Weight.REGULAR,7),-21+i*11,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,7);
        g.pose().popMatrix();

        if(transparencyTarget!=null){
            var rows=menuRows();
            for(ElementHit hit:elements)if(hit.id.equals(transparencyTarget))transparencyMenu=dev.sqwat.vela.ui.hud.HudGeometry.context(hit.rect,width,height,menuWidth(),menuHeight(rows));
            g.nextStratum();
            dev.sqwat.vela.ui.render.UiClip.enable(g,transparencyMenu.x()-1,transparencyMenu.y()-1,transparencyMenu.x()+transparencyMenu.width()+1,transparencyMenu.y()+Math.round(transparencyMenu.height()*contextReveal)+1);
            g.pose().pushMatrix();g.pose().translate(transparencyMenu.x(),transparencyMenu.y()-3*(1-contextReveal));
            int pw=transparencyMenu.width(),ph=transparencyMenu.height();
            VelaDraw.roundedOutline(g,0,0,pw,ph,8,VelaTheme.BORDER,VelaTheme.WINDOW);
            dev.sqwat.vela.ui.render.VelaIcons.draw(g,"settings",8,7,9,VelaTheme.PRIMARY);
            VelaDraw.text(g,VelaDraw.ellipsize(layout.functionName(transparencyTarget),pw-28,VelaDraw.Weight.MEDIUM,8),21,8,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8);
            VelaDraw.roundedRect(g,8,MENU_HEAD-1,pw-16,1,0,VelaTheme.BORDER);
            float lx=mouseX-transparencyMenu.x(),ly=mouseY-transparencyMenu.y();
            for(var row:rows){
                var option=row.setting();
                boolean over=new Rect(4,row.y(),pw-8,row.height()).contains(lx,ly)&&!picker.open();
                float hover=rowHover.compute(option.id(),(k,v)->dev.sqwat.vela.ui.animation.Motion.approach(v==null?0:v,over?1:0,14,menuDt));
                if(hover>.01f)VelaDraw.roundedRect(g,4,row.y(),pw-8,row.height(),5,VelaTheme.mix(VelaTheme.WINDOW,VelaTheme.SURFACE_HOVER,hover));
                // The same EXPERIMENTAL mark as in the module's settings, after the name.
                float badge=option.isExperimental()?VelaDraw.textWidth("EXPERIMENTAL",VelaDraw.Weight.SEMIBOLD,6f)+5:0;
                String name=VelaDraw.ellipsize(optionName(option),Math.round(pw-44-badge),VelaDraw.Weight.MEDIUM,7.5f);
                VelaDraw.text(g,name,10,row.y()+5,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,7.5f);
                if(badge>0)VelaDraw.text(g,"EXPERIMENTAL",10+VelaDraw.textWidth(name,VelaDraw.Weight.MEDIUM,7.5f)+5,row.y()+6,VelaTheme.TEXT_MUTED,VelaDraw.Weight.SEMIBOLD,6f);
                for(int l=0;l<row.description().size();l++)VelaDraw.text(g,row.description().get(l),10,row.y()+15+l*8,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,6.5f);
                int cy=row.y()+row.height()/2;
                if(option instanceof dev.sqwat.vela.module.setting.BooleanSetting toggle){
                    VelaDraw.toggle(g,"hud-context-"+transparencyTarget+"-"+option.id(),pw-29,cy-5,toggle.value(),over);
                }else if(option instanceof dev.sqwat.vela.module.setting.ColorSetting color){
                    VelaDraw.roundedRect(g,pw-27,cy-7,14,14,7,VelaTheme.mix(VelaTheme.BORDER,0xFFFFFFFF,hover*.6f));
                    VelaDraw.roundedRect(g,pw-25,cy-5,10,10,5,0xFF000000|color.value());
                }
            }
            g.pose().popMatrix();
            g.disableScissor();
            picker.draw(g,menuDt);
        }
        if(resizing!=null){
            g.nextStratum();dev.sqwat.vela.ui.render.UiClip.enable(g,sizeMenu.x()-1,sizeMenu.y()-1,sizeMenu.x()+sizeMenu.width()+1,sizeMenu.y()+Math.round(sizeMenu.height()*menuReveal)+1);
            int mx=sizeMenu.x(),my=sizeMenu.y()-Math.round(3*(1-menuReveal)),mw=sizeMenu.width();
            VelaDraw.roundedOutline(g,mx,my,mw,sizeMenu.height(),8,VelaTheme.BORDER,VelaTheme.WINDOW);
            dev.sqwat.vela.ui.render.VelaIcons.draw(g,"move",mx+8,my+7,9,VelaTheme.PRIMARY);
            VelaDraw.text(g,"Размер",mx+21,my+8,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8);
            float current=layout.scale(resizing);
            String percent=Math.round(current*100)+"%";
            VelaDraw.text(g,percent,mx+mw-8-VelaDraw.textWidth(percent,VelaDraw.Weight.REGULAR,7),my+8.5f,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7);
            // A segmented switch; the selected pill slides between the three sizes.
            int track=mx+6,trackW=mw-12,segY=my+SIZE_SEG_Y,seg=trackW/3;
            VelaDraw.roundedRect(g,track,segY,trackW,SIZE_SEG_H,6,VelaTheme.SURFACE);
            int chosen=-1;for(int i=0;i<3;i++)if(Math.abs(current-SIZES[i])<.01f)chosen=i;
            if(chosen>=0){
                sizeSlide=Float.isNaN(sizeSlide)?chosen:dev.sqwat.vela.ui.animation.Motion.approach(sizeSlide,chosen,18,menuDt);
                VelaDraw.roundedRect(g,Math.round(track+2+sizeSlide*seg),segY+2,seg-4+(chosen==2?trackW-seg*3:0),SIZE_SEG_H-4,5,VelaTheme.readableAccentFill());
            }
            String[] labels={"Меньше","Средний","Большой"};
            for(int i=0;i<3;i++){
                Rect cell=new Rect(track+i*seg,segY,seg,SIZE_SEG_H);
                sizeHover[i]=dev.sqwat.vela.ui.animation.Motion.approach(sizeHover[i],cell.contains(mouseX,mouseY)?1:0,14,menuDt);
                int color=i==chosen?VelaTheme.TEXT:VelaTheme.mix(VelaTheme.TEXT_MUTED,VelaTheme.TEXT,sizeHover[i]);
                VelaDraw.centeredText(g,labels[i],cell.x()+cell.width()/2f,segY+5,color,VelaDraw.Weight.MEDIUM,7);
            }
            g.disableScissor();
        }

    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if(picker.open()){
            if(picker.click(event.x(),event.y()))return true;
            saveSettings();
            if(transparencyTarget!=null&&transparencyMenu.contains(event.x(),event.y())&&event.button()==0)return true;
        }
        if(event.button()==GLFW.GLFW_MOUSE_BUTTON_MIDDLE){
            // The open popup owns its own area; other HUDs remain selectable.
            if(transparencyTarget!=null&&transparencyMenu.contains(event.x(),event.y()))return true;
            for(ElementHit hit:elements.reversed())if(hit.rect.contains(event.x(),event.y())&&layout.supportsTransparency(hit.id)){
                if(!hit.id.equals(transparencyTarget)){contextReveal=0;rowHover.clear();}
                transparencyTarget=hit.id;
                dragging=null;resizing=null;
                transparencyMenu=dev.sqwat.vela.ui.hud.HudGeometry.context(hit.rect,width,height,menuWidth(),menuHeight(menuRows()));
                return true;
            }
            transparencyTarget=null;
            return true;
        }
        if(transparencyTarget!=null){
            if(event.button()==0&&transparencyMenu.contains(event.x(),event.y())){
                double lx=event.x()-transparencyMenu.x(),ly=event.y()-transparencyMenu.y();
                for(var row:menuRows())if(contextReveal>.95f&&new Rect(4,row.y(),transparencyMenu.width()-8,row.height()).contains(lx,ly)){
                    if(row.setting() instanceof dev.sqwat.vela.module.setting.ColorSetting color)
                        picker.show(color,new Rect(transparencyMenu.x(),transparencyMenu.y()+row.y(),transparencyMenu.width(),row.height()),width,height);
                    else layout.toggleContextSetting(transparencyTarget,row.setting().id());
                    break;
                }
                return true;
            }
            transparencyTarget=null;picker.close();return true;
        }
        if(resizing!=null){
            if(event.button()==0&&sizeMenu.contains(event.x(),event.y())){
                int track=sizeMenu.x()+6,seg=(sizeMenu.width()-12)/3,segY=sizeMenu.y()+SIZE_SEG_Y;
                if(menuReveal<.95f||event.y()<segY||event.y()>segY+SIZE_SEG_H)return true;
                int cell=(int)((event.x()-track)/seg);if(cell>=0&&cell<3)layout.setScale(resizing,SIZES[cell]);
                return true;
            }
            resizing=null;return true;
        }
        if(event.button()==GLFW.GLFW_MOUSE_BUTTON_RIGHT){
            for(ElementHit hit:elements.reversed())if(hit.rect.contains(event.x(),event.y())){resizing=hit.id;menuReveal=0;sizeSlide=Float.NaN;java.util.Arrays.fill(sizeHover,0);sizeMenu=new Rect(Math.max(2,Math.min(width-SIZE_W-2,(int)event.x())),Math.max(2,Math.min(height-SIZE_H-2,(int)event.y())),SIZE_W,SIZE_H);return true;}
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubled);
        if (saveHit.contains(event.x(), event.y())) { layout.save(); onClose(); return true; }
        if (resetHit.contains(event.x(), event.y())) { layout.reset(); return true; }
        if (exitHit.contains(event.x(), event.y())) { onClose(); return true; }
        for (ElementHit element : elements.reversed()) {
            if (!element.rect().contains(event.x(), event.y())) continue;
            dragging = element.id();
            dragOffsetX = (int) event.x() - element.rect().x();
            dragOffsetY = (int) event.y() - element.rect().y();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if(picker.drag(event.x(),event.y()))return true;
        if(transparencyTarget!=null)return true;
        if (dragging == null || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseDragged(event, deltaX, deltaY);
        var size=layout.bounds(minecraft,dragging);
        int rawX=(int)event.x()-dragOffsetX,rawY=(int)event.y()-dragOffsetY;
        int x=rawX,y=rawY,bestX=9,bestY=9;
        // Alt temporarily disables snapping for exact positioning at the hotbar.
        if((event.modifiers()&GLFW.GLFW_MOD_ALT)!=0){layout.move(dragging,rawX,rawY,width,height,size);return true;}
        var guides=new java.util.ArrayList<Rect>();
        guides.add(new Rect(width/2,height/2,0,0));guides.add(new Rect(width/2-91,height-22,182,22));
        for(ElementHit other:elements)if(!other.id.equals(dragging))guides.add(other.rect);
        for(Rect guide:guides){
            int candidateX=dev.sqwat.vela.ui.hud.HudSnap.align(rawX,size.width(),guide.x(),guide.x()+guide.width());
            int candidateY=dev.sqwat.vela.ui.hud.HudSnap.align(rawY,size.height(),guide.y(),guide.y()+guide.height());
            int dx=Math.abs(candidateX-rawX),dy=Math.abs(candidateY-rawY);
            if(candidateX!=rawX&&dx<bestX){bestX=dx;x=candidateX;}
            if(candidateY!=rawY&&dy<bestY){bestY=dy;y=candidateY;}
        }
        int hotbarY=dev.sqwat.vela.ui.hud.HudSnap.align(rawY,size.height(),height-22,height);
        if(hotbarY!=rawY)y=hotbarY;
        int hotbarX=dev.sqwat.vela.ui.hud.HudSnap.align(rawX,size.width(),width/2-91,width/2+91);
        if(hotbarX!=rawX)x=hotbarX;
        var origin=layout.nativeOrigin(dragging);
        if(origin!=null){
            if(Math.abs(rawX-origin.x())<=12)x=origin.x();
            if(Math.abs(rawY-origin.y())<=12)y=origin.y();
        }
        layout.move(dragging, x, y, width, height, layout.bounds(minecraft, dragging));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if(picker.open()){picker.release();saveSettings();}
        if(transparencyTarget!=null)return true;
        if (dragging != null) {
            dragging = null;
            layout.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){if(event.key()==GLFW.GLFW_KEY_ESCAPE&&picker.open()){picker.close();saveSettings();return true;}if(event.key()==GLFW.GLFW_KEY_ESCAPE&&transparencyTarget!=null){transparencyTarget=null;return true;}if(event.key()==GLFW.GLFW_KEY_ESCAPE&&resizing!=null){resizing=null;return true;}if(event.key()==GLFW.GLFW_KEY_TAB){minecraft.options.keyPlayerList.setDown(true);return true;}return super.keyPressed(event);}
    @Override public boolean keyReleased(net.minecraft.client.input.KeyEvent event){if(event.key()==GLFW.GLFW_KEY_TAB){minecraft.options.keyPlayerList.setDown(false);return true;}return super.keyReleased(event);}
    @Override public void onClose() { layout.save(); minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    private void drawSafeZones(GuiGraphicsExtractor g) {
        g.outline(6, height - 79, Math.min(164, width / 3), 70, 0x4455CFFF);
        g.outline(width / 2 - 92, height - 33, 184, 27, 0x447C72FF);
        VelaDraw.text(g, font, "Зона чата", 10, height - 75, 0x6655CFFF, VelaDraw.Weight.REGULAR);
        VelaDraw.text(g, font, "Зона хотбара", width / 2 - 88, height - 29, 0x667C72FF, VelaDraw.Weight.REGULAR);
    }

    private void drawGuides(GuiGraphicsExtractor g) {
        if (dragging == null) return;
        g.fill(width / 2, 0, width / 2 + 1, height, 0x4455CFFF);
        g.fill(0, height / 2, width, height / 2 + 1, 0x4455CFFF);
        var p=layout.position(dragging,width,height);var s=layout.bounds(minecraft,dragging);
        int tint=(VelaTheme.PRIMARY&0xFFFFFF)|0x66000000;
        for(ElementHit other:elements)if(!other.id.equals(dragging)){
            for(int gx:new int[]{other.rect.x(),other.rect.x()+other.rect.width()/2,other.rect.x()+other.rect.width()})
                if(Math.abs(p.x()-gx)<=1||Math.abs(p.x()+s.width()/2-gx)<=1||Math.abs(p.x()+s.width()-gx)<=1)g.fill(gx,0,gx+1,height,tint);
            for(int gy:new int[]{other.rect.y(),other.rect.y()+other.rect.height()/2,other.rect.y()+other.rect.height()})
                if(Math.abs(p.y()-gy)<=1||Math.abs(p.y()+s.height()/2-gy)<=1||Math.abs(p.y()+s.height()-gy)<=1)g.fill(0,gy,width,gy+1,tint);
        }
    }

    private float resetHover,exitHover;
    private void button(GuiGraphicsExtractor g, Rect rect, String label, float hover) {
        float factor=2f/Math.max(1,minecraft.getWindow().getGuiScale());g.pose().pushMatrix();g.pose().translate(rect.x(),rect.y());g.pose().scale(factor);
        Rect local=new Rect(0,0,Math.round(rect.width()/factor),Math.round(rect.height()/factor));
          VelaDraw.roundedRect(g,0,0,local.width(),local.height(),5,VelaTheme.mix(0xFF0B0E14,VelaTheme.readableAccentFill(),hover));
        VelaDraw.label(g,label,local,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.TEXT,hover),VelaDraw.Weight.MEDIUM);g.pose().popMatrix();
    }

    private static int snap(int value, int guide) { return Math.abs(value - guide) <= 4 ? guide : value; }
    private record ElementHit(String id, Rect rect) {}
    /** One setting in the middle-click menu: its place, and its description wrapped to at most two lines. */
    private record MenuRow(dev.sqwat.vela.module.setting.Setting<?> setting,int y,int height,List<String> description) {}
    private int menuWidth(){return transparencyTarget!=null&&(transparencyTarget.equals("target")||transparencyTarget.equals("music"))?176:156;}
    private int menuHeight(List<MenuRow> rows){return rows.isEmpty()?MENU_HEAD+6:rows.getLast().y()+rows.getLast().height()+4;}
    private List<MenuRow> menuRows(){
        var rows=new ArrayList<MenuRow>();if(transparencyTarget==null)return rows;
        int y=MENU_HEAD+3,textWidth=menuWidth()-48;
        for(var option:layout.contextSettings(transparencyTarget)){
            String about=option.id().equals("transparent")?"Заменяет фон панели прозрачным стеклом"
                :option instanceof dev.sqwat.vela.module.setting.ColorSetting?"Нажмите, чтобы выбрать цвет":option.description();
            var lines=wrap(about,textWidth);
            int h=16+lines.size()*8;
            rows.add(new MenuRow(option,y,h,lines));y+=h+1;
        }
        return rows;
    }
    private static String optionName(dev.sqwat.vela.module.setting.Setting<?> option){return option.id().equals("transparent")?"Прозрачность":option.name();}
    /** Word-wraps the shown (translated) text into at most two lines, the second cut with an ellipsis. */
    private static List<String> wrap(String text,int width){
        String shown=dev.sqwat.vela.ui.Lang.tr(text);
        if(shown.isBlank())return List.of();
        var lines=new ArrayList<String>();var line=new StringBuilder();
        for(String word:shown.split(" ")){
            String next=line.isEmpty()?word:line+" "+word;
            if(line.isEmpty()||VelaDraw.textWidth(next,VelaDraw.Weight.REGULAR,6.5f)<=width){line.setLength(0);line.append(next);continue;}
            lines.add(line.toString());line.setLength(0);line.append(word);
            if(lines.size()==2)break;
        }
        if(lines.size()<2&&!line.isEmpty())lines.add(line.toString());
        else if(lines.size()==2&&!line.isEmpty())lines.set(1,VelaDraw.ellipsize(lines.get(1)+" "+line,width,VelaDraw.Weight.REGULAR,6.5f));
        return List.copyOf(lines);
    }
    private static void saveSettings(){var services=dev.sqwat.vela.VelaClient.services();if(services!=null)services.configs().requestSave();}
}
