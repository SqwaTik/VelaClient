package dev.sqwat.vela.gametest;

import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.module.ClientModule;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Smoke and performance gate run before a build is installed (./gradlew verifyClient):
 * every mixin is applied (audit), each Vela screen renders, a world runs with every module switched on, and the
 * median FPS of a fixed scene must stay within 85% of the recorded baseline (perf/fps-baseline.properties).
 */
@SuppressWarnings("UnstableApiUsage")
public final class VelaClientGameTest implements FabricClientGameTest {
    private static final double ALLOWED_DROP=.85;
    /** Modules left off: they open screens, respawn or send commands, which would disturb the scripted run. */
    private static final List<String> SKIP=List.of("auto_reconnect","auto_tpaccept","death","freelook","click_pearl","hand_swap","macros","interval_commands");

    @Override public void runTest(ClientGameTestContext context){
        // 1. Every mixin target loads and every injection applies; a broken hook fails here, not in a match.
        context.runOnClient(mc->org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit());

        // 2. Vela screens render without errors.
        context.setScreen(()->new dev.sqwat.vela.ui.screen.VelaDashboardScreen(null,VelaClient.services()));
        context.waitTicks(10);context.takeScreenshot("vela-dashboard");
        context.setScreen(()->new dev.sqwat.vela.ui.screen.ChangelogScreen(null));
        context.waitTicks(10);context.takeScreenshot("vela-changelog");
        context.setScreen(()->new dev.sqwat.vela.ui.screen.VelaSettingsScreen(null,VelaClient.services()));
        context.waitTicks(10);
        context.setScreen(()->new dev.sqwat.vela.ui.screen.VelaTitleScreen(VelaClient.services()));
        context.waitTicks(20);context.takeScreenshot("vela-title");
        context.setScreen(()->new dev.sqwat.vela.ui.screen.CommandEditorScreen(new dev.sqwat.vela.ui.screen.VelaDashboardScreen(null,VelaClient.services()),VelaClient.services(),false));
        context.waitTicks(10);context.takeScreenshot("vela-macros");
        context.setScreen(()->new dev.sqwat.vela.ui.screen.CommandEditorScreen(new dev.sqwat.vela.ui.screen.VelaDashboardScreen(null,VelaClient.services()),VelaClient.services(),true));
        context.waitTicks(10);context.takeScreenshot("vela-interval");
        context.setScreen(()->null);

        // Resource reload: the loading overlay draws, the reload finishes and the overlay goes away.
        context.runOnClient(mc->mc.reloadResourcePacks());
        context.waitTicks(3);context.takeScreenshot("vela-loading");
        context.waitFor(mc->mc.gui.overlay()==null,ClientGameTestContext.DEFAULT_TIMEOUT);

        context.runOnClient(mc->{
            mc.options.framerateLimit().set(260); // unlimited
            mc.options.enableVsync().set(false);
        });
        try(var world=context.worldBuilder().setUseConsistentSettings(true).create()){
            // The gate is about crashes and FPS: if not every section reports rendered in time, settle and go on.
            try{world.getConnection().waitForChunksRender(true,ClientGameTestContext.DEFAULT_TIMEOUT);}
            catch(AssertionError slow){System.out.println("[Vela gametest] chunks still rendering after the timeout; settling 200 ticks");context.waitTicks(200);}
            // 3. Every module on for a while: catches crashes in ticks and render hooks.
            List<ClientModule> enabled=context.computeOnClient(mc->{
                var on=new ArrayList<ClientModule>();
                for(var m:VelaClient.services().modules().all())if(!m.enabled()&&m.available()&&!SKIP.contains(m.id())){m.setEnabled(true);on.add(m);}
                return on;
            });
            context.waitTicks(100);
            context.setScreen(()->new dev.sqwat.vela.ui.screen.StorageIndexScreen(dev.sqwat.vela.module.utility.StorageIndex.current()));
            context.waitTicks(10);context.takeScreenshot("vela-storage");
            context.setScreen(()->null);
            context.takeScreenshot("vela-world-all-modules");
            context.runOnClient(mc->{for(var m:enabled)m.setEnabled(false);});

            // 4. FPS of a fixed scene with the default modules.
            context.runOnClient(mc->{mc.player.setYRot(0);mc.player.setXRot(10);});
            context.waitTicks(300); // warm-up: shader and chunk compilation settle (a cold run measured half the FPS)
            var samples=new ArrayList<Integer>();
            for(int i=0;i<20;i++){context.waitTicks(20);samples.add(context.computeOnClient(Minecraft::getFps));}
            check(samples);
        }
    }

    /**
     * The baseline is the median of the last five passing runs (perf/fps-baseline.properties), not one number: the
     * same code measured 894 and 721 on this machine at different times, so a single recording made the gate
     * unreliable. A passing run joins the history; a run below 85% of the baseline fails the build.
     */
    private static void check(List<Integer> samples){
        Collections.sort(samples);
        int median=samples.get(samples.size()/2);
        Path file=Path.of(System.getProperty("vela.fpsBaseline","../../perf/fps-baseline.properties")).toAbsolutePath().normalize();
        var props=new Properties();
        try{
            if(Files.exists(file))try(var in=Files.newInputStream(file)){props.load(in);}
            var history=new ArrayList<Integer>();
            for(String part:props.getProperty("history",props.getProperty("median","")).split(","))if(!part.isBlank())history.add(Integer.parseInt(part.strip()));
            boolean rebaseline=Boolean.getBoolean("vela.fpsRebaseline");
            if(rebaseline)history.clear();
            var sorted=new ArrayList<>(history);Collections.sort(sorted);
            Integer baseline=sorted.isEmpty()?null:sorted.get(sorted.size()/2);
            System.out.println("[Vela gametest] FPS samples "+samples+", median "+median+(baseline==null?"":", baseline "+baseline+" from "+history));
            if(baseline!=null&&median<baseline*ALLOWED_DROP)
                throw new AssertionError("FPS regression: median "+median+" < "+Math.round(ALLOWED_DROP*100)+"% of baseline "+baseline+" (samples "+samples+")");
            history.add(median);
            while(history.size()>5)history.removeFirst();
            props.remove("median");
            props.setProperty("history",String.join(",",history.stream().map(String::valueOf).toList()));
            Files.createDirectories(file.getParent());
            try(var out=Files.newOutputStream(file)){props.store(out,"Vela FPS history of the fixed gametest scene (last 5 passing medians). Run with -Prebaseline to start over.");}
        }catch(java.io.IOException error){throw new AssertionError("Cannot read or write the FPS baseline "+file,error);}
    }
}
