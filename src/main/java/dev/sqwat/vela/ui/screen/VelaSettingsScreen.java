package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.ui.UiSounds;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import java.awt.Color;
import java.util.*;

/** Persistent client UI preferences with real pointer dragging and a cached RGB wheel. */
public final class VelaSettingsScreen extends VelaScreen {
    private static final Identifier WHEEL = Identifier.fromNamespaceAndPath("velaclient", "textures/gui/color_wheel_256.png");
    private final Screen parent;
    private final VelaServices services;
    private final List<Control> controls = new ArrayList<>();
    private Rect back = new Rect(0,0,0,0), hexHit = back, wheelHit = back, content = back;
    private String hex;
    private boolean hexFocused;
    private String dragging;
    private Rect dragRect;
    private float dragScale=1, smoothScroll,targetScroll;
    private String capturingKey;
    private Rect helpHit=new Rect(0,0,0,0);
    private int scroll, maxScroll, tab;
    private final dev.sqwat.vela.ui.animation.FrameClock motionClock=new dev.sqwat.vela.ui.animation.FrameClock();
    private final Map<String,Float> motion=new HashMap<>();
    private float frameDt,entrance;
    private int pointerX,pointerY;
    private final UiControls chrome=new UiControls();
    private final List<Rect> tabs=new ArrayList<>();
    private static final String[] TAB_NAMES={"Appearance","Interface","Sounds","Controls"};

