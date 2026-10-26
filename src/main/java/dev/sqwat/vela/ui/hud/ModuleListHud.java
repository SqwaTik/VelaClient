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
 * Active modules, longest name first, with eased enter/exit. The sorted list and text widths are rebuilt only
 * when the set of enabled modules changes; per frame the panel compares a cheap signature and animates rows.
 */
final class ModuleListHud {
    static final int ROW=13,PAD=4;
    private static final class Row {final ClientModule module;final int width;float reveal;boolean wanted=true;Row(ClientModule module,int width){this.module=module;this.width=width;}}
    private final List<Row> rows=new ArrayList<>();
    private final Map<String,Row> byId=new HashMap<>();
    private long signature=Long.MIN_VALUE,last;
    private int maxWidth=40;

    /** Rows currently visible or animating out; call once per frame before {@link #width}/{@link #height}. */
    void update(ModuleRegistry modules,boolean immediate){
        long sig=1;int count=0;
        for(var m:modules.all())if(shown(m)){sig=sig*31+m.id().hashCode();count++;}
        sig=sig*31+count;
        if(sig!=signature){signature=sig;rebuild(modules);}
        long now=System.nanoTime();float dt=last==0?0:Math.min(.1f,(now-last)/1e9f);last=now;
        for(int i=rows.size()-1;i>=0;i--){
            var row=rows.get(i);float goal=row.wanted?1:0;
            row.reveal=immediate?goal:dev.sqwat.vela.ui.animation.Motion.approach(row.reveal,goal,14,dt);
            if(!row.wanted&&row.reveal<.01f){rows.remove(i);byId.remove(row.module.id());}
        }
    }
    private static boolean shown(ClientModule m){return m.enabled()&&m.available()&&m.category()!=ModuleCategory.HUD;}
    private void rebuild(ModuleRegistry modules){
        for(var row:rows)row.wanted=false;
        for(var m:modules.all()){
            if(!shown(m))continue;
            var row=byId.get(m.id());
            if(row==null){row=new Row(m,(int)Math.ceil(VelaDraw.textWidth(m.name(),VelaDraw.Weight.MEDIUM,8)));rows.add(row);byId.put(m.id(),row);}
            row.wanted=true;
        }
        rows.sort((a,b)->a.width!=b.width?Integer.compare(b.width,a.width):a.module.name().compareTo(b.module.name()));
        maxWidth=40;for(var row:rows)maxWidth=Math.max(maxWidth,row.width+PAD*2+6);
    }
    boolean empty(){return rows.isEmpty();}
    int width(){return maxWidth;}
    int height(){float h=PAD*2;for(var row:rows)h+=ROW*row.reveal;return Math.max(ROW+PAD*2,Math.round(h));}

    /** Right-aligned rows with an accent bar; rows slide in from the right edge as they reveal. */
    void draw(GuiGraphicsExtractor g){
        float y=PAD;
        for(var row:rows){
            if(row.reveal<.01f)continue;
            int alpha=Math.round(255*row.reveal);
            float x=maxWidth-PAD-6-row.width+(1-row.reveal)*12;
            VelaDraw.text(g,row.module.name(),x,y+2.5f,(alpha<<24)|(VelaTheme.TEXT&0xFFFFFF),VelaDraw.Weight.MEDIUM,8);
            VelaDraw.roundedRect(g,maxWidth-PAD-2,Math.round(y+1),2,ROW-3,1,(alpha<<24)|(VelaTheme.PRIMARY&0xFFFFFF));
            y+=ROW*row.reveal;
        }
    }
}
