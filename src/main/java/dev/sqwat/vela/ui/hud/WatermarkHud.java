package dev.sqwat.vela.ui.hud;

import dev.sqwat.vela.ui.render.GameUiCanvas;
import dev.sqwat.vela.ui.render.MetricIcon;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * "Vela · FPS · TPS · ms" chip. Inputs are compared every frame; the string, split and text measurement
 * rebuild only when a displayed value changes (about once a second), not twice per frame.
 */
final class WatermarkHud {
    static final int HEIGHT=22;
    private String text="Vela";
    private String[] parts={"Vela"},labels={"Vela"};
    private int[] textX={23};
    private int width=-1,fps,ping;
    private long tps;
    private Object mask;

    String text(Minecraft client,boolean showFps,boolean showTps,boolean showPing){
        int fps=showFps?client.getFps():-1;
        long tps=Double.doubleToRawLongBits(showTps?HudTelemetry.tps():Double.NaN);
        int ping=-1;
        if(showPing&&client.player!=null&&client.getConnection()!=null){
            var info=client.getConnection().getPlayerInfo(client.player.getUUID());
            if(info!=null)ping=Math.max(-1,HudTelemetry.ping(info.getLatency()));
        }
        Object mask=dev.sqwat.vela.module.utility.StreamerMode.mask();
        if(width>=0&&fps==this.fps&&tps==this.tps&&ping==this.ping&&mask==this.mask)return text;
        this.fps=fps;this.tps=tps;this.ping=ping;this.mask=mask;
        String value="Vela";
        if(fps>=0)value+=" · "+fps+" FPS";
        double ticks=Double.longBitsToDouble(tps);if(!Double.isNaN(ticks))value+=" · "+String.format(java.util.Locale.ROOT,"%.1f",ticks)+" TPS";
        if(ping>=0)value+=" · "+ping+" ms";
        text=value;parts=value.split(" · ");
        labels=new String[parts.length];textX=new int[parts.length];
        int x=23;width=30+Math.max(0,parts.length-1)*21;
        for(int i=0;i<parts.length;i++){
            if(i>0)x+=21;
            labels[i]=i==0?parts[i]:MetricIcon.value(parts[i]);
            int w=VelaDraw.textWidth(labels[i],VelaDraw.Weight.MEDIUM);
            textX[i]=x;x+=w;width+=w;
        }
        return text;
    }
    /** Width of the text last returned by {@link #text}. */
    int width(){return width;}
    /** Logo, metric icons and labels; the panel itself is drawn by the layout service. */
    void drawContent(GuiGraphicsExtractor g,Minecraft client){
        VelaDraw.logo(g,5,4,14);
        GameUiCanvas canvas=parts.length>1?new GameUiCanvas(g):null;
        for(int i=0;i<parts.length;i++){
            if(i>0)MetricIcon.draw(canvas,parts[i],textX[i]-13,6,VelaTheme.TEXT_SECONDARY);
            VelaDraw.text(g,client.font,labels[i],textX[i],7,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM);
        }
    }
}
