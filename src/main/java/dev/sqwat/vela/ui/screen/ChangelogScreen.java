package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.ui.Changelog;
import dev.sqwat.vela.ui.Lang;
import dev.sqwat.vela.ui.animation.FrameClock;
import dev.sqwat.vela.ui.animation.Motion;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "What's new" window over the main menu: releases on the left, the chosen release on the right as coloured sections
 * of bullet points. Opening it marks the current version as seen, which clears the dot on the menu pill.
 */
public final class ChangelogScreen extends VelaScreen {
    private static final int SIDE=150,ITEM=34,LINE=12;
    private final Screen parent;
    private final List<Changelog.Version> versions=Changelog.versions();
    private final UiControls controls=new UiControls();
    private final FrameClock clock=new FrameClock();
    private final List<Map.Entry<Rect,Runnable>> actions=new ArrayList<>();
    private int selected;
    private float scroll,targetScroll,maxScroll,entrance;
    private Rect body=new Rect(0,0,0,0);

    public ChangelogScreen(Screen parent){super(Component.literal("Changelog"));this.parent=parent;}
    @Override protected void init(){
        var services=dev.sqwat.vela.VelaClient.services();
        if(services!=null&&!versions.isEmpty()){services.configs().current().lastSeenChangelog=Changelog.current();services.configs().requestSave();}
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){}

