package dev.sqwat.vela.render.perf;

import dev.sqwat.vela.render.entity.NameTagTextCache;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Explicit local measurement; no account or server data is recorded. A developer machine can opt in to automatic
 * profiling with an empty {@code config/velaclient/profiler-auto} file (or VELA_PROFILE=1): every world is then recorded
 * with a rolling three-minute profile that is saved on a stall and when the world is left.
 */
public final class PerformanceCapture {
    /** A frame this long is a stall worth a snapshot of the minutes before it. */
    static final double STALL_MS=120;
    private static final long SNAPSHOT_GAP=90_000_000_000L;
    private static FrameSamples all,combat,spareAll,spareCombat;
    private static long last,attackUntil,lastSnapshot;
    private static boolean automatic,autoRun;
    private static Object level;
    private static String lastReport;
    private PerformanceCapture(){}
    public static void initialize(){
        automatic=automaticEnabled(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("velaclient"),System.getenv("VELA_PROFILE"));
        if(automatic)org.slf4j.LoggerFactory.getLogger("Vela/Performance").info("Automatic profiling is on (config/velaclient/profiler-auto)");
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client->{if(all!=null)finish("Выход из игры");});
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,registry)->dispatcher.register(literal("vela")
            .then(literal("perf")
                .then(literal("start").executes(ctx->{start(false);ctx.getSource().sendFeedback(Component.literal(dev.sqwat.vela.ui.Lang.tr("Замер кадров начат. Завершить: /vela perf stop")));return 1;})
                    .then(literal("profile").executes(ctx->{
                        try{start(true);}catch(RuntimeException error){org.slf4j.LoggerFactory.getLogger("Vela/Performance").warn("Cannot start profile",error);ctx.getSource().sendError(Component.literal(dev.sqwat.vela.ui.Lang.tr("Не удалось запустить профиль нагрузки. См. журнал.")));return 0;}
                        ctx.getSource().sendFeedback(Component.literal(dev.sqwat.vela.ui.Lang.tr("Замер и профиль нагрузки начаты (до 5 минут). Завершить: /vela perf stop")));return 1;})))
                .then(literal("stop").executes(ctx->{
                    if(all==null){ctx.getSource().sendFeedback(Component.literal(lastReport==null?"Замер не запущен.":dev.sqwat.vela.ui.Lang.tr("Последний сохранённый замер: ")+lastReport));return lastReport==null?0:1;}
                    String report=finish("Остановлен командой");
                    ctx.getSource().sendFeedback(Component.literal(report));return 1;
                })))));
    }
    static boolean automaticEnabled(java.nio.file.Path configDir,String environment){
        return "1".equals(environment)||java.nio.file.Files.exists(configDir.resolve("profiler-auto"));
    }
    private static java.nio.file.Path output(){return net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("debug/vela-perf");}
    private static void start(boolean profile){start(profile,false);}
    private static void start(boolean profile,boolean auto){
        if(all!=null)finish("Начат новый замер");
        PerformanceRecording.cancel();
        all=combat=null;last=attackUntil=0;level=null;autoRun=auto;
        if(profile)PerformanceRecording.start(auto);
        NameTagTextCache.resetStatistics();
        if(spareAll==null){spareAll=new FrameSamples(65536);spareCombat=new FrameSamples(65536);}
        all=spareAll;combat=spareCombat;all.reset();combat.reset();last=attackUntil=0;level=Minecraft.getInstance().level;
        logContext("start");
    }
    private static String finish(String reason){
        lastReport=reason+": "+format("Все кадры",all.summary())+"; "+format("После атак",combat.summary());
        all=combat=null;last=attackUntil=0;level=null;
        autoRun=false;
        PerformanceRecording.finish(output());
        org.slf4j.LoggerFactory.getLogger("Vela/Performance").info(lastReport);
        org.slf4j.LoggerFactory.getLogger("Vela/Performance").info(NameTagTextCache.statistics());
        logContext("stop");
        return lastReport;
    }
    private static void rollOver(){
        org.slf4j.LoggerFactory.getLogger("Vela/Performance").info(dev.sqwat.vela.ui.Lang.tr("Смена мира")+": "+format("Все кадры",all.summary())+"; "+format("После атак",combat.summary()));
        all.reset();combat.reset();last=attackUntil=0;level=Minecraft.getInstance().level;
    }
    private static void logContext(String phase){
        var client=Minecraft.getInstance();var runtime=Runtime.getRuntime();
        org.slf4j.LoggerFactory.getLogger("Vela/Performance").info(
            "Context {}: resolution={}x{}, processors={}, heapUsedMiB={}, heapCommittedMiB={}, heapMaxMiB={}",
            phase,client.getWindow().getWidth(),client.getWindow().getHeight(),runtime.availableProcessors(),
            (runtime.totalMemory()-runtime.freeMemory())/(1024*1024),runtime.totalMemory()/(1024*1024),runtime.maxMemory()/(1024*1024));
    }
    private static String format(String name,FrameSamples.Summary s){return String.format(java.util.Locale.ROOT,dev.sqwat.vela.ui.Lang.tr("%s: n=%d, среднее %.2f мс, p95 %.2f мс, p99 %.2f мс, max %.2f мс, >16.67 мс: %d, 1%% low %.0f FPS"),name,s.frames(),s.averageMs(),s.p95Ms(),s.p99Ms(),s.maxMs(),s.over16Ms(),s.p99Ms()>0?1000/s.p99Ms():0);}
    public static void attacked(){if(all!=null)attackUntil=System.nanoTime()+1_000_000_000L;}
    public static void frame(){
        if(all==null){
            if(automatic&&Minecraft.getInstance().level!=null){
                try{start(true,true);}catch(RuntimeException error){automatic=false;org.slf4j.LoggerFactory.getLogger("Vela/Performance").warn("Automatic profiling disabled",error);}
            }
            return;
        }
        var c=Minecraft.getInstance();long now=System.nanoTime();
        if(c.level!=level){
            // Servers move players hub -> limbo -> game several times a minute. The automatic recording keeps rolling
            // across those switches: restarting and dumping JFR on each one was itself a 300 ms stall.
            if(autoRun&&c.level!=null){rollOver();return;}
            finish("Смена мира — замер сохранён");return;
        }
        if(c.level==null||c.gui.screen()!=null||c.gui.overlay()!=null||!c.isWindowActive()){last=0;return;}
        if(last!=0){
            double ms=(now-last)/1e6;all.add(ms);if(now<=attackUntil)combat.add(ms);PerformanceRecording.frame(ms);
            if(autoRun&&ms>=STALL_MS&&now-lastSnapshot>SNAPSHOT_GAP){lastSnapshot=now;PerformanceRecording.snapshot(output(),"stall"+Math.round(ms)+"ms");}
        }
        last=now;
    }
}
