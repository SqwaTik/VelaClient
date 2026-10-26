package dev.sqwat.vela.friend;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Local launcher overrides. File IO is off-thread; texture registration stays on the client thread. */
public final class LocalAppearanceService {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger("Vela/Appearance");
    private static volatile Map<UUID, OverrideSkin> current = Map.of();
    private static final Set<Identifier> registered = new HashSet<>();
    private static ScheduledExecutorService worker;
    private static volatile boolean stopped;
    private static String lastDigest = "";
    private static String lastError = "";

    private LocalAppearanceService() {}

    public static synchronized void initialize() {
        if (worker != null) return;
        String value = System.getProperty("vela.appearance.manifest", "");
        if (value.isBlank()) return;
        Path manifest = Path.of(value).toAbsolutePath().normalize();
        stopped = false;
        worker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Vela appearance reader"); t.setDaemon(true); return t;
        });
        worker.scheduleWithFixedDelay(() -> reload(manifest), 0, 1, TimeUnit.SECONDS);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            stopped = true;
            worker.shutdownNow();
            current = Map.of();
            for (Identifier id : registered) client.getTextureManager().release(id);
            registered.clear();
        });
    }

    public static PlayerSkin apply(UUID uuid, PlayerSkin original) {
        OverrideSkin entry = current.get(uuid);
        if (entry == null) return original;
        return entry.apply(original);
    }

    private static void reload(Path path) {
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > 1_048_576) return;
            byte[] json = Files.readAllBytes(path);
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
            if (digest.equals(lastDigest)) return;
            Manifest manifest = new Gson().fromJson(new String(json, java.nio.charset.StandardCharsets.UTF_8), Manifest.class);
            if (manifest == null || manifest.version != 1 || manifest.entries == null || manifest.entries.size() > 32)
                throw new IllegalArgumentException("Invalid appearance manifest");
            List<PendingSkin> pending = new ArrayList<>();
            Path root = path.getParent().toRealPath();
            for (Entry entry : manifest.entries) {
                UUID uuid = UUID.fromString(entry.uuid);
                UUID offline = entry.offlineUuid == null ? null : UUID.fromString(entry.offlineUuid);
                pending.add(new PendingSkin(uuid, offline, asset(root, entry.skinFile), asset(root, entry.capeFile),
                    "slim".equals(entry.model) ? PlayerModelType.SLIM : PlayerModelType.WIDE, entry.capeHidden));
            }
            lastDigest = digest;
            Minecraft.getInstance().execute(() -> {
                if (stopped) return;
                try { applyPending(pending); lastError = ""; }
                catch (Exception error) { lastDigest = ""; report(error); }
            });
        } catch (Exception error) { report(error); }
    }

    private static void report(Exception error) {
        String message = error.toString();
        if (!message.equals(lastError)) { LOG.warn("Unable to load local appearance: {}", message); lastError = message; }
    }

    private static byte[] asset(Path root, String file) throws Exception {
        if (file == null || file.isBlank()) return null;
        if (!file.matches("[a-f0-9]{64}\\.png")) throw new IllegalArgumentException("Invalid texture filename");
        Path path = root.resolve(file).toRealPath();
        if (!path.startsWith(root) || Files.size(path) > 4 * 1024 * 1024) throw new IllegalArgumentException("Invalid texture path");
        byte[] bytes = Files.readAllBytes(path);
        if (!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).concat(".png").equals(file))
            throw new IllegalArgumentException("Texture checksum mismatch");
        return bytes;
    }

    private static void applyPending(List<PendingSkin> pending) throws Exception {
        Map<UUID, OverrideSkin> replacement = new HashMap<>();
        Set<Identifier> nextTextures = new HashSet<>();
        try {
            for (PendingSkin entry : pending) {
                var skin = texture(entry.skin, true, nextTextures);
                var cape = texture(entry.cape, false, nextTextures);
                var override = new OverrideSkin(skin, cape, entry.model, entry.capeHidden);
                replacement.put(entry.uuid, override);
                if (entry.offlineUuid != null) replacement.put(entry.offlineUuid, override);
            }
        } catch (Exception error) {
            for (Identifier id : nextTextures) if (!registered.contains(id)) Minecraft.getInstance().getTextureManager().release(id);
            throw error;
        }
        current = Map.copyOf(replacement);
        for (Identifier id : registered) if (!nextTextures.contains(id)) Minecraft.getInstance().getTextureManager().release(id);
        registered.clear(); registered.addAll(nextTextures);
    }

    private static ClientAsset.Texture texture(byte[] bytes, boolean skin, Set<Identifier> used) throws Exception {
        if (bytes == null) return null;
        Identifier id = Identifier.fromNamespaceAndPath("velaclient", "appearance/" +
            (skin ? "skin/" : "cape/") + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        used.add(id);
        if (!registered.contains(id)) {
            if (bytes.length < 24) throw new IllegalArgumentException("Truncated PNG");
            var header = java.nio.ByteBuffer.wrap(bytes);
            int w = header.getInt(16), h = header.getInt(20);
            if (skin ? w != 64 || (h != 32 && h != 64) : w < 64 || w > 1024 || h != w / 2 || (w & (w - 1)) != 0)
                throw new IllegalArgumentException("Unsupported texture dimensions");
            NativeImage image = NativeImage.read(bytes);
            if (skin && image.getHeight() == 32) image = legacySkin(image);
            NativeImage pixels = image;
            try {
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Vela local appearance", pixels));
            } catch (Throwable error) { pixels.close(); throw error; }
        }
        return new ClientAsset.ResourceTexture(id, id);
    }

    private static NativeImage legacySkin(NativeImage old) {
        NativeImage next = new NativeImage(64, 64, true);
        for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++) next.setPixel(x, y, old.getPixel(x, y));
        old.close();
        // Mirror the original right leg and arm into the 1.8+ left-limb UV islands.
        next.copyRect(4,16,16,32,4,4,true,false);
        next.copyRect(8,16,16,32,4,4,true,false);
        next.copyRect(0,20,24,32,4,12,true,false);
        next.copyRect(4,20,16,32,4,12,true,false);
        next.copyRect(8,20,8,32,4,12,true,false);
        next.copyRect(12,20,16,32,4,12,true,false);
        next.copyRect(44,16,-8,32,4,4,true,false);
        next.copyRect(48,16,-8,32,4,4,true,false);
        next.copyRect(40,20,0,32,4,12,true,false);
        next.copyRect(44,20,-8,32,4,12,true,false);
        next.copyRect(48,20,-16,32,4,12,true,false);
        next.copyRect(52,20,-8,32,4,12,true,false);
        return next;
    }

    private static final class OverrideSkin {
        final ClientAsset.Texture skin,cape;
        final PlayerModelType model;
        final boolean capeHidden;
        private PlayerSkin source,result;
        OverrideSkin(ClientAsset.Texture skin,ClientAsset.Texture cape,PlayerModelType model,boolean capeHidden){
            this.skin=skin;this.cape=cape;this.model=model;this.capeHidden=capeHidden;
        }
        synchronized PlayerSkin apply(PlayerSkin original){
            if(original==source)return result;
            source=original;
            return result=new PlayerSkin(skin!=null?skin:original.body(),capeHidden?null:cape!=null?cape:original.cape(),original.elytra(),skin!=null?model:original.model(),original.secure());
        }
    }
    private record PendingSkin(UUID uuid, UUID offlineUuid, byte[] skin, byte[] cape, PlayerModelType model, boolean capeHidden) {}
    private record Manifest(int version, List<Entry> entries) {}
    private record Entry(String uuid, String offlineUuid, String model, String skinFile, String capeFile, boolean capeHidden) {}
}
