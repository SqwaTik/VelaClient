package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.module.visual.DeathRecap;
import dev.sqwat.vela.module.visual.DeathRecap.Recording;
import dev.sqwat.vela.module.visual.DeathReplay;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Death Recap viewer: the world shows the replay (DeathReplay), this screen draws the story around it — what happened
 * when, a health graph on a scrubbable timeline, playback speed and camera. Drag to orbit, wheel to zoom, Space pauses.
 */
public final class DeathRecapScreen extends VelaScreen {
    private static final int EVENT_H=30,PANEL_W=184,BAR_H=78;
    private final Recording recording;
    private final Screen parent;
    private final UiControls controls=new UiControls();
    private final List<Map.Entry<Rect,Runnable>> actions=new ArrayList<>();
    private Rect track=new Rect(0,0,0,0),events=track,bar=track;
    private boolean started,scrubbing,orbiting;
    private double lastX,lastY;
    private float eventScroll;

    public DeathRecapScreen(Recording recording,Screen parent){super(Component.literal("Death Recap"));this.recording=recording;this.parent=parent;}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){if(!started){DeathReplay.start(recording);started=true;}}
    @Override public void removed(){DeathReplay.stop();super.removed();}
    @Override public void onClose(){
        var player=minecraft.player;
        minecraft.gui.setScreen(player!=null&&player.isDeadOrDying()&&parent!=null?parent:null);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){}

    private int tickAt(float time){return recording.firstTick()+Math.round(time);}
    private String clock(int tick){float s=(tick-recording.deathTick())/20f;return (s>0?"+":s<0?"−":"")+String.format(Locale.ROOT,"%.1f с",Math.abs(s));}

    @Override protected void renderVela(GuiGraphicsExtractor g,int mx,int my,float delta){
        DeathReplay.frame();actions.clear();
        // Soft vignette top and bottom keeps the text readable over any scene.
        g.fillGradient(0,0,width,70,0xB0050608,0x00050608);
        g.fillGradient(0,height-120,width,height,0x00050608,0xC0050608);
        header(g);
        eventList(g,mx,my);
        controlsBar(g,mx,my);
    }
    private void header(GuiGraphicsExtractor g){
        VelaDraw.text(g,"Death Recap",16,14,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,15f);
        String cause=recording.message()!=null?recording.message().getString():recording.killerName()!=null?"Убил: "+recording.killerName():"Смерть";
        VelaDraw.text(g,VelaDraw.ellipsize(cause,width-32,VelaDraw.Weight.REGULAR,8.5f),16,33,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8.5f);
        float damage=0;int hits=0;
        for(var e:recording.events())if(e.kind()==DeathRecap.Kind.HIT){damage+=e.amount();hits++;}
        var death=frameAt(recording.deathTick());
        int x=16,y=47;
        for(String stat:List.of(hits+" "+plural(hits,"удар","удара","ударов"),String.format(Locale.ROOT,"%.1f урона",damage),
                death.armor()<0?"без брони":"броня "+death.armor()+"%","пинг "+death.ping()+" мс")){
            x+=UiKit.chip(g,x,y,stat,0,0,false).width()+4;
        }
    }
    private DeathRecap.Frame frameAt(int tick){
        var frames=recording.frames();
        for(var f:frames)if(f.tick()>=tick)return f;
        return frames.getLast();
    }
    private void eventList(GuiGraphicsExtractor g,int mx,int my){
        var list=recording.events();
        int top=70,bottom=height-BAR_H-24;
        events=new Rect(16,top,PANEL_W,Math.max(0,bottom-top));
        UiKit.caption(g,"События",events.x(),top-1);
        var area=new Rect(events.x(),top+11,events.width(),events.height()-11);
        if(list.isEmpty()){VelaDraw.text(g,"Ничего не произошло",area.x(),area.y()+4,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,8f);return;}
        eventScroll=Math.clamp(eventScroll,0,Math.max(0,list.size()*(EVENT_H+3)-area.height()));
        int now=tickAt(DeathReplay.time());
        UiClip.enable(g,area.x(),area.y(),area.x()+area.width(),area.y()+area.height());
        try{
            for(int i=0;i<list.size();i++){
                var e=list.get(i);int y=area.y()+i*(EVENT_H+3)-Math.round(eventScroll);
                if(y+EVENT_H<area.y()||y>area.y()+area.height())continue;
                var r=new Rect(area.x(),y,area.width(),EVENT_H);
                boolean live=Math.abs(now-e.tick())<=4;
                UiKit.card(g,r,controls.animate("ev-h"+i,r.contains(mx,my)?1:0),controls.animate("ev-l"+i,live?1:0));
                int color=switch(e.kind()){case HIT->VelaTheme.DANGER;case TOTEM->0xFFFFC94D;case CONSUME->VelaTheme.POSITIVE;case EFFECT->VelaTheme.PRIMARY;};
                g.fill(r.x()+5,r.y()+6,r.x()+7,r.y()+EVENT_H-6,color);
                String time=clock(e.tick());float tw=VelaDraw.textWidth(time,VelaDraw.Weight.MEDIUM,7.5f);
                VelaDraw.text(g,time,r.x()+r.width()-7-tw,r.y()+6,VelaTheme.TEXT_MUTED,VelaDraw.Weight.MEDIUM,7.5f);
                String title=e.kind()==DeathRecap.Kind.HIT&&e.amount()>0?String.format(Locale.ROOT,"−%.1f  %s",e.amount(),e.title()):e.title();
                VelaDraw.text(g,VelaDraw.ellipsize(title,r.width()-tw-24,VelaDraw.Weight.MEDIUM,8.5f),r.x()+13,r.y()+5,e.kind()==DeathRecap.Kind.HIT?0xFFFF8A9A:VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8.5f);
                String detail=switch(e.kind()){case HIT->e.detail().isEmpty()?"":e.detail();case TOTEM->"сработал тотем";case CONSUME->"использовано";case EFFECT->"новый эффект";};
                if(!detail.isEmpty())VelaDraw.text(g,VelaDraw.ellipsize(detail,r.width()-20,VelaDraw.Weight.REGULAR,7.5f),r.x()+13,r.y()+17,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7.5f);
                actions.add(Map.entry(r,()->{DeathReplay.seek(e.tick()-recording.firstTick()-20);if(!DeathReplay.playing())DeathReplay.toggle();}));
            }
        }finally{g.disableScissor();}
    }
    private void controlsBar(GuiGraphicsExtractor g,int mx,int my){
        bar=new Rect(16,height-BAR_H-14,width-32,BAR_H);
        VelaDraw.roundedRect(g,bar.x(),bar.y(),bar.width(),bar.height(),10,0xD80E1119);
        new GameUiCanvas(g).border(bar.x(),bar.y(),bar.width(),bar.height(),10,VelaTheme.BORDER);
        int x=bar.x()+12,y=bar.y()+10;
        // Play / pause
        var play=new Rect(x,y-2,20,20);float ph=controls.animate("play",play.contains(mx,my)?1:0);
        VelaDraw.roundedRect(g,play.x(),play.y(),20,20,10,VelaTheme.mix(VelaTheme.PRIMARY,0xFFFFFFFF,ph*.15f));
        int ic=VelaTheme.onPrimary();
        if(DeathReplay.playing()){g.fill(play.x()+7,play.y()+6,play.x()+9,play.y()+14,ic);g.fill(play.x()+11,play.y()+6,play.x()+13,play.y()+14,ic);}
        else for(int i=0;i<8;i++){int half=Math.min(i,7-i);g.fill(play.x()+8,play.y()+6+i,play.x()+9+half,play.y()+7+i,ic);}
        actions.add(Map.entry(play,DeathReplay::toggle));
        x+=28;
        int now=tickAt(DeathReplay.time());
        VelaDraw.text(g,clock(now),x,y+3,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,9f);
        x+=52;
        for(float speed:new float[]{.25f,.5f,1f}){
            String label=(speed==1?"1":speed==.5f?"0.5":"0.25")+"×";
            var chip=new Rect(x,y+1,UiKit.chipWidth(label),UiKit.CHIP_H);
            UiKit.chip(g,x,y+1,label,controls.animate("sp"+label,DeathReplay.speed()==speed?1:0),controls.animate("sph"+label,chip.contains(mx,my)?1:0),false);
            actions.add(Map.entry(chip,()->DeathReplay.speed(speed)));x+=chip.width()+4;
        }
        int rx=bar.x()+bar.width()-12;
        for(var view:new DeathReplay.View[]{DeathReplay.View.KILLER,DeathReplay.View.SELF,DeathReplay.View.ORBIT}){
            if(view==DeathReplay.View.KILLER&&!DeathReplay.hasKiller())continue;
            String label=switch(view){case ORBIT->"Свободная камера";case SELF->"Моими глазами";case KILLER->"Глазами убийцы";};
            int w=UiKit.chipWidth(label);rx-=w;var chip=new Rect(rx,y+1,w,UiKit.CHIP_H);
            UiKit.chip(g,rx,y+1,label,controls.animate("v"+view,DeathReplay.view()==view?1:0),controls.animate("vh"+view,chip.contains(mx,my)?1:0),false);
            actions.add(Map.entry(chip,()->DeathReplay.view(view)));rx-=4;
        }
        // Timeline with health graph
        track=new Rect(bar.x()+12,bar.y()+bar.height()-16,bar.width()-24,4);
        graph(g,new Rect(track.x(),track.y()-26,track.width(),22));
        VelaDraw.roundedRect(g,track.x(),track.y(),track.width(),track.height(),2,0x40FFFFFF);
        float length=Math.max(1,DeathReplay.length()),progress=DeathReplay.time()/length;
        VelaDraw.roundedRect(g,track.x(),track.y(),Math.max(4,Math.round(track.width()*progress)),track.height(),2,VelaTheme.PRIMARY);
        int deathX=track.x()+Math.round(track.width()*(recording.deathTick()-recording.firstTick())/length);
        g.fill(deathX,track.y()-30,deathX+1,track.y()+track.height()+2,0x80FF6178);
        for(var e:recording.events()){
            int ex=track.x()+Math.round(track.width()*(e.tick()-recording.firstTick())/length);
            int color=switch(e.kind()){case HIT->VelaTheme.DANGER;case TOTEM->0xFFFFC94D;case CONSUME->VelaTheme.POSITIVE;case EFFECT->VelaTheme.PRIMARY;};
            g.fill(ex,track.y()-3,ex+2,track.y()+track.height()+3,color);
        }
        int knob=track.x()+Math.round(track.width()*progress);
        VelaDraw.roundedRect(g,knob-4,track.y()-3,8,10,4,0xFFFFFFFF);
        String hint="ЛКМ + движение — вращать · колесо — приблизить · Пробел — пауза · ← → — перемотка";
        VelaDraw.text(g,hint,width-16-VelaDraw.textWidth(hint,VelaDraw.Weight.REGULAR,7f),height-11,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7f);
    }
    /** Health plus absorption over the recording; the played part is brighter. */
    private void graph(GuiGraphicsExtractor g,Rect r){
        var frames=recording.frames();if(frames.size()<2)return;
        float max=1;for(var f:frames)max=Math.max(max,f.maxHealth()+f.absorption());
        float progress=DeathReplay.time()/Math.max(1,DeathReplay.length());
        for(int px=0;px<r.width();px++){
            var f=frames.get(Math.min(frames.size()-1,Math.round(px/(float)(r.width()-1)*(frames.size()-1))));
            float value=(f.health()+f.absorption())/max;int h=Math.max(1,Math.round(value*r.height()));
            int color=VelaTheme.mix(VelaTheme.DANGER,VelaTheme.POSITIVE,Math.clamp(f.health()/Math.max(1,f.maxHealth()),0,1));
            int alpha=px<=progress*r.width()?0x70:0x28;
            g.fill(r.x()+px,r.y()+r.height()-h,r.x()+px+1,r.y()+r.height(),(alpha<<24)|(color&0xFFFFFF));
        }
    }

    @Override protected boolean clickVela(MouseButtonEvent e,boolean doubled){
        for(var a:actions)if(a.getKey().contains(e.x(),e.y())){a.getValue().run();return true;}
        if(new Rect(track.x()-4,track.y()-30,track.width()+8,track.height()+36).contains(e.x(),e.y())){scrubbing=true;scrub(e.x());return true;}
        if(!bar.contains(e.x(),e.y())&&!events.contains(e.x(),e.y())&&e.button()==0){orbiting=true;lastX=e.x();lastY=e.y();}
        return true;
    }
    private void scrub(double x){DeathReplay.seek((float)((x-track.x())/track.width()*DeathReplay.length()));}
    @Override protected boolean dragVela(MouseButtonEvent e,double dx,double dy){
        if(scrubbing){scrub(e.x());return true;}
        if(orbiting){DeathReplay.orbit((float)(e.x()-lastX)*.5f,(float)(e.y()-lastY)*.4f);lastX=e.x();lastY=e.y();}
        return true;
    }
    @Override protected boolean releaseVela(MouseButtonEvent e){scrubbing=orbiting=false;return true;}
    @Override protected boolean scrollVela(double x,double y,double h,double v){
        if(events.contains(x,y))eventScroll-=(float)v*(EVENT_H+3)*2;else DeathReplay.zoom((float)v);
        return true;
    }
    @Override public boolean keyPressed(KeyEvent event){
        switch(event.key()){
            case GLFW.GLFW_KEY_ESCAPE->onClose();
            case GLFW.GLFW_KEY_SPACE->DeathReplay.toggle();
            case GLFW.GLFW_KEY_LEFT->DeathReplay.seek(DeathReplay.time()-20);
            case GLFW.GLFW_KEY_RIGHT->DeathReplay.seek(DeathReplay.time()+20);
            case GLFW.GLFW_KEY_1->DeathReplay.view(DeathReplay.View.ORBIT);
            case GLFW.GLFW_KEY_2->DeathReplay.view(DeathReplay.View.SELF);
            case GLFW.GLFW_KEY_3->{if(DeathReplay.hasKiller())DeathReplay.view(DeathReplay.View.KILLER);}
            default->{return false;}
        }
        return true;
    }
    private static String plural(int n,String one,String few,String many){
        int m10=n%10,m100=n%100;
        return m10==1&&m100!=11?one:m10>=2&&m10<=4&&(m100<12||m100>14)?few:many;
    }
}
