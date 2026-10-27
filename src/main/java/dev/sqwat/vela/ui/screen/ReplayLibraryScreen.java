package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.marker.MarkerRuntime;
import dev.sqwat.vela.module.visual.DeathRecap;
import dev.sqwat.vela.module.visual.DeathReplay;
import dev.sqwat.vela.module.visual.ReplayFiles;
import dev.sqwat.vela.ui.Lang;
import dev.sqwat.vela.ui.Notifications;
import dev.sqwat.vela.ui.animation.FrameClock;
import dev.sqwat.vela.ui.animation.Motion;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Every Death Recap recording in one place: the last death (even unsaved) and all saved replays. Replays of the world
 * and dimension you are in play right away; the others say where they are from. The folder is one click away. Opens from the module settings, its key and the death screen.
 */
public final class ReplayLibraryScreen extends VelaScreen {
    private static final int ROW=38;
    private final Screen parent;
    private final UiControls controls=new UiControls();
    private final FrameClock clock=new FrameClock();
    private final List<Map.Entry<Rect,Runnable>> actions=new ArrayList<>();
    private List<ReplayFiles.Saved> saved=List.of();
    private boolean everywhere;
    private float scroll,targetScroll,maxScroll,entrance;
    private Rect list=new Rect(0,0,0,0);

    public ReplayLibraryScreen(Screen parent){super(Component.literal("Death Recap"));this.parent=parent;}
    public static void open(){var mc=net.minecraft.client.Minecraft.getInstance();mc.gui.setScreen(new ReplayLibraryScreen(mc.gui.screen()));}

