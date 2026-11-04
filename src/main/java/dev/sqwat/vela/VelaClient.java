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
        dev.sqwat.vela.ui.render.GlassSurface.initialize();
        dev.sqwat.vela.render.effect.WorldSprite.initialize();
        dev.sqwat.vela.ui.render.DissolveBody.initialize();
        dev.sqwat.vela.render.shader.TemporalBlur.initialize();
        dev.sqwat.vela.render.shader.WorldShaderRenderer.initialize();
        dev.sqwat.vela.render.shader.HandShaderRenderer.initialize();
        dev.sqwat.vela.render.shader.SkyShaderRenderer.initialize();
        dev.sqwat.vela.render.perf.PerformanceCapture.initialize();
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((message,overlay)->{
            var accept=dev.sqwat.vela.module.utility.AutoTpAccept.instance();
            if(accept!=null)accept.message(message.getString(),overlay);
        });
        dev.sqwat.vela.ui.render.VelaDraw.prewarmFonts();
        dev.sqwat.vela.friend.LocalAppearanceService.initialize();
        dev.sqwat.vela.ui.screen.ContainerSearchBox.initialize();
        dev.sqwat.vela.module.visual.DeathRecap.initialize();
        services = new VelaServices(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID));
        dev.sqwat.vela.ui.Lang.set("ru".equals(services.configs().current().language)?dev.sqwat.vela.ui.Lang.Language.RU:dev.sqwat.vela.ui.Lang.Language.EN);
        net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.ALLOW_CHAT.register(message->dev.sqwat.vela.core.LocalCommands.send(message,services));
        net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.ALLOW_CHAT.register(dev.sqwat.vela.module.utility.ChatTweaks::allowChat);
        dev.sqwat.vela.module.utility.ChatThreads.initialize();
        dev.sqwat.vela.marker.MarkerRuntime.registerCommand(services.markers());
        dev.sqwat.vela.marker.MarkerNotices.initialize(services.markers());
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->client.execute(()->{
            var server=client.getCurrentServer();
            var off=dev.sqwat.vela.module.ServerRules.join(server==null?null:server.ip,services.modules());
            var project=dev.sqwat.vela.module.ServerRules.active();
            if(project!=null&&!off.isEmpty())dev.sqwat.vela.ui.Notifications.show(project.name()+": "+dev.sqwat.vela.ui.Lang.tr("правила сервера"),
                dev.sqwat.vela.ui.Lang.tr("Отключено")+": "+String.join(", ",off),"settings",0xFFFFA24D,"",null,()->true,0);
            dev.sqwat.vela.core.UpdateCheck.joined();
        }));
        dev.sqwat.vela.core.ServerOptOut.register(services.modules());
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->client.execute(()->{dev.sqwat.vela.module.ServerRules.leave(services.modules());dev.sqwat.vela.core.UpdateCheck.left();}));
        services.modules().byId("speakers").require(()->FabricLoader.getInstance().isModLoaded("voicechat"));
        KeyMapping.Category category = KeyMapping.Category.register(id("controls"));
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.velaclient.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category
        ));
        // Under the HUD cache the panels go into the cached texture; world-anchored effects, motion blur and markers
        // follow the camera, so HudCache draws them live every frame instead.
        HudElementRegistry.addLast(id("hud"),(g,delta)->{
            if(dev.sqwat.vela.render.perf.HudCache.cachedPass())services.hudLayout().extractPanels(g,delta);
            else services.hudLayout().extractRenderState(g,delta);
        });
        HudElementRegistry.addFirst(id("motion_blur"),(g,delta)->{if(!dev.sqwat.vela.render.perf.HudCache.cachedPass())dev.sqwat.vela.render.shader.TemporalBlur.draw(g);});
        HudElementRegistry.addLast(id("markers"),(g,delta)->{if(!dev.sqwat.vela.render.perf.HudCache.cachedPass())dev.sqwat.vela.marker.MarkerRuntime.draw(g,services.markers());});
        HudElementRegistry.addLast(id("marker_notices"),(g,delta)->{if(Minecraft.getInstance().gui.screen()==null)dev.sqwat.vela.marker.MarkerNotices.draw(g);});
        dev.sqwat.vela.module.utility.LootMemory.initialize();
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player,world,hand,entity,hit)->{
            if(player==Minecraft.getInstance().player&&services.configs().current().protectFriends
                &&entity instanceof net.minecraft.world.entity.player.Player friend
                &&services.friends().contains(friend.getGameProfile().name()))return net.minecraft.world.InteractionResult.FAIL;
            if(player==Minecraft.getInstance().player)dev.sqwat.vela.module.visual.HitBubbles.attacked(entity);
            if(player==Minecraft.getInstance().player)dev.sqwat.vela.module.visual.MaceSmash.attacked(player,entity);
            if(player==Minecraft.getInstance().player)dev.sqwat.vela.module.utility.HitSound.attacked(entity);
            if(player==Minecraft.getInstance().player&&services.modules().byId("fake_player") instanceof dev.sqwat.vela.module.utility.FakePlayer fake&&fake.hit(entity)){
                if(entity instanceof net.minecraft.world.entity.LivingEntity living)services.hudLayout().attacked(living);
                player.swing(hand);
                return net.minecraft.world.InteractionResult.FAIL;
            }
            if(player==Minecraft.getInstance().player)dev.sqwat.vela.render.perf.PerformanceCapture.attacked();
            if(player==Minecraft.getInstance().player&&entity instanceof net.minecraft.world.entity.LivingEntity living)
                services.hudLayout().attacked(living);
            return net.minecraft.world.InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            dev.sqwat.vela.render.entity.NameTagTextCache.tick();
            dev.sqwat.vela.render.entity.ArmorMemory.tick(client);
            // Follows the active config profile, which can be switched at any time.
            dev.sqwat.vela.ui.Lang.set("ru".equals(services.configs().current().language)?dev.sqwat.vela.ui.Lang.Language.RU:dev.sqwat.vela.ui.Lang.Language.EN);
            var watermark=services.modules().byId("watermark");
            dev.sqwat.vela.ui.hud.HudTelemetry.tickPing(client,watermark!=null&&watermark.enabled()&&watermark.settings().stream().anyMatch(s->s.id().equals("ping")&&Boolean.TRUE.equals(s.value())));
            dev.sqwat.vela.module.utility.CommandAutomation.tick(client,services);
            dev.sqwat.vela.module.utility.Far.clearDisabledTerrain();
            if(client.level!=null)dev.sqwat.vela.ui.SessionPresentation.visit();
            services.hudLayout().viewport(client.getWindow().getGuiScale());
            dev.sqwat.vela.friend.FriendPresence.tick(client,services);
            dev.sqwat.vela.friend.VelaUsers.tick(client);
            dev.sqwat.vela.marker.MarkerRuntime.tick(client,services.markers());
            services.hudLayout().tickTarget(client);
            if(client.level==null){dev.sqwat.vela.render.entity.AutomaticVisibility.clear();dev.sqwat.vela.ui.hud.HudTelemetry.clear();dev.sqwat.vela.ui.hud.CooldownTracker.clear();}
            dev.sqwat.vela.ui.input.ModuleBindings.tick(client, services);
            services.modules().tickEnabled();
            while (openKey.consumeClick()) {
                // A combination (Ctrl+RShift) also needs its modifiers held.
                if(!dev.sqwat.vela.ui.input.BindInput.modifiersHeld(client.getWindow().handle(),services.configs().current().openModifiers))continue;
                if (client.gui.screen() instanceof VelaDashboardScreen dashboard) {if(!dashboard.hasModal())dashboard.onClose();}
                else if(client.gui.screen() instanceof dev.sqwat.vela.ui.screen.VelaScreen)continue;
                else { dev.sqwat.vela.ui.UiSounds.play(services, dev.sqwat.vela.ui.UiSounds.Event.OPEN); client.gui.setScreen(new VelaDashboardScreen(client.gui.screen(), services)); }
            }
            if (services.configs().current().replaceMainMenu && client.gui.screen() instanceof TitleScreen) {
                client.gui.setScreen(new VelaTitleScreen(services));
            }
        });
        ClientLifecycleEvents.CLIENT_STARTED.register(this::captureLauncherAccount);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {dev.sqwat.vela.render.resource.TextureDecodeCache.close();dev.sqwat.vela.render.shader.TemporalBlur.close();dev.sqwat.vela.ui.render.GlassSurface.close();services.close();});
    }

    private void captureLauncherAccount(Minecraft client) {
        services.accounts().registerLauncherAccount(client.getUser().getName(), client.getUser().getProfileId());
    }

    public static String openKeyLabel(){
        if(openKey==null)return "RShift";
        int mods=services.configs().current().openModifiers;
        String prefix=((mods&dev.sqwat.vela.ui.input.BindInput.CTRL)!=0?"Ctrl+":"")+((mods&dev.sqwat.vela.ui.input.BindInput.SHIFT)!=0?"Shift+":"")+((mods&dev.sqwat.vela.ui.input.BindInput.ALT)!=0?"Alt+":"");
        return prefix+openKey.getTranslatedKeyMessage().getString();
    }
    /** {@code code}: a key with optional Ctrl/Shift/Alt (BindInput); the key goes to the vanilla mapping, the modifiers to the config. */
    public static void rebindOpenKey(int code){
        if(openKey==null)return;
        openKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(dev.sqwat.vela.ui.input.BindInput.base(code)));KeyMapping.resetMapping();Minecraft.getInstance().options.save();
        services.configs().current().openModifiers=dev.sqwat.vela.ui.input.BindInput.modifiers(code);services.configs().requestSave();
    }
    public static void resetOpenKey(){rebindOpenKey(GLFW.GLFW_KEY_RIGHT_SHIFT);}
    public static VelaServices services() { return services; }
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }
}
