package dev.sqwat.vela.ui.hud;

import dev.sqwat.vela.module.ModuleRegistry;
import dev.sqwat.vela.module.visual.ItemPickups;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.List;

/** Item Pickups panel: rows slide in from the left, the counter pops when a repeat merges in, each row fades on its own. */
final class PickupsHud {
    static final int WIDTH=156,ROW=20;
    private final ModuleRegistry modules;
    private List<ItemPickups.Entry> samples;
    PickupsHud(ModuleRegistry modules){this.modules=modules;}

    boolean hasRows(){return modules.byId("pickups") instanceof ItemPickups feed&&!feed.empty();}
    int height(Minecraft client,boolean editing){return 6+Math.max(1,rows(client,editing).size())*ROW;}
    /** Live rows, or a fixed sample while the HUD editor or chat is open and nothing was picked up. */
    private List<ItemPickups.Entry> rows(Minecraft client,boolean editing){
        var rows=modules.byId("pickups") instanceof ItemPickups feed?feed.entries():List.<ItemPickups.Entry>of();
        if(!rows.isEmpty()||!(editing||client.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen))return rows;
        if(samples==null)samples=ItemPickups.samples();
        return samples;
    }
    void draw(GuiGraphicsExtractor g,Minecraft client,boolean editing,java.util.function.IntConsumer panel){
        var rows=rows(client,editing);
        var feed=modules.byId("pickups") instanceof ItemPickups f?f:null;
        boolean sample=feed==null||rows!=feed.entries();
        panel.accept(6+Math.max(1,rows.size())*ROW);
        long now=System.currentTimeMillis(),life=feed==null?4000:feed.lifetime();
        boolean showTotal=feed==null||feed.totals();
        for(int i=0;i<rows.size();i++){
            var row=rows.get(i);
            long age=sample?10_000:now-row.born;
            float slide=sample?1:Math.clamp(age/(float)ItemPickups.SLIDE_MS,0,1);slide=1-(1-slide)*(1-slide)*(1-slide);
            float alpha=sample?1:1-Math.clamp((age-life)/(float)ItemPickups.FADE_MS,0,1);
            if(alpha<=0)continue;
            int y=3+i*ROW;float x=(1-slide)*-14;
            int a=Math.round(alpha*slide*255)<<24;
            g.pose().pushMatrix();g.pose().translate(x,0);
            if(alpha>.35f&&slide>.35f)g.item(row.icon,5,y+2);
            String amount=(row.amount>0?"+":"–")+Math.abs(row.amount);
            float bump=sample?0:Math.max(0,1-(now-row.bumped)/260f);
            int amountColor=(row.amount>0?VelaTheme.POSITIVE:VelaTheme.DANGER)&0xFFFFFF|a;
            float amountWidth=VelaDraw.textWidth(amount,VelaDraw.Weight.SEMIBOLD,9);
            g.pose().pushMatrix();g.pose().translate(26+amountWidth/2,y+10);g.pose().scale(1+.28f*bump*bump);g.pose().translate(-(26+amountWidth/2),-(y+10));
            VelaDraw.text(g,amount,26,y+6,amountColor,VelaDraw.Weight.SEMIBOLD,9);
            g.pose().popMatrix();
            String total=showTotal?"×"+row.total:"";
            float totalWidth=total.isEmpty()?0:VelaDraw.textWidth(total,VelaDraw.Weight.REGULAR,7.5f);
            float nameX=26+amountWidth+5;
            String name=VelaDraw.ellipsize(row.icon.getHoverName().getString(),WIDTH-nameX-totalWidth-12,VelaDraw.Weight.REGULAR,8);
            VelaDraw.text(g,name,nameX,y+6.5f,VelaTheme.TEXT&0xFFFFFF|a,VelaDraw.Weight.REGULAR,8);
            if(!total.isEmpty())VelaDraw.text(g,total,WIDTH-7-totalWidth,y+7,VelaTheme.TEXT_MUTED&0xFFFFFF|a,VelaDraw.Weight.REGULAR,7.5f);
            g.pose().popMatrix();
        }
    }
}