    /** True when a newer release than the one last opened is installed. */
    public static boolean unseen(){
        var services=dev.sqwat.vela.VelaClient.services();
        return services!=null&&!Changelog.versions().isEmpty()&&!Changelog.current().equals(services.configs().current().lastSeenChangelog);
    }
    private static String date(String iso){
        try{return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy",Lang.english()?Locale.ENGLISH:Locale.forLanguageTag("ru")));}
        catch(RuntimeException bad){return iso;}
    }

    @Override protected void renderVela(GuiGraphicsExtractor g,int mx,int my,float delta){
        float dt=clock.step();actions.clear();
        entrance=Motion.approach(entrance,1,14,dt);float ease=Motion.easeOutCubic(entrance);
        if(parent instanceof VelaScreen backdrop)backdrop.renderAsBackdrop(g,width,height,delta);
        g.fill(0,0,width,height,(Math.round(0xC0*ease)<<24)|0x05070A);
        int w=Math.min(width-40,620),h=Math.min(height-40,370);
        var window=new Rect((width-w)/2,(height-h)/2+Math.round((1-ease)*10),w,h);
        VelaDraw.roundedRect(g,window.x(),window.y(),window.width(),window.height(),12,VelaTheme.WINDOW);
        new GameUiCanvas(g).border(window.x(),window.y(),window.width(),window.height(),12,VelaTheme.BORDER);
        if(versions.isEmpty()){UiKit.empty(g,window,"star","Записей пока нет",null);return;}
        sidebar(g,window,mx,my);
        content(g,window,mx,my,dt);
        var close=new Rect(window.x()+window.width()-30,window.y()+10,20,20);
        float ch=controls.animate("close",close.contains(mx,my)?1:0);
        if(ch>.01f)VelaDraw.roundedRect(g,close.x(),close.y(),20,20,6,VelaTheme.mix(0x00000000,VelaTheme.SURFACE_HOVER,ch));
        VelaIcons.draw(g,"x",close.x()+5,close.y()+5,10,VelaTheme.mix(VelaTheme.TEXT_MUTED,VelaTheme.TEXT,ch));
        actions.add(Map.entry(close,this::onClose));
    }
    private void sidebar(GuiGraphicsExtractor g,Rect window,int mx,int my){
        var side=new Rect(window.x(),window.y(),SIDE,window.height());
        VelaDraw.roundedRect(g,side.x(),side.y(),side.width(),side.height(),12,VelaTheme.SIDEBAR);
        g.fill(side.x()+side.width()-1,side.y()+12,side.x()+side.width(),side.y()+side.height()-12,VelaTheme.BORDER);
        VelaDraw.logo(g,side.x()+14,side.y()+14,16);
        VelaDraw.text(g,"Что нового",side.x()+36,side.y()+17,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,10f);
        String current=Changelog.current();
        for(int i=0;i<versions.size();i++){
            var v=versions.get(i);var r=new Rect(side.x()+8,side.y()+44+i*(ITEM+2),side.width()-16,ITEM);
            if(r.y()+r.height()>side.y()+side.height()-8)break;
            float hover=controls.animate("vh"+i,r.contains(mx,my)?1:0),active=controls.animate("va"+i,i==selected?1:0);
            int fill=VelaTheme.mix(VelaTheme.mix(0x00000000,0x0EFFFFFF,hover),(VelaTheme.PRIMARY&0xFFFFFF)|0x26000000,active);
            if((fill>>>24)>0)VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),7,fill);
            if(active>.01f)VelaDraw.roundedRect(g,r.x(),r.y()+8,2,Math.round((r.height()-16)*active),1,VelaTheme.PRIMARY);
            VelaDraw.text(g,v.version(),r.x()+10,r.y()+6,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.TEXT,Math.max(active,hover)),VelaDraw.Weight.SEMIBOLD,10f);
            VelaDraw.text(g,date(v.date()),r.x()+10,r.y()+19,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7.5f);
            if(v.version().equals(current)){
                String badge=Lang.pick("Current","Текущая");
                int bw=(int)Math.ceil(VelaDraw.textWidth(badge,VelaDraw.Weight.SEMIBOLD,UiKit.SMALL))+8;
                UiKit.badge(g,r.x()+r.width()-bw-8,r.y()+7,badge,VelaTheme.PRIMARY);
            }
            final int index=i;
            actions.add(Map.entry(r,()->{selected=index;scroll=targetScroll=0;}));
        }
    }
    private void content(GuiGraphicsExtractor g,Rect window,int mx,int my,float dt){
        var v=versions.get(selected);
        int x=window.x()+SIDE+22,top=window.y()+18,width=window.x()+window.width()-22-x;
        VelaDraw.text(g,Lang.pick("Version ","Версия ")+v.version(),x,top,VelaTheme.TEXT_MUTED,VelaDraw.Weight.MEDIUM,8f);
        var titleLines=UiKit.wrap(v.title(),width-30,VelaDraw.Weight.SEMIBOLD,15f);
        int y=top+12;
        for(String line:titleLines){VelaDraw.text(g,line,x,y,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,15f);y+=19;}
        VelaDraw.text(g,date(v.date()),x,y+1,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,8f);
        y+=18;UiKit.divider(g,x,y,width);y+=10;
        body=new Rect(x,y,width,window.y()+window.height()-16-y);
        scroll=Motion.scroll(scroll,targetScroll,dt);
        UiClip.enable(g,body.x()-4,body.y(),body.x()+body.width()+4,body.y()+body.height());
        int cy=body.y()-Math.round(scroll);
        try{
            boolean first=true;
            for(var section:v.sections()){
                if(!first)cy+=12;first=false;
                int color=switch(section.kind()){case NEW->VelaTheme.PRIMARY;case IMPROVED->VelaTheme.CYAN;case FIXED->VelaTheme.POSITIVE;};
                String label=switch(section.kind()){case NEW->Lang.pick("New","Новое");case IMPROVED->Lang.pick("Improved","Улучшено");case FIXED->Lang.pick("Fixed","Исправлено");};
                // Pill: same padding on every side, the dot and the text centred on one line.
                int textW=(int)Math.ceil(VelaDraw.textWidth(label,VelaDraw.Weight.SEMIBOLD,8f)),pillH=16,pillW=8+5+5+textW+9;
                VelaDraw.roundedRect(g,x,cy,pillW,pillH,8,(color&0xFFFFFF)|0x2A000000);
                VelaDraw.roundedRect(g,x+8,cy+6,5,5,3,color);
                VelaDraw.text(g,label,x+18,cy+4,VelaTheme.mix(color,0xFFFFFFFF,.6f),VelaDraw.Weight.SEMIBOLD,8f);
                cy+=pillH+9;
                for(String item:section.items()){
                    var lines=UiKit.wrap(item,width-16,VelaDraw.Weight.REGULAR,8.5f);
                    VelaDraw.roundedRect(g,x+4,cy+4,4,4,2,(color&0xFFFFFF)|0xC0000000); // centred on the first line
                    for(String line:lines){VelaDraw.text(g,line,x+16,cy,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8.5f);cy+=LINE;}
                    cy+=6;
                }
            }
        }finally{g.disableScissor();}
        maxScroll=Math.max(0,cy+Math.round(scroll)-body.y()-body.height());
        targetScroll=Math.clamp(targetScroll,0,maxScroll);
        // Soft edges where text scrolls out of view.
        if(scroll>1)g.fillGradient(body.x()-4,body.y(),body.x()+body.width()+4,body.y()+14,VelaTheme.WINDOW,VelaTheme.WINDOW&0xFFFFFF);
        if(scroll<maxScroll-1)g.fillGradient(body.x()-4,body.y()+body.height()-14,body.x()+body.width()+4,body.y()+body.height(),VelaTheme.WINDOW&0xFFFFFF,VelaTheme.WINDOW);
    }

    @Override protected boolean clickVela(MouseButtonEvent e,boolean doubled){
        for(var a:actions)if(a.getKey().contains(e.x(),e.y())){a.getValue().run();return true;}
        return true;
    }
    @Override protected boolean scrollVela(double x,double y,double h,double v){targetScroll=Math.clamp(targetScroll-(float)v*28,0,maxScroll);return true;}
    @Override public boolean keyPressed(KeyEvent event){
        switch(event.key()){
            case GLFW.GLFW_KEY_ESCAPE->onClose();
            case GLFW.GLFW_KEY_DOWN->{selected=Math.min(versions.size()-1,selected+1);scroll=targetScroll=0;}
            case GLFW.GLFW_KEY_UP->{selected=Math.max(0,selected-1);scroll=targetScroll=0;}
            default->{return false;}
        }
        return true;
    }
}
