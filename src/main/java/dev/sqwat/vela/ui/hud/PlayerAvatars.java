package dev.sqwat.vela.ui.hud;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import dev.sqwat.vela.friend.LocalAppearanceService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
public final class PlayerAvatars {
    private static final Map<String, PlayerSkin> cache = new LinkedHashMap<>();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private PlayerAvatars() {}
    public static void draw(GuiGraphicsExtractor g, String name, int x, int y, int size) {
        Minecraft client = Minecraft.getInstance();
        if (name.equalsIgnoreCase(client.getUser().getName()) && client.player != null) {
            PlayerFaceExtractor.extractRenderState(g, client.player.getSkin(), x, y, size); return;
        }
        String key = name.toLowerCase(Locale.ROOT);
        PlayerSkin skin = cache.get(key);
        if (skin == null) {
            UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            skin = DefaultPlayerSkin.get(id);
            if (cache.size() >= 256) cache.remove(cache.keySet().iterator().next());
            cache.put(key, skin);
            if (name.matches("[A-Za-z0-9_]{3,16}")) resolve(name, key, client);
        }
        if (name.equalsIgnoreCase(client.getUser().getName())) skin = LocalAppearanceService.apply(client.getUser().getProfileId(), skin);
        PlayerFaceExtractor.extractRenderState(g, skin, x, y, size);
    }
    private static void resolve(String name, String key, Minecraft client) {
        var request = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name)).timeout(Duration.ofSeconds(6)).GET().build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenCompose(response -> {
            if (response.statusCode() != 200 || response.body().length() > 4096) return CompletableFuture.<Optional<PlayerSkin>>completedFuture(Optional.empty());
            var json = JsonParser.parseString(response.body()).getAsJsonObject();
            String raw = json.get("id").getAsString();
            UUID id = UUID.fromString(raw.replaceFirst("(........)(....)(....)(....)(............)", "$1-$2-$3-$4-$5"));
            return CompletableFuture.supplyAsync(()->client.services().sessionService().fetchProfile(id,false))
                .thenCompose(profile->profile==null?CompletableFuture.completedFuture(Optional.<PlayerSkin>empty()):client.getSkinManager().get(profile.profile()));
        }).thenAccept(skin -> client.execute(() -> skin.ifPresent(value -> {if(cache.containsKey(key))cache.put(key,value);}))).exceptionally(error -> null);
    }
}
