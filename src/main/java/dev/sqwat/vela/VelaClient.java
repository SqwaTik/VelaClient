package dev.sqwat.vela;

import com.mojang.blaze3d.platform.InputConstants;
import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.ui.screen.VelaDashboardScreen;
import dev.sqwat.vela.ui.screen.VelaTitleScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class VelaClient implements ClientModInitializer {
    public static final String MOD_ID = "velaclient";
    private static VelaServices services;
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        dev.sqwat.vela.zako.hud.HudCache.initialize();
        dev.sqwat.vela.ui.render.GlassSurface.initialize();
        dev.sqwat.vela.render.WorldSprite.initialize();
        dev.sqwat.vela.ui.render.DissolveBody.initialize();
        dev.sqwat.vela.render.TemporalBlur.initialize();
        dev.sqwat.vela.render.WorldShaderRenderer.initialize();
        dev.sqwat.vela.render.HandShaderRenderer.initialize();
        dev.sqwat.vela.render.SkyShaderRenderer.initialize();
        dev.sqwat.vela.render.PerformanceCapture.initialize();
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((message,overlay)->{
            var accept=dev.sqwat.vela.module.AutoTpAccept.instance();
            if(accept!=null)accept.message(message.getString(),overlay);
        });
        dev.sqwat.vela.cosmetics.LocalAppearanceService.initialize();
        services = new VelaServices(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID));
        net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.ALLOW_CHAT.register(message->dev.sqwat.vela.command.LocalCommands.send(message,services));
        services.zakoOptimization().require(()->!FabricLoader.getInstance().isModLoaded("zakoopt"));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> dev.sqwat.vela.zako.IntegrationRuntime.clear());
        dev.sqwat.vela.marker.MarkerNotices.initialize(services.markers());
        services.modules().byId("speakers").require(()->FabricLoader.getInstance().isModLoaded("voicechat"));
        KeyMapping.Category category = KeyMapping.Category.register(id("controls"));
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.velaclient.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category
        ));
        HudElementRegistry.addLast(id("hud"),(g,delta)->{if(!dev.sqwat.vela.zako.hud.HudCache.active())services.hudLayout().extractRenderState(g,delta);});
        HudElementRegistry.addFirst(id("motion_blur"),(g,delta)->{if(!dev.sqwat.vela.zako.hud.HudCache.active())dev.sqwat.vela.render.TemporalBlur.draw(g);});
        HudElementRegistry.addLast(id("markers"),(g,delta)->{if(!dev.sqwat.vela.zako.hud.HudCache.active())dev.sqwat.vela.marker.MarkerRuntime.draw(g,services.markers());});
        HudElementRegistry.addLast(id("marker_notices"),(g,delta)->{if(!dev.sqwat.vela.zako.hud.HudCache.active()&&Minecraft.getInstance().gui.screen()==null)dev.sqwat.vela.marker.MarkerNotices.draw(g);});
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player,world,hand,entity,hit)->{
            if(player==Minecraft.getInstance().player&&services.configs().current().protectFriends
                &&entity instanceof net.minecraft.world.entity.player.Player friend
                &&services.friends().contains(friend.getGameProfile().name()))return net.minecraft.world.InteractionResult.FAIL;
            if(player==Minecraft.getInstance().player)dev.sqwat.vela.module.HitBubbles.attacked(entity);
            if(player==Minecraft.getInstance().player)dev.sqwat.vela.module.HitSound.attacked(entity);
            if(player==Minecraft.getInstance().player&&services.modules().byId("fake_player") instanceof dev.sqwat.vela.module.FakePlayer fake&&fake.hit(entity)){
                if(entity instanceof net.minecraft.world.entity.LivingEntity living)services.hudLayout().attacked(living);
                player.swing(hand);
                return net.minecraft.world.InteractionResult.FAIL;
            }
            if(player==Minecraft.getInstance().player)dev.sqwat.vela.render.PerformanceCapture.attacked();
            if(player==Minecraft.getInstance().player&&entity instanceof net.minecraft.world.entity.LivingEntity living)
                services.hudLayout().attacked(living);
            return net.minecraft.world.InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var watermark=services.modules().byId("watermark");
            dev.sqwat.vela.ui.HudTelemetry.tickPing(client,watermark!=null&&watermark.enabled()&&watermark.settings().stream().anyMatch(s->s.id().equals("ping")&&Boolean.TRUE.equals(s.value())));
            dev.sqwat.vela.module.CommandAutomation.tick(client,services);
            dev.sqwat.vela.module.Far.clearDisabledTerrain();
            if(client.level!=null)dev.sqwat.vela.ui.SessionPresentation.visit();
            services.hudLayout().viewport(client.getWindow().getGuiScale());
            dev.sqwat.vela.friend.FriendPresence.tick(client,services);
            dev.sqwat.vela.marker.MarkerRuntime.tick(client,services.markers());
            services.hudLayout().tickTarget(client);
            if(client.level==null){dev.sqwat.vela.render.AutomaticVisibility.clear();dev.sqwat.vela.ui.HudTelemetry.clear();dev.sqwat.vela.ui.CooldownTracker.clear();}
            dev.sqwat.vela.ui.ModuleBindings.tick(client, services);
            services.modules().tickEnabled();
            while (openKey.consumeClick()) {
                if (client.gui.screen() instanceof VelaDashboardScreen dashboard) {if(!dashboard.hasModal())dashboard.onClose();}
                else if(client.gui.screen() instanceof dev.sqwat.vela.ui.screen.VelaScreen)continue;
                else { dev.sqwat.vela.ui.UiSounds.play(services, dev.sqwat.vela.ui.UiSounds.Event.OPEN); client.gui.setScreen(new VelaDashboardScreen(client.gui.screen(), services)); }
            }
            if (services.configs().current().replaceMainMenu && client.gui.screen() instanceof TitleScreen) {
                client.gui.setScreen(new VelaTitleScreen(services));
            }
        });
        ClientLifecycleEvents.CLIENT_STARTED.register(this::captureLauncherAccount);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {dev.sqwat.vela.render.TextureDecodeCache.close();dev.sqwat.vela.render.TemporalBlur.close();dev.sqwat.vela.ui.render.GlassSurface.close();services.close();});
    }

    private void captureLauncherAccount(Minecraft client) {
        services.accounts().registerLauncherAccount(client.getUser().getName(), client.getUser().getProfileId());
    }

    public static String openKeyLabel(){return openKey==null?"RShift":openKey.getTranslatedKeyMessage().getString();}
    public static void rebindOpenKey(int key){if(openKey!=null){openKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));KeyMapping.resetMapping();Minecraft.getInstance().options.save();}}
    public static VelaServices services() { return services; }
    public static void extractLiveHud(net.minecraft.client.gui.GuiGraphicsExtractor g,net.minecraft.client.DeltaTracker delta){
        if(services==null)return;
        services.hudLayout().extractRenderState(g,delta);
        dev.sqwat.vela.marker.MarkerRuntime.draw(g,services.markers());
        if(Minecraft.getInstance().gui.screen()==null)dev.sqwat.vela.marker.MarkerNotices.draw(g);
    }
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }
}
