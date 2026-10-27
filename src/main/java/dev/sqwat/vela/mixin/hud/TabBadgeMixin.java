package dev.sqwat.vela.mixin.hud;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.sqwat.vela.friend.VelaUsers;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Vela players get the Vela logo in front of their tab list name; a glyph of the velaclient:badge font. */
@Mixin(PlayerTabOverlay.class)
abstract class TabBadgeMixin {
    @Unique private static final Component VELA$BADGE=Component.literal("\uE000").withStyle(Style.EMPTY
        .withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("velaclient","badge"))).withColor(0xFFFFFF).withBold(false).withItalic(false));
    @ModifyReturnValue(method="getNameForDisplay",at=@At("RETURN"))
    private Component vela$badge(Component name,PlayerInfo info){
        if(!VelaUsers.isVela(info.getProfile().id()))return name;
        return Component.empty().append(VELA$BADGE).append(" ").append(name);
    }
}
