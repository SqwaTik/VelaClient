package dev.sqwat.vela.neoforge;

import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.neoforge.shim.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import dev.sqwat.vela.neoforge.shim.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import dev.sqwat.vela.neoforge.shim.fabric.api.client.keymapping.v1.KeyMappingHelper;
import dev.sqwat.vela.neoforge.shim.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import dev.sqwat.vela.neoforge.shim.fabric.api.client.message.v1.ClientSendMessageEvents;
import dev.sqwat.vela.neoforge.shim.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import dev.sqwat.vela.neoforge.shim.fabric.api.client.networking.v1.ClientPlayNetworking;
import dev.sqwat.vela.neoforge.shim.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import dev.sqwat.vela.neoforge.shim.fabric.api.client.screen.v1.ScreenEvents;
import dev.sqwat.vela.neoforge.shim.fabric.api.client.screen.v1.ScreenHooks;
import dev.sqwat.vela.neoforge.shim.fabric.api.networking.v1.PacketSender;
import dev.sqwat.vela.neoforge.shim.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;

/** Runs the Fabric API shims' listeners from the matching NeoForge events. */
final class NeoEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vela/NeoForge");
    private static final List<KeyMapping.Category> VANILLA_CATEGORIES = List.of(KeyMapping.Category.MOVEMENT, KeyMapping.Category.MISC,
        KeyMapping.Category.MULTIPLAYER, KeyMapping.Category.GAMEPLAY, KeyMapping.Category.INVENTORY, KeyMapping.Category.CREATIVE,
        KeyMapping.Category.SPECTATOR);
    private static boolean started;

    private NeoEvents() {}

    static void register(IEventBus modBus) {
        NeoHud.register();
        modBus.addListener(NeoEvents::keys);
        modBus.addListener(NeoEvents::hud);
        modBus.addListener(NeoEvents::payloads);
        var game = NeoForge.EVENT_BUS;
        game.addListener(NeoEvents::tickStart);
        game.addListener(NeoEvents::tickEnd);
        game.addListener((GameShuttingDownEvent e) -> {
            for (var l : ClientLifecycleEvents.CLIENT_STOPPING.listeners()) l.onClientStopping(Minecraft.getInstance());
        });
        game.addListener((ScreenEvent.Init.Pre e) -> ScreenHooks.reset(e.getScreen()));
        game.addListener((ScreenEvent.Init.Post e) -> {
            var screen = e.getScreen();
            for (var l : ScreenEvents.AFTER_INIT.listeners()) l.afterInit(Minecraft.getInstance(), screen, screen.width, screen.height);
        });
        game.addListener((ScreenEvent.Render.Pre e) -> {
            var hooks = ScreenHooks.peek(e.getScreen());
            if (hooks != null) for (var l : hooks.beforeExtract.listeners())
                l.beforeExtract(e.getScreen(), e.getGuiGraphics(), e.getMouseX(), e.getMouseY(), e.getPartialTick());
        });
        game.addListener((ScreenEvent.Render.Post e) -> {
            var hooks = ScreenHooks.peek(e.getScreen());
            if (hooks != null) for (var l : hooks.afterExtract.listeners())
                l.afterExtract(e.getScreen(), e.getGuiGraphics(), e.getMouseX(), e.getMouseY(), e.getPartialTick());
        });
        game.addListener((ScreenEvent.MouseButtonPressed.Pre e) -> {
            var hooks = ScreenHooks.peek(e.getScreen());
            if (hooks != null) for (var l : hooks.allowMouseClick.listeners())
                if (!l.allowMouseClick(e.getScreen(), e.getMouseButtonEvent())) { e.setCanceled(true); return; }
        });
        game.addListener((ScreenEvent.KeyPressed.Pre e) -> {
            var hooks = ScreenHooks.peek(e.getScreen());
            if (hooks != null) for (var l : hooks.allowKeyPress.listeners())
                if (!l.allowKeyPress(e.getScreen(), e.getKeyEvent())) { e.setCanceled(true); return; }
        });
        game.addListener((ScreenEvent.CharacterTyped.Pre e) -> {
            var hooks = ScreenHooks.peek(e.getScreen());
            if (hooks != null) for (var l : hooks.allowCharType.listeners())
                if (!l.allowCharType(e.getScreen(), e.getCharacterEvent())) { e.setCanceled(true); return; }
        });
        game.addListener((ScreenEvent.Closing e) -> {
            var hooks = ScreenHooks.peek(e.getScreen());
            if (hooks != null) for (var l : hooks.remove.listeners()) l.onRemove(e.getScreen());
            ScreenHooks.reset(e.getScreen());
        });
        game.addListener((ClientChatEvent e) -> {
            if (e.getMessage().startsWith("/")) return;
            for (var l : ClientSendMessageEvents.ALLOW_CHAT.listeners())
                if (!l.allowSendChatMessage(e.getMessage())) { e.setCanceled(true); return; }
        });
        game.addListener((ClientChatReceivedEvent.System e) -> {
            for (var l : ClientReceiveMessageEvents.ALLOW_GAME.listeners())
                if (!l.allowReceiveGameMessage(e.getMessage(), e.isOverlay())) { e.setCanceled(true); return; }
            for (var l : ClientReceiveMessageEvents.GAME.listeners()) l.onReceiveGameMessage(e.getMessage(), e.isOverlay());
        });
        game.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> {
            var client = Minecraft.getInstance();
            for (var l : ClientPlayConnectionEvents.JOIN.listeners()) l.onPlayReady(client.getConnection(), PacketSender.SERVER, client);
        });
        game.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            var client = Minecraft.getInstance();
            for (var l : ClientPlayConnectionEvents.DISCONNECT.listeners()) l.onPlayDisconnect(client.getConnection(), client);
        });
        game.addListener((RegisterClientCommandsEvent e) -> ClientCommandBridge.register(e.getDispatcher(), e.getBuildContext()));
    }

    private static void tickStart(ClientTickEvent.Pre e) {
        if (started) return;
        started = true;
        for (var l : ClientLifecycleEvents.CLIENT_STARTED.listeners()) l.onClientStarted(Minecraft.getInstance());
    }

    private static void tickEnd(ClientTickEvent.Post e) {
        for (var l : ClientTickEvents.END_CLIENT_TICK.listeners()) l.onEndTick(Minecraft.getInstance());
    }

    private static void keys(RegisterKeyMappingsEvent e) {
        var categories = new HashSet<KeyMapping.Category>();
        for (var mapping : KeyMappingHelper.PENDING) {
            var category = mapping.getCategory();
            if (!VANILLA_CATEGORIES.contains(category) && categories.add(category)) {
                try { e.registerCategory(category); } catch (RuntimeException alreadyKnown) { LOGGER.debug("Key category {} is already registered", category, alreadyKnown); }
            }
            e.register(mapping);
        }
    }

    private static void hud(RegisterGuiLayersEvent e) {
        for (var entry : HudElementRegistry.PENDING) {
            if (entry.first()) e.registerBelowAll(entry.id(), entry.element()::extractRenderState);
            else e.registerAboveAll(entry.id(), entry.element()::extractRenderState);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void payloads(RegisterPayloadHandlersEvent e) {
        var registrar = e.registrar(VelaClient.MOD_ID).optional();
        for (var codec : PayloadTypeRegistry.clientboundPlay().codecs.entrySet()) {
            var type = (CustomPacketPayload.Type) codec.getKey();
            var handler = (ClientPlayNetworking.PlayPayloadHandler) ClientPlayNetworking.RECEIVERS.get(type);
            registrar.playToClient(type, (StreamCodec<? super RegistryFriendlyByteBuf, CustomPacketPayload>) codec.getValue(),
                (payload, context) -> { if (handler != null) handler.receive(payload, CONTEXT); });
        }
    }

    private static final ClientPlayNetworking.Context CONTEXT = new ClientPlayNetworking.Context() {
        @Override public Minecraft client() { return Minecraft.getInstance(); }

        @Override public LocalPlayer player() { return Minecraft.getInstance().player; }

        @Override public PacketSender responseSender() { return PacketSender.SERVER; }
    };
}