    @Override protected void init(){reload();}
    private void reload(){saved=ReplayFiles.list(null,null);}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){}

    private String world(){return minecraft.level==null?null:MarkerRuntime.world(minecraft);}
    private String dimension(){return minecraft.level==null?null:MarkerRuntime.dimension(minecraft);}
    private boolean here(ReplayFiles.Saved s){return minecraft.level!=null&&s.world().equals(world())&&s.dimension().equals(dimension());}

    @Override protected void renderVela(GuiGraphicsExtractor g,int mx,int my,float delta){
        float dt=clock.step();actions.clear();
        entrance=Motion.approach(entrance,1,14,dt);float ease=Motion.easeOutCubic(entrance);
        if(parent instanceof VelaScreen backdrop)backdrop.renderAsBackdrop(g,width,height,delta);
        g.fill(0,0,width,height,(Math.round(0xC0*ease)<<24)|0x05070A);
        int w=Math.min(width-40,420),h=Math.min(height-40,330);
        var r=new Rect((width-w)/2,(height-h)/2+Math.round((1-ease)*10),w,h);
        var content=UiKit.window(g,r,"Записи смертей","Повторы лежат в папке vela-replays");
        var close=new Rect(r.x()+r.width()-30,r.y()+10,20,20);
        UiKit.iconAction(g,close,"x",10,controls.animate("close",close.contains(mx,my)?1:0),VelaTheme.TEXT);
        actions.add(Map.entry(close,this::onClose));

        // Filter and folder, one row of chips.
        int x=content.x(),y=content.y();
        for(boolean all:new boolean[]{false,true}){
            String label=all?"Все миры":"Этот мир";var chip=new Rect(x,y,UiKit.chipWidth(label),UiKit.CHIP_H);
            UiKit.chip(g,x,y,label,controls.animate("f"+all,everywhere==all?1:0),controls.animate("fh"+all,chip.contains(mx,my)?1:0),false);
            actions.add(Map.entry(chip,()->{everywhere=all;targetScroll=0;}));x+=chip.width()+4;
        }
        String folderLabel="Открыть папку";int fw=UiKit.chipWidth(folderLabel);
        var folder=new Rect(content.x()+content.width()-fw,y,fw,UiKit.CHIP_H);
        UiKit.chip(g,folder.x(),y,folderLabel,0,controls.animate("folder",folder.contains(mx,my)?1:0),false);
        actions.add(Map.entry(folder,()->{var dir=ReplayFiles.dir();try{Files.createDirectories(dir);}catch(java.io.IOException ignored){}Util.getPlatform().openPath(dir);}));

        list=new Rect(content.x(),y+UiKit.CHIP_H+10,content.width(),content.y()+content.height()-(y+UiKit.CHIP_H+10));
        var rows=new ArrayList<Runnable>();
        var recap=DeathRecap.current();
        boolean last=recap!=null&&recap.hasRecording()&&minecraft.level!=null;
        var shown=saved.stream().filter(s->everywhere||here(s)).toList();
        int count=(last?1:0)+shown.size();
        if(count==0){
            UiKit.empty(g,list,"save",everywhere?"Записей пока нет":"В этом мире записей нет",
                minecraft.level==null?"Зайдите в мир, чтобы смотреть повторы":"После смерти нажмите «Повтор смерти», затем «Сохранить»");
            return;
        }
        maxScroll=Math.max(0,count*(ROW+4)-4-list.height());
        targetScroll=Math.clamp(targetScroll,0,maxScroll);scroll=Motion.approach(scroll,targetScroll,18,dt);
        var fmt=DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm",Lang.english()?Locale.ENGLISH:Locale.forLanguageTag("ru"));
        UiClip.enable(g,list.x(),list.y(),list.x()+list.width(),list.y()+list.height());
        try{
            int i=0;
            if(last){
                var row=rowRect(i++);
                if(visible(row)){
                    boolean over=row.contains(mx,my)&&list.contains(mx,my);
                    UiKit.card(g,row,controls.animate("last",over?1:0),.6f);
                    VelaDraw.text(g,"Последняя смерть",row.x()+10,row.y()+7,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8.5f);
                    VelaDraw.text(g,"Ещё не сохранена · откроется сразу",row.x()+10,row.y()+21,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7.5f);
                    if(over)actions.add(Map.entry(row,()->{var rec=recap.recording();if(rec!=null)minecraft.gui.setScreen(new DeathRecapScreen(rec,this));}));
                }
            }
            for(var s:shown){
                var row=rowRect(i);int index=i++;
                if(!visible(row))continue;
                boolean over=row.contains(mx,my)&&list.contains(mx,my),playable=here(s);
                UiKit.card(g,row,controls.animate("r"+index,over?1:0),0);
                int right=row.x()+row.width()-8;
                var delete=new Rect(right-18,row.y()+(ROW-18)/2,18,18);right=delete.x()-4;
                UiKit.iconAction(g,delete,"trash",10,controls.animate("d"+index,delete.contains(mx,my)?1:0),VelaTheme.DANGER);
                if(over)actions.add(Map.entry(delete,()->{ReplayFiles.delete(s);reload();}));
                VelaDraw.text(g,VelaDraw.ellipsize(s.title(),right-row.x()-14,VelaDraw.Weight.MEDIUM,8.5f),row.x()+10,row.y()+7,playable?VelaTheme.TEXT:VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.MEDIUM,8.5f);
                String where=playable?Lang.tr(viewLabel(s.options().view())):Lang.tr("другой мир — смотреть там")+" · "+s.world();
                String detail=fmt.format(Instant.ofEpochMilli(s.recordedAt()).atZone(ZoneId.systemDefault()))+" · "+where;
                VelaDraw.text(g,VelaDraw.ellipsize(detail,right-row.x()-14,VelaDraw.Weight.REGULAR,7.5f),row.x()+10,row.y()+21,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,7.5f);
                if(over)actions.add(Map.entry(row,()->open(s)));
            }
        }finally{g.disableScissor();}
    }
    private Rect rowRect(int i){return new Rect(list.x(),list.y()+i*(ROW+4)-Math.round(scroll),list.width(),ROW);}
    private boolean visible(Rect row){return row.y()+row.height()>=list.y()&&row.y()<=list.y()+list.height();}
    private static String viewLabel(DeathReplay.View view){return switch(view){case ORBIT->"Свободная камера";case SELF->"Моими глазами";case KILLER->"Глазами убийцы";};}

    private void open(ReplayFiles.Saved s){
        if(!here(s)){
            Notifications.show(Lang.tr("Повтор из другого мира"),minecraft.level==null?Lang.tr("Зайдите в мир, чтобы смотреть повторы"):Lang.tr("Зайдите в тот мир и измерение, чтобы посмотреть"),"save");
            return;
        }
        try{minecraft.gui.setScreen(new DeathRecapScreen(ReplayFiles.load(s),this,s.options()));}
        catch(Exception error){
            org.slf4j.LoggerFactory.getLogger("Vela/Recap").warn("Cannot open replay",error);
            Notifications.show(Lang.tr("Не удалось открыть повтор"),String.valueOf(error.getMessage()),"save");
        }
    }

    @Override protected boolean clickVela(MouseButtonEvent e,boolean doubled){
        for(var a:actions)if(a.getKey().contains(e.x(),e.y())){a.getValue().run();return true;}
        return true;
    }
    @Override protected boolean scrollVela(double x,double y,double h,double v){targetScroll-=(float)v*(ROW+4);return true;}
    @Override public boolean keyPressed(KeyEvent event){
        if(event.key()==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}
        return super.keyPressed(event);
    }
}
