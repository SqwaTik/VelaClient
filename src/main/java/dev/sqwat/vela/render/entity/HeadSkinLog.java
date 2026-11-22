package dev.sqwat.vela.render.entity;

import net.minecraft.world.item.component.ResolvableProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Diagnostics for player heads whose skin goes missing (custom "balls" on servers behind ViaVersion): each distinct
 * head profile is logged once with what the server sent and how the skin lookup ended. Bounded, so a server full of
 * heads cannot grow it.
 */
public final class HeadSkinLog {
    private static final Logger LOG = LoggerFactory.getLogger("Vela/Heads");
    private static final Pattern URL = Pattern.compile("\"url\"\s*:\s*\"([^\"]+)\"");
    private static final int LIMIT = 48;
    private static final Set<ResolvableProfile> seen = ConcurrentHashMap.newKeySet();

    private HeadSkinLog() {}

    public static void lookup(ResolvableProfile profile, CompletableFuture<? extends Optional<?>> future) {
        if (profile == null || future == null || seen.size() >= LIMIT || !seen.add(profile)) return;
        var partial = profile.partialProfile();
        var textures = partial.properties().get("textures");
        String url = "none";
        boolean signed = false;
        for (var property : textures) {
            signed |= property.hasSignature();
            try {
                var json = new String(Base64.getMimeDecoder().decode(property.value()), StandardCharsets.UTF_8);
                var m = URL.matcher(json);
                url = m.find() ? m.group(1) : "no url in " + json.substring(0, Math.min(120, json.length()));
            } catch (IllegalArgumentException bad) {
                url = "bad base64 (" + property.value().length() + " chars)";
            }
        }
        String what = profile.getClass().getSimpleName() + " name=" + profile.name().orElse("-") + " id=" + partial.id()
            + " properties=" + partial.properties().keySet() + " signed=" + signed + " skin=" + url;
        future.whenComplete((result, error) -> {
            if (error != null) LOG.warn("Head {} -> lookup failed: {}", what, error.toString());
            else if (result == null || result.isEmpty()) LOG.warn("Head {} -> no skin, drawn with the default", what);
            else LOG.info("Head {} -> skin loaded", what);
        });
    }
}
