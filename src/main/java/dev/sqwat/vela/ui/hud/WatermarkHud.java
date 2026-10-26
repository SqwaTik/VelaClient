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
    private int width=-1,fps,ping,speedTenths,minute,online;
    private long tps;
    private Object mask;
    private double lastX=Double.NaN,lastZ;
    private Object level;
    private float speed;

    /** Once per client tick while BPS is shown: horizontal blocks per second, smoothed; teleports are ignored. */
    void tick(Minecraft client){
        var p=client.player;
        if(p==null||client.level!=level){lastX=Double.NaN;speed=0;level=client.level;if(p==null)return;}
        if(!Double.isNaN(lastX)){
            double dx=p.getX()-lastX,dz=p.getZ()-lastZ,perTick=Math.sqrt(dx*dx+dz*dz);
            if(perTick<10)speed=speed*.6f+(float)(perTick*20)*.4f;
        }
        lastX=p.getX();lastZ=p.getZ();
    }

    String text(Minecraft client,boolean showFps,boolean showBps,boolean showTps,boolean showPing,boolean showTime,boolean showOnline){
        int fps=showFps?client.getFps():-1;
        int speedTenths=showBps?Math.round(speed*10):-1;
        long tps=Double.doubleToRawLongBits(showTps?HudTelemetry.tps():Double.NaN);
        int ping=-1;
        var connection=client.getConnection();
        if(showPing&&client.player!=null&&connection!=null){
            var info=connection.getPlayerInfo(client.player.getUUID());
            if(info!=null)ping=Math.max(-1,HudTelemetry.ping(info.getLatency()));
        }
        int minute=-1;
        if(showTime){var now=java.time.LocalTime.now();minute=now.getHour()*60+now.getMinute();}
        int online=showOnline&&connection!=null?connection.getOnlinePlayers().size():-1;
        Object mask=dev.sqwat.vela.module.utility.StreamerMode.mask();
        if(width>=0&&fps==this.fps&&speedTenths==this.speedTenths&&tps==this.tps&&ping==this.ping&&minute==this.minute&&online==this.online&&mask==this.mask)return text;
        this.fps=fps;this.speedTenths=speedTenths;this.tps=tps;this.ping=ping;this.minute=minute;this.online=online;this.mask=mask;
        String value="Vela";
        if(fps>=0)value+=" · "+fps+" FPS";
        if(speedTenths>=0)value+=" · "+speedTenths/10+"."+speedTenths%10+" BPS";
        double ticks=Double.longBitsToDouble(tps);if(!Double.isNaN(ticks))value+=" · "+String.format(java.util.Locale.ROOT,"%.1f",ticks)+" TPS";
        if(ping>=0)value+=" · "+ping+" ms";
        if(minute>=0)value+=" · "+String.format(java.util.Locale.ROOT,"%02d:%02d",minute/60,minute%60);
        if(online>=0)value+=" · "+online+" online";
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
