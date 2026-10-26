package dev.sqwat.vela;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sqwat.vela.zako.entity.PosePool;
import dev.sqwat.vela.zako.hud.HudDelta;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZakoLifecycleTest {
    @Test void disabledGpuTimersNeverAcquireContextAndClearIsIdempotent(){
        boolean enabled=dev.sqwat.vela.zako.ZakoOptConfig.enabled;
        try{
            dev.sqwat.vela.zako.ZakoOptConfig.enabled=false;
            dev.sqwat.vela.zako.gl.GpuTimers.selectFrame();
            assertFalse(dev.sqwat.vela.zako.gl.GpuTimers.ownsFrame());
            dev.sqwat.vela.zako.gl.GpuTimers.clear();dev.sqwat.vela.zako.gl.GpuTimers.clear();
            assertEquals(-1,dev.sqwat.vela.zako.gl.GpuTimers.utilization());
        }finally{dev.sqwat.vela.zako.ZakoOptConfig.enabled=enabled;}
    }
    @Test void quickDisableEnableDefersAndCoalescesResetUntilFrameBoundary(){
        dev.sqwat.vela.zako.IntegrationRuntime.beginFrame();
        long before=dev.sqwat.vela.zako.IntegrationRuntime.generation;
        var module=new dev.sqwat.vela.module.ZakoOptimization();
        module.require(()->false);module.require(()->true);
        module.require(()->false);module.require(()->true);
        assertEquals(before,dev.sqwat.vela.zako.IntegrationRuntime.generation);
        dev.sqwat.vela.zako.IntegrationRuntime.beginFrame();
        assertEquals(before+1,dev.sqwat.vela.zako.IntegrationRuntime.generation);
        dev.sqwat.vela.zako.IntegrationRuntime.beginFrame();
        assertEquals(before+1,dev.sqwat.vela.zako.IntegrationRuntime.generation);
    }
    @Test void hudDeltaAccumulatesOnlyBetweenRefreshes(){
        HudDelta.idle();
        HudDelta.frame(true,0.25f);assertEquals(0.25f,HudDelta.sinceRefresh());
        HudDelta.frame(false,0.25f);HudDelta.frame(true,0.5f);
        assertEquals(0.75f,HudDelta.sinceRefresh());
        HudDelta.idle();HudDelta.frame(true,0.1f);assertEquals(0.1f,HudDelta.sinceRefresh());
    }
    @Test void posesAreReusedBetweenFramesAndReleasedOnClear(){
        PosePool.clear();PosePool.reset();
        var pose=new PoseStack().last();var first=PosePool.copy(pose);
        assertNotSame(first,PosePool.copy(pose));
        PosePool.reset();assertSame(first,PosePool.copy(pose));
        PosePool.clear();PosePool.reset();assertNotSame(first,PosePool.copy(pose));
        PosePool.clear();
    }
}
