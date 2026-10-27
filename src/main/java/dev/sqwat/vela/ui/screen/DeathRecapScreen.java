package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.module.visual.DeathRecap;
import dev.sqwat.vela.module.visual.DeathRecap.Recording;
import dev.sqwat.vela.module.visual.DeathReplay;
import dev.sqwat.vela.module.visual.ReplayFiles;
import dev.sqwat.vela.module.visual.ReplayVideo;
import dev.sqwat.vela.ui.Lang;
import dev.sqwat.vela.ui.input.GameTextInput;
import dev.sqwat.vela.ui.input.TextEdit;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Death Recap viewer: the world shows the replay (DeathReplay), this screen draws the story around it — what happened
 * when, a health graph on a scrubbable timeline, playback speed and camera. Replays can be saved with their own
 * settings and reopened from the library. Drag to orbit, wheel to zoom, Space pauses.
 */
public final class DeathRecapScreen extends VelaScreen {
    private static final int EVENT_H=30,PANEL_W=184,BAR_H=78;
    private enum Modal{NONE,SAVE,LIBRARY}
    private final Recording recording;
    private final Screen parent;
    private final ReplayFiles.Options initial;
    private final UiControls controls=new UiControls();
    private final List<Map.Entry<Rect,Runnable>> actions=new ArrayList<>();
    private Rect track=new Rect(0,0,0,0),events=track,bar=track,modalRect=track;
    private boolean started,scrubbing,orbiting;
    private double lastX,lastY;
    private float eventScroll;
    private Modal modal=Modal.NONE;
    // Save window state
    private final TextEdit name=new TextEdit();
    private DeathReplay.View saveView=DeathReplay.View.ORBIT;
    private boolean saveHud;
    private float saveSpeed=1f;
    private boolean saveVideo=true;
    /** Events with hits from the same source within two seconds merged into one row. */
    private final List<Row> rows;
    private record Row(DeathRecap.Event first,int lastTick,float amount,int count){}
    private int saveSeconds=0;
    private List<ReplayFiles.Saved> library=List.of();
    // Health graph runs, rebuilt only when the width changes
    private int[] graphX=new int[0],graphW=new int[0],graphH=new int[0],graphColor=new int[0];
    private int graphWidth=-1;

    public DeathRecapScreen(Recording recording,Screen parent){this(recording,parent,null);}
    public DeathRecapScreen(Recording recording,Screen parent,ReplayFiles.Options options){
        super(Component.literal("Death Recap"));this.recording=recording;this.parent=parent;this.initial=options;
        this.rows=merge(recording.events());
    }
    static final int MERGE_GAP=40;
    private static List<Row> merge(List<DeathRecap.Event> events){
        var rows=new ArrayList<Row>();
        for(var e:events){
            int last=rows.size()-1;
            if(last>=0){
                var row=rows.get(last);var f=row.first();
                if(f.kind()==e.kind()&&f.title().equals(e.title())&&java.util.Objects.equals(f.source(),e.source())&&e.tick()-row.lastTick()<=MERGE_GAP){
                    rows.set(last,new Row(f,e.tick(),row.amount()+e.amount(),row.count()+1));continue;
                }
            }
            rows.add(new Row(e,e.tick(),e.amount(),1));
        }
        return List.copyOf(rows);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){
        if(started)return;
        started=true;
        if(initial==null)DeathReplay.start(recording);
        else DeathReplay.start(recording,initial.view(),initial.hud(),initial.speed());
    }
    @Override public void removed(){ReplayVideo.cancel();DeathReplay.stop();super.removed();}
    @Override public void onClose(){
        var player=minecraft.player;
        minecraft.gui.setScreen(player!=null&&player.isDeadOrDying()&&parent!=null?parent:null);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){}

    private static final net.minecraft.resources.Identifier CROSSHAIR=net.minecraft.resources.Identifier.withDefaultNamespace("hud/crosshair");
    private int tickAt(float time){return recording.firstTick()+Math.round(time);}
    private String clock(int tick){float s=(tick-recording.deathTick())/20f;return (s>0?"+":s<0?"−":"")+String.format(Locale.ROOT,Lang.tr("%.1f с"),Math.abs(s));}

