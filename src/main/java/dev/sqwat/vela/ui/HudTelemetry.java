package dev.sqwat.vela.ui;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Scalar HUD telemetry only; no audio samples are retained. */
public final class HudTelemetry {
    public record Speaker(UUID id,float level,long updated){}
    private static final Map<UUID,Speaker> speakers=new ConcurrentHashMap<>();
    private static long lastTime,lastTick;
    private static double tps=Double.NaN;
    private static net.minecraft.client.multiplayer.ClientPacketListener pingConnection;
    private static long pingSent,pingNext,pingUpdated,pingNanos;
    private static int measuredPing=-1;
    public static synchronized void tickPing(net.minecraft.client.Minecraft client,boolean enabled){
        var connection=enabled&&client.level!=null?client.getConnection():null;
        if(connection!=pingConnection){pingConnection=connection;pingSent=pingNext=pingUpdated=0;measuredPing=-1;}
        if(connection==null)return;
        long now=System.nanoTime();
        if(now<pingNext||pingSent!=0&&now-pingNanos<5_000_000_000L)return;
        pingSent=net.minecraft.util.Util.getMillis();pingNanos=now;pingNext=now+1_000_000_000L;
        connection.send(new net.minecraft.network.protocol.ping.ServerboundPingRequestPacket(pingSent));
    }
    public static synchronized void pong(net.minecraft.client.multiplayer.ClientPacketListener connection,long time){
        if(connection!=pingConnection||pingSent==0||time!=pingSent)return;
        measuredPing=(int)Math.clamp((System.nanoTime()-pingNanos)/1_000_000L,0,60_000);
        pingUpdated=System.nanoTime();pingSent=0;
    }
    public static synchronized int ping(int fallback){return measuredPing>=0&&System.nanoTime()-pingUpdated<5_000_000_000L?measuredPing:fallback;}
    public static void voice(UUID id,short[] pcm){
        if(pcm.length==0){speakers.remove(id);return;}
        double sum=0;for(short sample:pcm){double v=sample/32768.0;sum+=v*v;}
        float rms=(float)Math.sqrt(sum/pcm.length);
        if(rms<.0005f){speakers.remove(id);return;}
        speakers.put(id,new Speaker(id,Math.min(1,rms*5),System.nanoTime()));
    }
    public static List<Speaker> speakers(){
        long now=System.nanoTime();speakers.values().removeIf(s->now-s.updated()>350_000_000L);
        return speakers.values().stream().sorted(Comparator.comparing(s->s.id().toString())).toList();
    }
    public static synchronized void serverTime(long tick,long now){
        if(lastTime!=0&&now>lastTime&&tick>lastTick){double sample=Math.min(20,(tick-lastTick)*1e9/(now-lastTime));tps=Double.isNaN(tps)?sample:tps*.7+sample*.3;}
        lastTime=now;lastTick=tick;
    }
    public static synchronized double tps(){return lastTime==0||System.nanoTime()-lastTime>10_000_000_000L?Double.NaN:tps;}
    public static synchronized void clear(){speakers.clear();lastTime=lastTick=0;tps=Double.NaN;}
}
