package dev.sqwat.vela.friend;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerSkin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Mojang skins looked up by name, for profiles that arrive without textures (offline accounts, offline-mode servers). */
public final class NameSkins {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final Map<String, Optional<PlayerSkin>> cache = new ConcurrentHashMap<>();
    private static final Set<String> pending = ConcurrentHashMap.newKeySet();

    private NameSkins() {}

    /** The vanilla skin unless the profile has no textures and a Mojang skin is known for its name. */
    public static PlayerSkin forProfile(GameProfile profile, PlayerSkin original) {
        if (profile == null || profile.properties().containsKey("textures")) return original;
        PlayerSkin skin = get(profile.name());
        return skin == null ? original : skin;
    }

    /** The Mojang skin for this name, or null while it is loading or when the name has none. */
    public static PlayerSkin get(String name) {
        if (name == null || !NAME.matcher(name).matches()) return null;
        String key = name.toLowerCase(Locale.ROOT);
        Optional<PlayerSkin> skin = cache.get(key);
        if (skin != null) return skin.orElse(null);
        if (cache.size() >= 512) cache.clear();
        if (pending.add(key)) resolve(name, key);
        return null;
    }

    private static void resolve(String name, String key) {
        Minecraft client = Minecraft.getInstance();
        var request = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name)).timeout(Duration.ofSeconds(6)).GET().build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenCompose(response -> {
            if (response.statusCode() != 200 || response.body().length() > 4096) return CompletableFuture.completedFuture(Optional.<PlayerSkin>empty());
            String raw = JsonParser.parseString(response.body()).getAsJsonObject().get("id").getAsString();
            UUID id = UUID.fromString(raw.replaceFirst("(........)(....)(....)(....)(............)", "$1-$2-$3-$4-$5"));
            return CompletableFuture.supplyAsync(() -> client.services().sessionService().fetchProfile(id, false))
                .thenCompose(profile -> profile == null ? CompletableFuture.completedFuture(Optional.<PlayerSkin>empty()) : client.getSkinManager().get(profile.profile()));
        }).whenComplete((skin, error) -> {
            cache.put(key, skin == null ? Optional.empty() : skin);
            pending.remove(key);
        });
    }
}