    @Override protected void renderVela(GuiGraphicsExtractor g,int mx,int my,float delta){
        DeathReplay.frame();actions.clear();
        // First-person views get the ordinary crosshair; the free camera has none.
        if(DeathReplay.view()!=DeathReplay.View.ORBIT&&!DeathReplay.showsHud())
            g.blitSprite(net.minecraft.client.renderer.RenderPipelines.CROSSHAIR,CROSSHAIR,(width-15)/2,(height-15)/2,15,15);
        if(ReplayVideo.running())return; // the video shows the scene only
        boolean blocked=modal!=Modal.NONE;int px=blocked?-10000:mx,py=blocked?-10000:my;
        // Soft vignette top and bottom keeps the text readable over any scene.
        g.fillGradient(0,0,width,70,0xB0050608,0x00050608);
        g.fillGradient(0,height-120,width,height,0x00050608,0xC0050608);
        header(g,px,py);
        eventList(g,px,py);
        controlsBar(g,px,py);
        if(blocked){actions.clear();g.nextStratum();g.fill(0,0,width,height,0x99050608);if(modal==Modal.SAVE)saveWindow(g,mx,my);else libraryWindow(g,mx,my);}
    }
    private void header(GuiGraphicsExtractor g,int mx,int my){
        VelaDraw.text(g,"Death Recap",16,14,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,15f);
        String cause=recording.message()!=null?recording.message().getString():recording.killerName()!=null?Lang.tr("Убил: ")+recording.killerName():"Смерть";
        VelaDraw.text(g,VelaDraw.ellipsize(cause,width-260,VelaDraw.Weight.REGULAR,8.5f),16,33,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8.5f);
        float damage=0;int hits=0;
        for(var e:recording.events())if(e.kind()==DeathRecap.Kind.HIT){damage+=e.amount();hits++;}
        var death=frameAt(recording.deathTick());
        int x=16,y=47;
        for(String stat:List.of(hits+" "+plural(hits,"удар","удара","ударов"),String.format(Locale.ROOT,Lang.tr("%.1f урона"),damage),
                death.armor()<0?"без брони":Lang.tr("броня ")+death.armor()+"%",Lang.tr("пинг ")+death.ping()+Lang.tr(" мс"))){
            x+=UiKit.chip(g,x,y,stat,0,0,false).width()+4;
        }
        // Save / library, top right
        int rx=width-16;
        String libraryLabel="Сохранённые";int lw=UiKit.chipWidth(libraryLabel);rx-=lw;
        var lib=new Rect(rx,16,lw,UiKit.CHIP_H);
        UiKit.chip(g,rx,16,libraryLabel,0,controls.animate("lib",lib.contains(mx,my)?1:0),false);
        actions.add(Map.entry(lib,this::openLibrary));
        String saveLabel="Сохранить";int sw=UiKit.chipWidth(saveLabel);rx-=sw+4;
        var save=new Rect(rx,16,sw,UiKit.CHIP_H);
        UiKit.chip(g,rx,16,saveLabel,1,controls.animate("save",save.contains(mx,my)?1:0),false);
        actions.add(Map.entry(save,this::openSave));
    }
    private DeathRecap.Frame frameAt(int tick){
        var frames=recording.frames();
        for(var f:frames)if(f.tick()>=tick)return f;
        return frames.getLast();
    }
    private void eventList(GuiGraphicsExtractor g,int mx,int my){
        var list=rows;
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
                var row=list.get(i);var e=row.first();int y=area.y()+i*(EVENT_H+3)-Math.round(eventScroll);
                if(y+EVENT_H<area.y()||y>area.y()+area.height())continue;
                var r=new Rect(area.x(),y,area.width(),EVENT_H);
                boolean live=now>=e.tick()-4&&now<=row.lastTick()+4;
                UiKit.card(g,r,controls.animate("ev-h"+i,r.contains(mx,my)?1:0),controls.animate("ev-l"+i,live?1:0));
                int color=kindColor(e.kind());
                g.fill(r.x()+5,r.y()+6,r.x()+7,r.y()+EVENT_H-6,color);
                String time=clock(e.tick())+(row.count()>1?"  ×"+row.count():"");float tw=VelaDraw.textWidth(time,VelaDraw.Weight.MEDIUM,7.5f);
                VelaDraw.text(g,time,r.x()+r.width()-7-tw,r.y()+6,VelaTheme.TEXT_MUTED,VelaDraw.Weight.MEDIUM,7.5f);
                String title=e.kind()==DeathRecap.Kind.HIT&&row.amount()>0?String.format(Locale.ROOT,"−%.1f  %s",row.amount(),Lang.tr(e.title())):Lang.tr(e.title());
                VelaDraw.text(g,VelaDraw.ellipsize(title,r.width()-tw-24,VelaDraw.Weight.MEDIUM,8.5f),r.x()+13,r.y()+5,e.kind()==DeathRecap.Kind.HIT?0xFFFF8A9A:VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8.5f);
                String detail=switch(e.kind()){case HIT->e.detail().isEmpty()?"":e.detail();case TOTEM->"сработал тотем";case CONSUME->"использовано";case EFFECT->"новый эффект";};
                if(!detail.isEmpty())VelaDraw.text(g,VelaDraw.ellipsize(detail,r.width()-20,VelaDraw.Weight.REGULAR,7.5f),r.x()+13,r.y()+17,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7.5f);
                actions.add(Map.entry(r,()->{DeathReplay.seek(e.tick()-recording.firstTick()-20);if(!DeathReplay.playing())DeathReplay.toggle();}));
            }
        }finally{g.disableScissor();}
    }
    private static int kindColor(DeathRecap.Kind kind){return switch(kind){case HIT->VelaTheme.DANGER;case TOTEM->0xFFFFC94D;case CONSUME->VelaTheme.POSITIVE;case EFFECT->VelaTheme.PRIMARY;};}
    private static String viewLabel(DeathReplay.View view){return switch(view){case ORBIT->"Свободная камера";case SELF->"Моими глазами";case KILLER->"Глазами убийцы";};}
    private void controlsBar(GuiGraphicsExtractor g,int mx,int my){
        bar=new Rect(16,height-BAR_H-14,width-32,BAR_H);
        VelaDraw.roundedRect(g,bar.x(),bar.y(),bar.width(),bar.height(),10,0xD80E1119);
        new GameUiCanvas(g).border(bar.x(),bar.y(),bar.width(),bar.height(),10,VelaTheme.BORDER);
        int x=bar.x()+12,y=bar.y()+10;
        var play=new Rect(x,y-2,20,20);float ph=controls.animate("play",play.contains(mx,my)?1:0);
        VelaDraw.roundedRect(g,play.x(),play.y(),20,20,10,VelaTheme.mix(VelaTheme.PRIMARY,0xFFFFFFFF,ph*.15f));
        int ic=VelaTheme.onPrimary();
        if(DeathReplay.playing()){g.fill(play.x()+7,play.y()+6,play.x()+9,play.y()+14,ic);g.fill(play.x()+11,play.y()+6,play.x()+13,play.y()+14,ic);}
        else for(int i=0;i<8;i++){int half=Math.min(i,7-i);g.fill(play.x()+8,play.y()+6+i,play.x()+9+half,play.y()+7+i,ic);}
        actions.add(Map.entry(play,DeathReplay::toggle));
        x+=28;
        VelaDraw.text(g,clock(tickAt(DeathReplay.time())),x,y+3,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,9f);
        x+=52;
        x=speedChips(g,x,y+1,mx,my,DeathReplay.speed(),DeathReplay::speed,"sp");
        int rx=bar.x()+bar.width()-12;
        for(var view:new DeathReplay.View[]{DeathReplay.View.KILLER,DeathReplay.View.SELF,DeathReplay.View.ORBIT}){
            if(view==DeathReplay.View.KILLER&&!DeathReplay.hasKiller())continue;
            String label=viewLabel(view);
            int w=UiKit.chipWidth(label);rx-=w;var chip=new Rect(rx,y+1,w,UiKit.CHIP_H);
            UiKit.chip(g,rx,y+1,label,controls.animate("v"+view,DeathReplay.view()==view?1:0),controls.animate("vh"+view,chip.contains(mx,my)?1:0),false);
            actions.add(Map.entry(chip,()->DeathReplay.view(view)));rx-=4;
        }
        track=new Rect(bar.x()+12,bar.y()+bar.height()-16,bar.width()-24,4);
        graph(g,new Rect(track.x(),track.y()-26,track.width(),22));
        VelaDraw.roundedRect(g,track.x(),track.y(),track.width(),track.height(),2,0x40FFFFFF);
        float length=Math.max(1,DeathReplay.length()),progress=DeathReplay.time()/length;
        VelaDraw.roundedRect(g,track.x(),track.y(),Math.max(4,Math.round(track.width()*progress)),track.height(),2,VelaTheme.PRIMARY);
        int deathX=track.x()+Math.round(track.width()*(recording.deathTick()-recording.firstTick())/length);
        g.fill(deathX,track.y()-30,deathX+1,track.y()+track.height()+2,0x80FF6178);
        for(var e:recording.events()){
            int ex=track.x()+Math.round(track.width()*(e.tick()-recording.firstTick())/length);
            g.fill(ex,track.y()-3,ex+2,track.y()+track.height()+3,kindColor(e.kind()));
        }
        int knob=track.x()+Math.round(track.width()*progress);
        VelaDraw.roundedRect(g,knob-4,track.y()-3,8,10,4,0xFFFFFFFF);
        String hint="ЛКМ + движение — вращать · колесо — приблизить · Пробел — пауза · ← → — перемотка";
        VelaDraw.text(g,hint,width-16-VelaDraw.textWidth(hint,VelaDraw.Weight.REGULAR,7f),height-11,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7f);
    }
    private int speedChips(GuiGraphicsExtractor g,int x,int y,int mx,int my,float current,java.util.function.Consumer<Float> set,String key){
        for(float speed:new float[]{.25f,.5f,1f}){
            String label=(speed==1?"1":speed==.5f?"0.5":"0.25")+"×";
            var chip=new Rect(x,y,UiKit.chipWidth(label),UiKit.CHIP_H);
            UiKit.chip(g,x,y,label,controls.animate(key+label,current==speed?1:0),controls.animate(key+"h"+label,chip.contains(mx,my)?1:0),false);
            actions.add(Map.entry(chip,()->set.accept(speed)));x+=chip.width()+4;
        }
        return x;
    }
    /**
     * Health plus absorption over the recording; the played part is brighter. Columns with the same height and colour
     * are merged into one fill, rebuilt only when the width changes: a few dozen quads instead of one per pixel.
     */
    private void graph(GuiGraphicsExtractor g,Rect r){
        var frames=recording.frames();if(frames.size()<2||r.width()<=0)return;
        if(graphWidth!=r.width())buildGraph(r.width(),r.height());
        int played=Math.round(DeathReplay.time()/Math.max(1,DeathReplay.length())*r.width());
        for(int i=0;i<graphX.length;i++){
            int x0=graphX[i],x1=x0+graphW[i],h=graphH[i],c=graphColor[i]&0xFFFFFF;
            int split=Math.clamp(played,x0,x1);
            if(split>x0)g.fill(r.x()+x0,r.y()+r.height()-h,r.x()+split,r.y()+r.height(),0x70000000|c);
            if(x1>split)g.fill(r.x()+split,r.y()+r.height()-h,r.x()+x1,r.y()+r.height(),0x28000000|c);
        }
    }
    private void buildGraph(int width,int height){
        var frames=recording.frames();
        float max=1;for(var f:frames)max=Math.max(max,f.maxHealth()+f.absorption());
        var xs=new ArrayList<Integer>();var ws=new ArrayList<Integer>();var hs=new ArrayList<Integer>();var cs=new ArrayList<Integer>();
        int runStart=0,runH=-1,runC=0;
        for(int px=0;px<=width;px++){
            int h=-1,c=0;
            if(px<width){
                var f=frames.get(Math.min(frames.size()-1,Math.round(px/(float)Math.max(1,width-1)*(frames.size()-1))));
                h=Math.max(1,Math.round((f.health()+f.absorption())/max*height));
                c=VelaTheme.mix(VelaTheme.DANGER,VelaTheme.POSITIVE,Math.clamp(f.health()/Math.max(1,f.maxHealth()),0,1));
            }
            if(px==width||h!=runH||c!=runC){
                if(runH>0){xs.add(runStart);ws.add(px-runStart);hs.add(runH);cs.add(runC);}
                runStart=px;runH=h;runC=c;
            }
        }
        graphX=xs.stream().mapToInt(Integer::intValue).toArray();graphW=ws.stream().mapToInt(Integer::intValue).toArray();
        graphH=hs.stream().mapToInt(Integer::intValue).toArray();graphColor=cs.stream().mapToInt(Integer::intValue).toArray();
        graphWidth=width;
    }

    // ---- Save window ----
    private void openSave(){
        modal=Modal.SAVE;saveView=DeathReplay.view();saveSpeed=1f;saveHud=false;saveSeconds=0;
        String who=recording.killerName()!=null?recording.killerName():Lang.tr("Смерть");
        name.set(who+" · "+DateTimeFormatter.ofPattern("d MMM HH:mm",Lang.english()?Locale.ENGLISH:Locale.forLanguageTag("ru")).format(Instant.ofEpochMilli(recording.recordedAt()).atZone(ZoneId.systemDefault())));
        name.selectAll();
    }
    private void saveWindow(GuiGraphicsExtractor g,int mx,int my){
        int w=320,h=270;var r=new Rect((width-w)/2,(height-h)/2,w,h);modalRect=r;
        var content=UiKit.window(g,r,"Сохранить повтор","Настройки сохранённой записи");
        int x=content.x(),y=content.y(),cw=content.width();
        UiKit.caption(g,"Название",x,y);y+=10;
        UiKit.searchField(g,new Rect(x,y,cw,UiKit.FIELD_H),name,true,"Название повтора");y+=UiKit.FIELD_H+12;
        UiKit.caption(g,"Камера",x,y);y+=10;
        int cx=x;
        for(var view:DeathReplay.View.values()){
            if(view==DeathReplay.View.KILLER&&recording.killer()==null)continue;
            String label=viewLabel(view);var chip=new Rect(cx,y,UiKit.chipWidth(label),UiKit.CHIP_H);
            UiKit.chip(g,cx,y,label,controls.animate("sv"+view,saveView==view?1:0),controls.animate("svh"+view,chip.contains(mx,my)?1:0),false);
            actions.add(Map.entry(chip,()->saveView=view));cx+=chip.width()+4;
        }
        y+=UiKit.CHIP_H+12;
        UiKit.caption(g,"Скорость",x,y);y+=10;
        speedChips(g,x,y,mx,my,saveSpeed,v->saveSpeed=v,"ss");y+=UiKit.CHIP_H+12;
        UiKit.caption(g,"Длина",x,y);y+=10;
        cx=x;
        for(int seconds:new int[]{3,5,10,0}){
            String label=seconds==0?"Вся запись":Lang.pick(seconds+" s before death",seconds+" с до смерти");
            var chip=new Rect(cx,y,UiKit.chipWidth(label),UiKit.CHIP_H);
            UiKit.chip(g,cx,y,label,controls.animate("sl"+seconds,saveSeconds==seconds?1:0),controls.animate("slh"+seconds,chip.contains(mx,my)?1:0),false);
            actions.add(Map.entry(chip,()->saveSeconds=seconds));cx+=chip.width()+4;
        }
        y+=UiKit.CHIP_H+12;
        var hudRow=new Rect(x,y,cw,18);
        VelaDraw.text(g,"Интерфейс игры в повторе",x,y+4,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8f);
        VelaDraw.toggle(g,x+cw-24,y+2,controls.animate("svhud",saveHud?1:0),hudRow.contains(mx,my));
        actions.add(Map.entry(hudRow,()->saveHud=!saveHud));
        y+=22;
        var videoRow=new Rect(x,y,cw,18);
        VelaDraw.text(g,"Также записать видеофайл",x,y+4,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,8f);
        VelaDraw.toggle(g,x+cw-24,y+2,controls.animate("svvid",saveVideo?1:0),videoRow.contains(mx,my));
        actions.add(Map.entry(videoRow,()->saveVideo=!saveVideo));
        int by=r.y()+r.height()-30;
        String saveLabel="Сохранить",cancelLabel="Отмена";
        int sw=UiKit.chipWidth(saveLabel),cw2=UiKit.chipWidth(cancelLabel);
        var save=new Rect(r.x()+r.width()-14-sw,by,sw,UiKit.CHIP_H);var cancel=new Rect(save.x()-6-cw2,by,cw2,UiKit.CHIP_H);
        UiKit.chip(g,cancel.x(),by,cancelLabel,0,controls.animate("cancel",cancel.contains(mx,my)?1:0),false);
        UiKit.chip(g,save.x(),by,saveLabel,1,controls.animate("confirm",save.contains(mx,my)?1:0),false);
        actions.add(Map.entry(cancel,()->modal=Modal.NONE));
        actions.add(Map.entry(save,this::save));
    }
    private void save(){
        try{
            var file=ReplayFiles.save(recording,new ReplayFiles.Options(name.text().strip(),saveView,saveHud,saveSpeed,saveSeconds));
            modal=Modal.NONE;
            if(saveVideo)recordVideo(file);
            else dev.sqwat.vela.ui.Notifications.show(Lang.tr("Повтор сохранён"),name.text().strip(),"save");
        }catch(Exception error){
            org.slf4j.LoggerFactory.getLogger("Vela/Recap").warn("Cannot save replay",error);
            dev.sqwat.vela.ui.Notifications.show(Lang.tr("Не удалось сохранить повтор"),String.valueOf(error.getMessage()),"save");
        }
    }

    /** Plays the saved part once with the saved camera and writes it next to the replay file. Esc cancels. */
    private void recordVideo(java.nio.file.Path replay){
        String base=replay.getFileName().toString().replaceFirst("\\.vrec$","");
        float from=saveSeconds>0?Math.max(0,recording.deathTick()-saveSeconds*20-recording.firstTick()):0;
        DeathReplay.view(saveView);DeathReplay.showHud(saveHud);DeathReplay.seek(from);
        ReplayVideo.start(replay.resolveSibling(base),saveSpeed,video->{
            DeathReplay.showHud(false);
            dev.sqwat.vela.ui.Notifications.show(Lang.tr("Повтор и видео сохранены"),video.getFileName().toString(),"save");
        });
    }

    // ---- Library ----
    private void openLibrary(){library=ReplayFiles.list(recording.world(),recording.dimension());modal=Modal.LIBRARY;}
    private void libraryWindow(GuiGraphicsExtractor g,int mx,int my){
        int w=340,h=Math.min(height-40,280);var r=new Rect((width-w)/2,(height-h)/2,w,h);modalRect=r;
        var content=UiKit.window(g,r,"Сохранённые повторы","Только для этого мира и измерения");
        String folderLabel="Открыть папку";int fw=UiKit.chipWidth(folderLabel);
        var folder=new Rect(r.x()+r.width()-36-fw,r.y()+12,fw,UiKit.CHIP_H);
        UiKit.chip(g,folder.x(),folder.y(),folderLabel,0,controls.animate("folder",folder.contains(mx,my)?1:0),false);
        actions.add(Map.entry(folder,()->{
            var dir=ReplayFiles.dir();
            try{java.nio.file.Files.createDirectories(dir);}catch(java.io.IOException ignored){}
            net.minecraft.util.Util.getPlatform().openPath(dir);
        }));
        var close=new Rect(r.x()+r.width()-30,r.y()+10,20,20);
        UiKit.iconAction(g,close,"x",10,controls.animate("close",close.contains(mx,my)?1:0),VelaTheme.TEXT);
        actions.add(Map.entry(close,()->modal=Modal.NONE));
        if(library.isEmpty()){UiKit.empty(g,content,"save","Пока пусто","Нажмите «Сохранить», чтобы оставить повтор");return;}
        var fmt=DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm",Lang.english()?Locale.ENGLISH:Locale.forLanguageTag("ru"));
        int y=content.y();
        for(int i=0;i<library.size()&&y+34<=content.y()+content.height();i++){
            var saved=library.get(i);var row=new Rect(content.x(),y,content.width(),32);
            UiKit.card(g,row,controls.animate("lr"+i,row.contains(mx,my)?1:0),0);
            VelaDraw.text(g,VelaDraw.ellipsize(saved.title(),row.width()-40,VelaDraw.Weight.MEDIUM,8.5f),row.x()+10,row.y()+6,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8.5f);
            String detail=fmt.format(Instant.ofEpochMilli(saved.recordedAt()).atZone(ZoneId.systemDefault()))+" · "+Lang.tr(viewLabel(saved.options().view()));
            VelaDraw.text(g,detail,row.x()+10,row.y()+18,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7.5f);
            var delete=new Rect(row.x()+row.width()-24,row.y()+8,16,16);
            UiKit.iconAction(g,delete,"trash",10,controls.animate("ld"+i,delete.contains(mx,my)?1:0),VelaTheme.DANGER);
            actions.add(Map.entry(delete,()->{ReplayFiles.delete(saved);library=ReplayFiles.list(recording.world(),recording.dimension());}));
            actions.add(Map.entry(row,()->open(saved)));
            y+=36;
        }
    }
    private void open(ReplayFiles.Saved saved){
        try{
            var loaded=ReplayFiles.load(saved);
            minecraft.gui.setScreen(new DeathRecapScreen(loaded,parent,saved.options()));
        }catch(Exception error){
            org.slf4j.LoggerFactory.getLogger("Vela/Recap").warn("Cannot open replay",error);
            dev.sqwat.vela.ui.Notifications.show(Lang.tr("Не удалось открыть повтор"),String.valueOf(error.getMessage()),"save");
        }
    }

    @Override protected boolean clickVela(MouseButtonEvent e,boolean doubled){
        if(ReplayVideo.running())return true;
        for(var a:actions)if(a.getKey().contains(e.x(),e.y())){a.getValue().run();return true;}
        if(modal!=Modal.NONE){if(!modalRect.contains(e.x(),e.y()))modal=Modal.NONE;return true;}
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
        if(modal!=Modal.NONE)return true;
        if(events.contains(x,y))eventScroll-=(float)v*(EVENT_H+3)*2;else DeathReplay.zoom((float)v);
        return true;
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(ReplayVideo.running()){
            if(event.key()==GLFW.GLFW_KEY_ESCAPE){ReplayVideo.cancel();DeathReplay.showHud(false);dev.sqwat.vela.ui.Notifications.show(Lang.tr("Видео отменено"),Lang.tr("Повтор сохранён без видео"),"save");}
            return true;
        }
        if(modal!=Modal.NONE){
            if(event.key()==GLFW.GLFW_KEY_ESCAPE)modal=Modal.NONE;
            else if(modal==Modal.SAVE&&(event.key()==GLFW.GLFW_KEY_ENTER||event.key()==GLFW.GLFW_KEY_KP_ENTER))save();
            else if(modal==Modal.SAVE)GameTextInput.key(name,event,48);
            return true;
        }
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
    @Override public boolean charTyped(CharacterEvent event){
        if(modal==Modal.SAVE&&event.isAllowedChatCharacter()){name.insert(event.codepointAsString(),48);return true;}
        return false;
    }
    private static String plural(int n,String one,String few,String many){
        if(Lang.english())return Lang.tr(n==1?one:many);
        int m10=n%10,m100=n%100;
        return m10==1&&m100!=11?one:m10>=2&&m10<=4&&(m100<12||m100>14)?few:many;
    }
}
