package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.render.effect.NeonGeometry;
import dev.sqwat.vela.render.effect.ParticleAtlas;
import dev.sqwat.vela.render.effect.WorldParticleField;
import dev.sqwat.vela.render.effect.WorldSprite;
import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.ui.hud.HudVisibility;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import java.util.List;
import java.util.Random;

public final class WorldParticles extends ClientModule {
    private static final Identifier ATLAS=Identifier.fromNamespaceAndPath("velaclient","textures/effects/particles/atlas.png");
    private final NumberSetting count=setting(new NumberSetting("count","Количество","Максимум частиц вокруг игрока",48,8,128,8));
    private final NumberSetting radius=setting(new NumberSetting("radius","Радиус","Радиус появления в блоках",12,4,24,1));
    private final NumberSetting size=setting(new NumberSetting("size","Размер","Размер фоновых частиц",.2,.1,.5,.05));
    private final ChoiceSetting shape=setting(new ChoiceSetting("shape","Форма","Вид фоновых частиц","Звезда",List.of("Все","Точка","Звезда","Снежинка")));
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Использовать цвет темы",true));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет",0x91ACFF));
    private final WorldParticleField field=new WorldParticleField();
    private final Random random=new Random();
    private Object world;
    private WorldSprite sprites;
    private int visibilityCursor;
    public WorldParticles(){
        super("world_particles","World Particles","Плавные фоновые частицы вокруг игрока",ModuleCategory.VISUALS);
        color.visibleWhen(()->!theme.value());
    }
    @Override protected void onDisable(){field.clear();world=null;sprites=null;visibilityCursor=0;}
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){onDisable();return;}
        if(world!=mc.level){onDisable();world=mc.level;}
        if(mc.isPaused())return;
        long now=System.nanoTime();
        field.tick(mc.player.position(),count.value().intValue(),radius.value(),now,random);
        var particles=field.particles();
        // Bound collision queries per tick; rendering performs no world queries.
        var camera=mc.gameRenderer.mainCamera().position();
        for(int i=0,n=Math.min(8,particles.size());i<n;i++){
            var p=particles.get(Math.floorMod(visibilityCursor++,particles.size()));
            var position=p.position(now);
            p.visible=mc.level.hasChunkAt(BlockPos.containing(position))&&mc.level.clip(new ClipContext(camera,position,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player)).getType()==HitResult.Type.MISS;
        }
    }
    public void draw(GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();
        if(!enabled()||mc.level==null||world!=mc.level||field.particles().isEmpty()||!HudVisibility.allowed(mc))return;
        if(sprites==null)sprites=new WorldSprite(true);else sprites.begin();
        long now=System.nanoTime();int rgb=theme.value()?VelaTheme.PRIMARY:color.value();
        int chosen=switch(shape.value()){case "Точка"->0;case "Звезда"->1;case "Снежинка"->9;default->-1;};
        for(var p:field.particles()){
            if(!p.visible)continue;
            double alpha=p.opacity(now);if(alpha<=0)continue;
            int index=chosen<0?p.shape:chosen;
            sprites.drawRegion(g,ATLAS,128,128,p.position(now),size.value().floatValue(),p.phase,NeonGeometry.tint(rgb,alpha*.85),ParticleAtlas.u(index),ParticleAtlas.v(index),ParticleAtlas.u1(index),ParticleAtlas.v1(index));
        }
        sprites.flush(g);
    }
}
