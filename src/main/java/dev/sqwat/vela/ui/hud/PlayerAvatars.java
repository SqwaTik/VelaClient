package dev.sqwat.vela.ui.hud;
import dev.sqwat.vela.friend.LocalAppearanceService;
import dev.sqwat.vela.friend.NameSkins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
public final class PlayerAvatars {
    private PlayerAvatars() {}
    public static void draw(GuiGraphicsExtractor g, String name, int x, int y, int size) {
        Minecraft client = Minecraft.getInstance();
        if (name.equalsIgnoreCase(client.getUser().getName()) && client.player != null) {
            PlayerFaceExtractor.extractRenderState(g, client.player.getSkin(), x, y, size); return;
        }
        PlayerSkin skin = NameSkins.get(name);
        if (skin == null) skin = DefaultPlayerSkin.get(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
        if (name.equalsIgnoreCase(client.getUser().getName())) skin = LocalAppearanceService.apply(client.getUser().getProfileId(), skin);
        PlayerFaceExtractor.extractRenderState(g, skin, x, y, size);
    }
}