    public VelaSettingsScreen(Screen parent, VelaServices services) {
        super(Component.literal("Settings")); this.parent = parent; this.services = services;
        hex = services.configs().current().accentColor;tab=Math.max(0,Math.min(3,services.configs().current().settingsTab));
    }
    @Override protected float interfaceScale(){return services.configs().current().uiScale;}
    @Override public void renderVela(GuiGraphicsExtractor g,int mx,int my,float dt) {
        pointerX=mx;pointerY=my;frameDt=motionClock.step();smoothScroll=dev.sqwat.vela.ui.animation.Motion.scroll(smoothScroll,targetScroll,frameDt);scroll=Math.round(smoothScroll);entrance=dev.sqwat.vela.ui.animation.Motion.approach(entrance,1,18,frameDt);
        if(parent instanceof VelaScreen screen)screen.renderAsBackdrop(g,width,height,dt);
        g.nextStratum();g.fill(0,0,width,height,0xB805070A);
        int w=Math.min(307,width-24),h=Math.min(300,height-40),x=(width-w)/2,y=(height-h)/2;
        VelaDraw.roundedOutline(g,x,y,w,h,9,VelaTheme.BORDER,VelaTheme.WINDOW);
        VelaDraw.text(g,"Настройки",x+13,y+13,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,10);
        back=new Rect(x+w-31,y+8,20,20);chrome.iconButton(g,"settings-close",back,"x",false,back.contains(mx,my));
        VelaDraw.hairline(g,x,y+35,w,VelaTheme.BORDER);
        tabs.clear();helpHit=new Rect(0,0,0,0);
        for(int i=0;i<4;i++){
            Rect hit=new Rect(x+i*w/4,y+36,w/4,26);tabs.add(hit);
            float tabHover=chrome.animate("tab-hover:"+i,hit.contains(mx,my)?1:0);VelaDraw.roundedRect(g,hit.x()+3,hit.y()+2,hit.width()-6,22,5,VelaTheme.mix(VelaTheme.WINDOW,VelaTheme.SURFACE_HOVER,tabHover));
            VelaDraw.centeredText(g,TAB_NAMES[i],hit.x()+hit.width()/2f,hit.y()+8,i==tab?VelaTheme.TEXT:VelaTheme.TEXT_MUTED,VelaDraw.Weight.MEDIUM,8);
            float selected=chrome.animate("tab-selected:"+i,i==tab?1:0);
            if(selected>.001f)VelaDraw.roundedRect(g,hit.x()+3,hit.y()+2,hit.width()-6,22,5,(Math.round(36*selected)<<24)|(VelaTheme.PRIMARY&0xFFFFFF));
        }
        content=new Rect(x+13,y+76,w-26,h-88);controls.clear();
        hexHit=wheelHit=new Rect(0,0,0,0);
        dev.sqwat.vela.ui.render.UiClip.enable(g,content.x(),content.y(),content.x()+content.width(),content.y()+content.height());
        int cy=content.y()-scroll;var c=services.configs().current();
        if(tab==0){
            VelaDraw.text(g,"АКЦЕНТНЫЙ ЦВЕТ",content.x(),cy,VelaTheme.TEXT_MUTED,VelaDraw.Weight.MEDIUM,7);cy+=18;
            String[] colors={"#756CFF","#55CFFF","#45D39A","#FF667C","#F7BC62","#FF8855"};
            for(int i=0;i<colors.length;i++){Rect r=new Rect(content.x()+i*27,cy,19,19);VelaDraw.roundedOutline(g,r.x(),r.y(),19,19,10,c.accentColor.equalsIgnoreCase(colors[i])?VelaTheme.TEXT:VelaTheme.SURFACE,VelaTheme.parseHex(colors[i])|0xFF000000);controls.add(new Control(colors[i],r));}cy+=33;
            wheelHit=new Rect(content.x()+4,cy,104,104);
            VelaDraw.blit(g,WHEEL,wheelHit.x(),wheelHit.y(),104,104,0,0,256,256,256,256,0xFFFFFFFF);
            int rgb=VelaTheme.parseHex(c.accentColor);float[] hsv=Color.RGBtoHSB(rgb>>16&255,rgb>>8&255,rgb&255,null);
            int px=wheelHit.x()+52+Math.round((float)Math.cos(hsv[0]*Math.PI*2)*hsv[1]*50);
            int py=wheelHit.y()+52+Math.round((float)Math.sin(hsv[0]*Math.PI*2)*hsv[1]*50);
            VelaDraw.roundedOutline(g,px-3,py-3,7,7,4,0xFFFFFFFF,0xFF000000|rgb);
            hexHit=new Rect(content.x()+126,cy+17,content.width()-126,28);
            VelaDraw.roundedOutline(g,hexHit.x(),hexHit.y(),hexHit.width(),28,6,VelaTheme.BORDER,VelaTheme.SURFACE_DEEP);
            VelaDraw.text(g,hex,hexHit.x()+8,hexHit.y()+9,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8);
            if(hexFocused)VelaDraw.roundedRect(g,hexHit.x()+8+Math.round(VelaDraw.textWidth(hex,VelaDraw.Weight.MEDIUM,8)),hexHit.y()+7,1,9,0,VelaDraw.caretColor());
            VelaDraw.roundedRect(g,hexHit.x(),cy+57,hexHit.width(),25,6,0xFF000000|rgb);
            cy+=118;cy=slider(g,"brightness","Яркость цвета",hsv[2],.15,1,cy);
        } else if(tab==1){
            cy=language(g,c,cy);
            cy=slider(g,"scale","UI Scale",c.uiScale,.75,1.25,cy);
            cy=toggle(g,"animations","Анимации",c.animations,cy);
            cy=toggle(g,"motion","Уменьшение движения",c.reducedMotion,cy);
            cy=toggle(g,"mainmenu","Кастомное главное меню",c.replaceMainMenu,cy);
            cy=toggle(g,"panel-menu","Панельное меню модулей",c.panelModuleMenu,cy);
            Rect opt=new Rect(content.x(),cy+6,content.width(),27);button(g,opt,"Оптимизация рендера",false);controls.add(new Control("render-opt",opt));cy+=43;

        } else if(tab==2){
            cy=toggle(g,"sounds","Звуки интерфейса",c.uiSounds,cy);
            cy=slider(g,"volume","Громкость",c.uiSoundVolume,0,1,cy);
            Rect sound=new Rect(content.x(),cy,content.width(),27);
            button(g,sound,dev.sqwat.vela.ui.Lang.tr("Звуковой профиль: ")+dev.sqwat.vela.ui.Lang.tr(UiSounds.preset(c.uiSoundPreset)),false);controls.add(new Control("preset",sound));cy+=37;
            Rect preview=new Rect(content.x(),cy,content.width(),27);button(g,preview,"Прослушать",false);controls.add(new Control("preview",preview));cy+=37;
        } else {
            cy=toggle(g,"show-binds","Всегда показывать бинды",c.alwaysShowBinds,cy);
            String[] names={"Открыть GUI","Поиск","Бинд"};String[] keys={capturingKey!=null&&capturingKey.equals("open")?dev.sqwat.vela.ui.input.BindCapture.prompt("Нажмите…"):dev.sqwat.vela.VelaClient.openKeyLabel(),capturingKey!=null&&capturingKey.equals("search")?dev.sqwat.vela.ui.input.BindCapture.prompt("Нажмите…"):dev.sqwat.vela.ui.input.KeyLabels.of(c.searchCombo(),""),"СКМ"};
            for(int i=0;i<names.length;i++){VelaDraw.text(g,names[i],content.x(),cy+7,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);Rect key=new Rect(content.x()+content.width()-82,cy,82,25);button(g,key,keys[i],false);if(i<2)controls.add(new Control(i==0?"bind-open":"bind-search",key));cy+=35;}
            Rect reset=new Rect(content.x(),cy+9,content.width(),27);button(g,reset,"Сбросить бинды модулей",false);controls.add(new Control("resetbinds",reset));cy+=45;
        }
        g.disableScissor();
        if(content.contains(mx,my)&&helpHit.contains(mx,my)){g.nextStratum();int hx=Math.min(width-220,helpHit.x()),hy=helpHit.y()+20;VelaDraw.roundedOutline(g,hx,hy,210,37,6,VelaTheme.BORDER,VelaTheme.SURFACE);VelaDraw.text(g,"Сокращает движение и длительность",hx+8,hy+8,VelaTheme.TEXT,VelaDraw.Weight.REGULAR,8);VelaDraw.text(g,"анимаций, снижая визуальную нагрузку.",hx+8,hy+21,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8);}
        maxScroll=Math.max(0,cy+scroll-content.y()-content.height());scroll=Math.min(scroll,maxScroll);targetScroll=Math.min(targetScroll,maxScroll);
    }
    private int slider(GuiGraphicsExtractor g,String id,String name,double value,double min,double max,int y) {
        VelaDraw.text(g,font,name,content.x(),y+3,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR);
        String label=String.format(Locale.ROOT,"%.0f%%",value*100);
        VelaDraw.text(g,font,label,content.x()+content.width()-40,y+3,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM);
        Rect track=new Rect(content.x()+3,y+19,content.width()-6,13);
        VelaDraw.roundedRect(g,track.x(),track.y()+5,track.width(),3,2,VelaTheme.BORDER);
        int pos=(int)Math.round(Math.max(0,Math.min(1,(value-min)/(max-min)))*track.width());
        VelaDraw.roundedRect(g,track.x(),track.y()+5,pos,3,2,VelaTheme.PRIMARY);
        VelaDraw.roundedRect(g,track.x()+pos-4,track.y()+2,8,9,4,VelaTheme.TEXT);
        controls.add(new Control(id,track)); return y+42;
    }
    /** Language row: two chips; the names are written in their own language so either is findable. */
    private int language(GuiGraphicsExtractor g,dev.sqwat.vela.core.ClientConfig c,int y){
        VelaDraw.text(g,font,"Язык",content.x(),y+6,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR);
        int x=content.x()+content.width();
        for(String id:new String[]{"ru","en"}){
            String label=id.equals("en")?"English":"Русский";
            int w=dev.sqwat.vela.ui.render.UiKit.chipWidth(label);x-=w;
            var r=new Rect(x,y+4,w,dev.sqwat.vela.ui.render.UiKit.CHIP_H);
            float hover=chrome.animate("lang-hover:"+id,r.contains(pointerX,pointerY)?1:0),active=chrome.animate("lang:"+id,c.language.equals(id)?1:0);
            dev.sqwat.vela.ui.render.UiKit.chip(g,x,y+4,label,active,hover,false);
            controls.add(new Control("lang-"+id,r));x-=4;
        }
        return y+29;
    }
    private int toggle(GuiGraphicsExtractor g,String id,String name,boolean value,int y) {
        float rowHover=chrome.animate("row-hover:"+id,content.contains(pointerX,pointerY)&&new Rect(content.x(),y,content.width(),23).contains(pointerX,pointerY)?1:0);VelaDraw.roundedRect(g,content.x()-3,y,content.width()+6,23,5,VelaTheme.mix(VelaTheme.WINDOW,VelaTheme.SURFACE_HOVER,rowHover));
        VelaDraw.text(g,font,name,content.x(),y+6,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR);
        float p=motion.compute(id,(k,v)->dev.sqwat.vela.ui.animation.Motion.approach(v==null?(value?1f:0f):v,value?1:0,20,frameDt));
        VelaDraw.toggle(g,content.x()+content.width()-27,y+4,p,false);
        if(id.equals("motion")){helpHit=new Rect(content.x()+Math.round(VelaDraw.textWidth(name,VelaDraw.Weight.REGULAR))+6,y+3,13,15);VelaDraw.roundedRect(g,helpHit.x(),helpHit.y(),13,15,4,VelaTheme.SURFACE);VelaDraw.centeredText(g,"?",helpHit.x()+6,helpHit.y()+3,VelaTheme.TEXT_MUTED,VelaDraw.Weight.MEDIUM,8);}
        controls.add(new Control(id,new Rect(content.x(),y,content.width(),23)));return y+29;
    }
    private void button(GuiGraphicsExtractor g,Rect r,String label,boolean primary) {
        VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),7,primary?VelaTheme.PRIMARY:VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.SURFACE_HOVER,chrome.animate("button-hover:"+label,r.contains(pointerX,pointerY)?1:0)));
        VelaDraw.label(g,label,r,primary?VelaTheme.onPrimary():VelaTheme.TEXT,VelaDraw.Weight.MEDIUM);
    }
    @Override public boolean clickVela(MouseButtonEvent event,boolean doubled) {
        if(event.button()!=0)return super.clickVela(event,doubled);
        if(back.contains(event.x(),event.y())){onClose();return true;}
        if(content.contains(event.x(),event.y())&&helpHit.contains(event.x(),event.y()))return true;
        for(int i=0;i<tabs.size();i++)if(tabs.get(i).contains(event.x(),event.y())){UiSounds.play(services,UiSounds.Event.TAB);tab=i;services.configs().current().settingsTab=i;services.configs().requestSave();scroll=0;smoothScroll=targetScroll=0;dragging=null;hexFocused=false;return true;}
        hexFocused=content.contains(event.x(),event.y())&&hexHit.contains(event.x(),event.y());
        if(!content.contains(event.x(),event.y()))return true;
        if(wheelHit.contains(event.x(),event.y())){dragging="wheel";dragRect=wheelHit;dragScale=renderScale();drag(event.x(),event.y());return true;}
        for(Control hit:controls)if(hit.rect.contains(event.x(),event.y())){
            var c=services.configs().current();
            if(hit.id.startsWith("#")){c.accentColor=hit.id;hex=hit.id;VelaTheme.setPrimary(VelaTheme.parseHex(hex));}
            switch(hit.id){
                case "scale","volume","brightness"->{dragging=hit.id;dragRect=hit.rect;dragScale=renderScale();drag(event.x(),event.y());}
                case "bind-open"->capturingKey="open";
                case "bind-search"->capturingKey="search";
                case "animations"->c.animations=!c.animations;
                case "show-binds"->c.alwaysShowBinds=!c.alwaysShowBinds;
                case "motion"->c.reducedMotion=!c.reducedMotion;
                case "sounds"->c.uiSounds=!c.uiSounds;
                case "mainmenu"->c.replaceMainMenu=!c.replaceMainMenu;
                case "panel-menu"->c.panelModuleMenu=!c.panelModuleMenu;
                case "lang-en","lang-ru"->{
                    String id=hit.id.substring(5);var target=id.equals("ru")?dev.sqwat.vela.ui.Lang.Language.RU:dev.sqwat.vela.ui.Lang.Language.EN;
                    // The text changes only while the transition overlay covers the screen.
                    if(!id.equals(c.language))dev.sqwat.vela.ui.LangTransition.begin(target,()->{c.language=id;dev.sqwat.vela.ui.Lang.set(target);services.configs().requestSave();});
                }
                case "resetbinds"->{
                    // Module binds, the keys inside module settings, and the menu's own open and search keys.
                    for(var module:services.modules().all()){
                        module.bind(-1,"None",dev.sqwat.vela.module.ClientModule.BindMode.TOGGLE);
                        for(var setting:module.settings())if(setting instanceof dev.sqwat.vela.module.setting.KeySetting key)key.reset();
                    }
                    c.searchBind=-1;c.searchKey=75;dev.sqwat.vela.VelaClient.resetOpenKey();
                    dev.sqwat.vela.ui.Notifications.show("Бинды сброшены","Модули и клавиши меню вернулись к стандартным","settings");
                }
                case "render-opt"->{UiSounds.play(services,true);minecraft.gui.setScreen(new dev.sqwat.vela.opt.OptScreen(this));return true;}
                case "preview"->UiSounds.play(services,true);
                case "preset"->c.uiSoundPreset=UiSounds.next(c.uiSoundPreset);
            }
            services.configs().requestSave();UiSounds.play(services,true);return true;
        }
        return true;
    }
    private void drag(double mx,double my) {
        var c=services.configs().current();
        mx=mx*renderScale()/dragScale;my=my*renderScale()/dragScale;
        float f=(float)Math.max(0,Math.min(1,(mx-dragRect.x())/dragRect.width()));
        switch(dragging){
            case "scale"->c.uiScale=Math.round((0.75f+f*0.5f)*100)/100f;
            case "volume"->c.uiSoundVolume=Math.round(f*100)/100f;
            case "brightness","wheel"->{
                int rgb=VelaTheme.parseHex(c.accentColor);
                float[] hsv=Color.RGBtoHSB(rgb>>16&255,rgb>>8&255,rgb&255,null);
                if(dragging.equals("wheel")){
                    double dx=mx-dragRect.x()-dragRect.width()/2.0,dy=my-dragRect.y()-dragRect.height()/2.0;
                    hsv[0]=(float)((Math.atan2(dy,dx)/(2*Math.PI)+1)%1);hsv[1]=(float)Math.min(1,Math.hypot(dx,dy)/(dragRect.width()/2.0));
                }else hsv[2]=0.15f+0.85f*f;
                c.accentColor=String.format("#%06X",Color.HSBtoRGB(hsv[0],hsv[1],hsv[2])&0xFFFFFF);hex=c.accentColor;VelaTheme.setPrimary(VelaTheme.parseHex(hex));
            }
        }
        services.configs().requestSave();
    }
    @Override public boolean dragVela(MouseButtonEvent event,double dx,double dy){if(dragging!=null){drag(event.x(),event.y());UiSounds.play(services,UiSounds.Event.SLIDER);return true;}return super.dragVela(event,dx,dy);}
    @Override public boolean releaseVela(MouseButtonEvent event){dragging=null;return super.releaseVela(event);}
    @Override public boolean scrollVela(double x,double y,double horizontal,double vertical){if(dragging==null&&content.contains(x,y))targetScroll=(float)Math.max(0,Math.min(maxScroll,targetScroll-vertical*30));return true;}
    @Override public boolean keyReleased(KeyEvent event){
        if(capturingKey!=null){int code=dev.sqwat.vela.ui.input.BindCapture.release(event.key());if(code!=dev.sqwat.vela.ui.input.BindCapture.PENDING)finishBind(code);return true;}
        return super.keyReleased(event);
    }
    private void finishBind(int code){
        if(capturingKey.equals("open"))dev.sqwat.vela.VelaClient.rebindOpenKey(code);
        else{services.configs().current().searchBind=code;services.configs().requestSave();}
        capturingKey=null;
    }
    @Override public boolean charTyped(CharacterEvent event){
        if(hexFocused){String value=event.codepointAsString();if(value.matches("[#a-fA-F0-9]")&&hex.length()<7){hex+=value;applyHex();}return true;}
        return super.charTyped(event);
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(capturingKey!=null){
            if(event.key()==GLFW.GLFW_KEY_ESCAPE){dev.sqwat.vela.ui.input.BindCapture.reset();capturingKey=null;return true;}
            // Ctrl, Shift or Alt first waits for the main key, so combinations bind the way they are pressed.
            int code=dev.sqwat.vela.ui.input.BindCapture.press(event.key(),event.modifiers());
            if(code!=dev.sqwat.vela.ui.input.BindCapture.PENDING)finishBind(code);
            return true;
        }
        if(hexFocused){
            if(event.key()==GLFW.GLFW_KEY_BACKSPACE&&(event.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0){hex="";return true;}
            if(event.key()==GLFW.GLFW_KEY_BACKSPACE&&!hex.isEmpty()){hex=hex.substring(0,hex.length()-1);applyHex();}
            else if(event.key()==GLFW.GLFW_KEY_ENTER){applyHex();hexFocused=false;}
            else if(event.key()==GLFW.GLFW_KEY_ESCAPE){hex=services.configs().current().accentColor;hexFocused=false;}
            else if((event.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0&&event.key()==GLFW.GLFW_KEY_A)hex="";
            else if((event.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0&&event.key()==GLFW.GLFW_KEY_V){String pasted=minecraft.keyboardHandler.getClipboard().strip();if(pasted.matches("#?[a-fA-F0-9]{6}")){hex=pasted;applyHex();}}
            return true;
        }return super.keyPressed(event);
    }
    private void applyHex(){if(hex.matches("#?[a-fA-F0-9]{6}")){services.configs().current().accentColor="#"+hex.replace("#","").toUpperCase(Locale.ROOT);VelaTheme.setPrimary(VelaTheme.parseHex(hex));services.configs().requestSave();}}
    @Override public void onClose(){services.configs().requestSave();minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    private record Control(String id,Rect rect){}
}
