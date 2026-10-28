package dev.sqwat.vela.render.perf;

import dev.sqwat.vela.render.entity.NameTagTextCache;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Explicit local measurement; no account or server data is recorded. */
public final class PerformanceCapture {
    private static FrameSamples all,combat;
    private static long last,attackUntil;
    private static Object level;
    private static String lastReport;
    private PerformanceCapture(){}
    public static void initialize(){
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
    private static void start(boolean profile){
        if(all!=null)finish("Начат новый замер");
        PerformanceRecording.cancel();
        all=combat=null;last=attackUntil=0;level=null;
        if(profile)PerformanceRecording.start();
        NameTagTextCache.resetStatistics();
        all=new FrameSamples(65536);combat=new FrameSamples(65536);last=attackUntil=0;level=Minecraft.getInstance().level;
        logContext("start");
    }
    private static String finish(String reason){
        lastReport=reason+": "+format("Все кадры",all.summary())+"; "+format("После атак",combat.summary());
        all=combat=null;last=attackUntil=0;level=null;
        PerformanceRecording.finish(net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("debug/vela-perf"));
        org.slf4j.LoggerFactory.getLogger("Vela/Performance").info(lastReport);
        org.slf4j.LoggerFactory.getLogger("Vela/Performance").info(NameTagTextCache.statistics());
        logContext("stop");
        return lastReport;
    }
    private static void logContext(String phase){
        var client=Minecraft.getInstance();var runtime=Runtime.getRuntime();
        org.slf4j.LoggerFactory.getLogger("Vela/Performance").info(
            "Context {}: resolution={}x{}, processors={}, heapUsedMiB={}, heapCommittedMiB={}, heapMaxMiB={}",
            phase,client.getWindow().getWidth(),client.getWindow().getHeight(),runtime.availableProcessors(),
            (runtime.totalMemory()-runtime.freeMemory())/(1024*1024),runtime.totalMemory()/(1024*1024),runtime.maxMemory()/(1024*1024));
    }
    private static String format(String name,FrameSamples.Summary s){return String.format(java.util.Locale.ROOT,dev.sqwat.vela.ui.Lang.tr("%s: n=%d, среднее %.2f мс, p95 %.2f мс, p99 %.2f мс, max %.2f мс, >16.67 мс: %d"),name,s.frames(),s.averageMs(),s.p95Ms(),s.p99Ms(),s.maxMs(),s.over16Ms());}
    public static void attacked(){if(all!=null)attackUntil=System.nanoTime()+1_000_000_000L;}
    public static void frame(){
        if(all==null)return;
        var c=Minecraft.getInstance();long now=System.nanoTime();
        if(c.level!=level){finish("Смена мира — замер сохранён");return;}
        if(c.level==null||c.gui.screen()!=null||c.gui.overlay()!=null||!c.isWindowActive()){last=0;return;}
        if(last!=0){double ms=(now-last)/1e6;all.add(ms);if(now<=attackUntil)combat.add(ms);PerformanceRecording.frame(ms);}
        last=now;
    }
}
