package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/** Controls cached terrain; does not extend server entity tracking or simulation. */
public final class Far extends ClientModule {
    private final NumberSetting distance=setting(new NumberSetting("distance","Дальность, чанки","Ранее полученный рельеф; большая дальность увеличивает нагрузку",16,4,64,1));
    private Integer previous,applied;
    public Far(){super("far","Far","Показывает сохранённые чанки вдали",ModuleCategory.UTILITIES);}
    public static boolean active(){
        var services=dev.sqwat.vela.VelaClient.services();
        return services!=null&&services.far().enabled();
    }
    private static Object graceLevel;private static long graceStart;
    /** Saved chunks are only shown after ten seconds in a level. Anti-bot checks drop the player into an empty world
     *  and watch them fall; saved terrain from an earlier visit under their feet would stop the fall and fail the check. */
    public static boolean fakeChunksAllowed(){
        if(!active())return false;
        var level=Minecraft.getInstance().level;long now=System.nanoTime();
        if(level!=graceLevel){graceLevel=level;graceStart=now;}
        return now-graceStart>10_000_000_000L;
    }
    @Override public void onClientTick(){
        var client=Minecraft.getInstance();
        if(client.level==null||client.getSingleplayerServer()!=null){restore();return;}
        var option=client.options.renderDistance();int now=option.get();
        if(previous==null||applied!=null&&now!=applied)previous=now;
        int next=distance.value().intValue();
        if(now!=next)option.set(next);
        applied=next;
    }
    @Override protected void onDisable(){restore();clearDisabledTerrain();}
    public static void clearDisabledTerrain(){
        if(active())return;
        var client=Minecraft.getInstance();
        if(client.level==null)return;
        var cache=((de.johni0702.minecraft.bobby.ext.ClientChunkCacheExt)client.level.getChunkSource()).bobby_getFakeChunkManager();
        if(cache==null||cache.getFakeChunks().isEmpty())return;
        for(var chunk:new java.util.ArrayList<>(cache.getFakeChunks())){var p=chunk.getPos();cache.unload(p.x(),p.z(),false);}
        // Rebuild the extraction dirty tracker together with compiled geometry.
        // Invalidating only the renderer leaves previously clean sections invisible.
        client.levelExtractor.allChanged();
    }
    private void restore(){
        if(previous!=null){var option=Minecraft.getInstance().options.renderDistance();if(applied!=null&&option.get().equals(applied))option.set(previous);}
        previous=applied=null;
    }
}
