package dev.sqwat.vela.zako;

/** Render-thread lifecycle for the embedded optimizations. */
public final class IntegrationRuntime {
    private static boolean used;
    private static final java.util.concurrent.atomic.AtomicBoolean resetRequested=new java.util.concurrent.atomic.AtomicBoolean();
    private static Object world;
    private static boolean parallel,models,atlas,hud,animation,occlusion;
    public static long generation;
    private IntegrationRuntime(){}
    public static void requestReset(){resetRequested.set(true);}
    /** Consume UI/config changes before refreshing features or starting GPU work. */
    public static void beginFrame(){if(resetRequested.getAndSet(false))clear();}
    public static void frame(){
        var client=net.minecraft.client.Minecraft.getInstance();
        boolean enabled=dev.sqwat.vela.module.ZakoOptimization.active()!=null;
        if(used&&(!enabled||world!=client.level))clear();
        if(enabled){
            if(occlusion&&!dev.sqwat.vela.zako.feature.Features.OCCLUSION_CULL.on&&MixinPlugin.SODIUM)dev.sqwat.vela.zako.gl.OcclusionCull.clear();
            if(models&&!ZakoOptConfig.parallelModels()){
                dev.sqwat.vela.zako.entity.ParallelModels.clear();
                dev.sqwat.vela.zako.entity.ParallelModelHook.clear();
            }
            if(ZakoOptConfig.parallelModels())dev.sqwat.vela.zako.entity.ParallelModels.trim(System.nanoTime());
            if(parallel&&!ZakoOptConfig.parallelModels()&&!ZakoOptConfig.parallelParticles()){
                entityWorkers();
            }
            if(atlas&&!ZakoOptConfig.skinAtlas())dev.sqwat.vela.zako.entity.SkinAtlas.clear();
            if(hud&&!ZakoOptConfig.hudCache())dev.sqwat.vela.zako.hud.HudCache.clear();
            if(animation&&!ZakoOptConfig.entityAnimFreeze())dev.sqwat.vela.zako.entity.AnimFreeze.clear();
        }
        used=enabled;world=client.level;
        occlusion=dev.sqwat.vela.zako.feature.Features.OCCLUSION_CULL.on;
        models=ZakoOptConfig.parallelModels();
        parallel=ZakoOptConfig.parallelModels()||ZakoOptConfig.parallelParticles();atlas=ZakoOptConfig.skinAtlas();hud=ZakoOptConfig.hudCache();animation=ZakoOptConfig.entityAnimFreeze();
    }
    private static void entityWorkers(){
        dev.sqwat.vela.zako.entity.ParallelModels.clear();
        dev.sqwat.vela.zako.entity.ParallelModelHook.clear();
        dev.sqwat.vela.zako.particle.ParallelParticles.clear();
        dev.sqwat.vela.zako.particle.Workers.shutdown();
    }
    public static void clear(){
        generation++;
        if(!used)return;
        dev.sqwat.vela.zako.gl.GpuTimers.clear();
        if(MixinPlugin.SODIUM)dev.sqwat.vela.zako.gl.OcclusionCull.clear();
        entityWorkers();
        dev.sqwat.vela.zako.entity.AnimFreeze.clear();
        dev.sqwat.vela.zako.entity.SkinAtlas.clear();
        dev.sqwat.vela.zako.entity.ItemBounds.clear();
        dev.sqwat.vela.zako.entity.ItemLod.clear();
        dev.sqwat.vela.zako.block.SpawnerReplay.clear();
        dev.sqwat.vela.zako.text.PreparedTextCache.clear();
        dev.sqwat.vela.zako.hud.HudCache.clear();
        dev.sqwat.vela.zako.entity.PosePool.clear();
        dev.sqwat.vela.zako.gl.RenderScale.clear();
        dev.sqwat.vela.zako.gl.TexBufferCache.forget();
        dev.sqwat.vela.zako.gl.SharedDepthFbos.reset();
        used=false;world=null;
    }
}
