package dev.sqwat.vela.ui.hud;

import dev.sqwat.vela.ui.render.QuadBatch;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Compact readout: horizontal speed (blocks/s, measured per game tick), TPS, ping and block coordinates.
 * Values are sampled per tick and their strings rebuilt only when the displayed value changes.
 */
final class InfoHud {
    static final int ROW=12,PAD=6,TEXT=8,LABEL_WIDTH=34;
    record Row(String label,String value){}
    private double lastX=Double.NaN,lastZ;
    private Object level;
    private float speed;
    private List<Row> rows=List.of();
    private String key="";
    private int width=80;
    private final QuadBatch batch=new QuadBatch();

    /** Called once per client tick. Teleports and world changes do not produce speed spikes. */
    void tick(Minecraft client){
        var p=client.player;
        if(p==null||client.level!=level){lastX=Double.NaN;speed=0;level=client.level;if(p==null)return;}
        if(!Double.isNaN(lastX)){
            double dx=p.getX()-lastX,dz=p.getZ()-lastZ,perTick=Math.sqrt(dx*dx+dz*dz);
            if(perTick<10)speed=speed*.6f+(float)(perTick*20)*.4f;
        }
        lastX=p.getX();lastZ=p.getZ();
    }
    /** Rebuilds strings only when a shown value changes. */
    void update(Minecraft client,boolean bps,boolean tps,boolean ping,boolean coords){
        var p=client.player;
        String speedText=bps?String.format(Locale.ROOT,"%.1f",speed):"";
        double ticks=tps?HudTelemetry.tps():Double.NaN;
        String tpsText=tps?(Double.isNaN(ticks)?"—":String.format(Locale.ROOT,"%.1f",ticks)):"";
        String pingText="";
        if(ping){
            var info=p==null||client.getConnection()==null?null:client.getConnection().getPlayerInfo(p.getUUID());
            pingText=info==null?"—":HudTelemetry.ping(info.getLatency())+" ms";
        }
        String coordText=coords&&p!=null?p.getBlockX()+" "+p.getBlockY()+" "+p.getBlockZ():"";
        String next=speedText+'|'+tpsText+'|'+pingText+'|'+coordText+'|'+bps+tps+ping+coords;
        if(next.equals(key))return;
        key=next;
        var list=new ArrayList<Row>(4);
        if(bps)list.add(new Row("BPS",speedText));
        if(tps)list.add(new Row("TPS",tpsText));
        if(ping)list.add(new Row("Пинг",pingText));
        if(coords)list.add(new Row("XYZ",coordText));
        rows=List.copyOf(list);
        int widest=0;for(var r:rows)widest=Math.max(widest,(int)Math.ceil(VelaDraw.textWidth(r.value,VelaDraw.Weight.MEDIUM,TEXT)));
        // Widen in 8 px steps so a changing number does not make the panel twitch every frame.
        width=Math.max(80,PAD*2+LABEL_WIDTH+((widest+7)/8)*8);
    }
    boolean empty(){return rows.isEmpty();}
    int width(){return width;}
    int height(){return PAD*2+Math.max(1,rows.size())*ROW-2;}
    void draw(GuiGraphicsExtractor g){
        batch.begin(g);
        float y=PAD;
        for(var row:rows){
            batch.text(row.label,PAD,y,VelaTheme.TEXT_MUTED,VelaDraw.Weight.MEDIUM,TEXT);
            batch.text(row.value,PAD+LABEL_WIDTH,y,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,TEXT);
            y+=ROW;
        }
        batch.flush();
    }
}
