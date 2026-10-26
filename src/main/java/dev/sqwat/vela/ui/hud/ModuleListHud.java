package dev.sqwat.vela.ui.hud;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.ModuleRegistry;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Active modules as a staircase of pills, longest name first. The box width is the longest name of any module, so
 * toggling never moves the list; rows align to the screen side the box is on (left half: left, right half: right)
 * and slide in from that edge. The sorted list and widths rebuild only when the enabled set changes.
 */
final class ModuleListHud {
    static final int ROW=14,GAP=1,PAD_X=6,BAR=2,TEXT=8;
    private static final class Row {final ClientModule module;final int width;float reveal;boolean wanted=true;Row(ClientModule module,int width){this.module=module;this.width=width;}}
    private final List<Row> rows=new ArrayList<>();
    private final Map<String,Row> byId=new HashMap<>();
    private long signature=Long.MIN_VALUE,last;
    private int boxWidth=-1,registered=-1;
    private final ModuleRegistry modules;
    ModuleListHud(ModuleRegistry modules){this.modules=modules;}

    void update(ModuleRegistry modules,boolean immediate){
        long sig=1;int count=0;
        for(var m:modules.all())if(shown(m)){sig=sig*31+m.id().hashCode();count++;}
        sig=sig*31+count;
        if(sig!=signature){signature=sig;rebuild(modules);}
        if(registered!=modules.all().size())measureBox(modules);
        long now=System.nanoTime();float dt=last==0?0:Math.min(.1f,(now-last)/1e9f);last=now;
        for(int i=rows.size()-1;i>=0;i--){
            var row=rows.get(i);float goal=row.wanted?1:0;
            row.reveal=immediate?goal:dev.sqwat.vela.ui.animation.Motion.approach(row.reveal,goal,14,dt);
            if(!row.wanted&&row.reveal<.01f){rows.remove(i);byId.remove(row.module.id());}
        }
    }
    private static boolean shown(ClientModule m){return m.enabled()&&m.available()&&m.category()!=ModuleCategory.HUD;}
    private static int textWidth(ClientModule m){return (int)Math.ceil(VelaDraw.textWidth(m.name(),VelaDraw.Weight.MEDIUM,TEXT));}
    private void measureBox(ModuleRegistry modules){
        registered=modules.all().size();int widest=40;
        for(var m:modules.all())if(m.category()!=ModuleCategory.HUD)widest=Math.max(widest,textWidth(m));
        boxWidth=widest+PAD_X*2+BAR;
    }
    private void rebuild(ModuleRegistry modules){
        for(var row:rows)row.wanted=false;
        for(var m:modules.all()){
            if(!shown(m))continue;
            var row=byId.get(m.id());
            if(row==null){row=new Row(m,textWidth(m));rows.add(row);byId.put(m.id(),row);}
            row.wanted=true;
        }
        rows.sort((a,b)->a.width!=b.width?Integer.compare(b.width,a.width):a.module.name().compareTo(b.module.name()));
    }
    boolean empty(){return rows.isEmpty();}
    int width(){if(registered!=modules.all().size())measureBox(modules);return Math.max(60,boxWidth);}
    int height(){float h=0;for(var row:rows)h+=(ROW+GAP)*row.reveal;return Math.max(ROW,Math.round(h));}

    /**
     * @param rightSide the box centre is on the right half of the screen
     * @param background panel colour (already includes the transparency animation)
     */
    private final dev.sqwat.vela.ui.render.QuadBatch batch=new dev.sqwat.vela.ui.render.QuadBatch();

    /**
     * Whole list as two GUI elements (pills+bars, then text) instead of three per row: with dozens of modules the
     * per-element cost of vanilla's GUI sort and overlap tests dominated the HUD.
     */
    void draw(GuiGraphicsExtractor g,boolean rightSide,int background,boolean editing){
        int box=width();
        batch.begin(g);
        if(rows.isEmpty()){
            // HUD editor: show where the list goes even when nothing is enabled.
            if(editing)pill("Array List",textWidthOf("Array List"),0,rightSide,box,1,background,0);
        }else{
            float y=0;int index=0;
            for(var row:rows){
                if(row.reveal<.01f)continue;
                pill(row.module.name(),row.width,y,rightSide,box,row.reveal,background,index);
                y+=(ROW+GAP)*row.reveal;index++;
            }
        }
        batch.flush();
    }
    private static int textWidthOf(String text){return (int)Math.ceil(VelaDraw.textWidth(text,VelaDraw.Weight.MEDIUM,TEXT));}
    private void pill(String name,int textWidth,float y,boolean rightSide,int box,float reveal,int background,int index){
        int alpha=Math.round(255*reveal);
        int pill=textWidth+PAD_X*2+BAR;
        float slide=(1-reveal)*(pill*.6f);
        float x=rightSide?box-pill+slide:-slide;
        int bg=(Math.round(((background>>>24)&255)*reveal)<<24)|(background&0xFFFFFF);
        batch.roundedRect(Math.round(x),Math.round(y),pill,ROW,4,bg);
        // Subtle accent fade down the list, from the client colour towards white.
        int accent=VelaTheme.mix(VelaTheme.PRIMARY,0xFFFFFFFF,Math.min(.35f,index*.035f));
        int barX=Math.round(rightSide?x+pill-BAR-1:x+1);
        batch.roundedRect(barX,Math.round(y+3),BAR,ROW-6,1,(alpha<<24)|(accent&0xFFFFFF));
        float textX=rightSide?x+PAD_X:x+PAD_X+BAR;
        batch.text(name,textX,y+(ROW-TEXT)/2f-.5f,(alpha<<24)|(VelaTheme.TEXT&0xFFFFFF),VelaDraw.Weight.MEDIUM,TEXT);
    }
}
