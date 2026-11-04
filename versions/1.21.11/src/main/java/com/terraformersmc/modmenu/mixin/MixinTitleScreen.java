package com.terraformersmc.modmenu.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 1.21.11: the title screen lays out fixed rows (no getHorizontalPosition), so the ICON style has no slot here. */
@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen {
    @ModifyArg(at = @At(value = "INVOKE", target = "Lcom/mojang/realmsclient/gui/screens/RealmsNotificationsScreen;init(II)V"), method = "init", index = 1)
    private int adjustRealmsHeight(int height) {
        if (ModMenuConfig.MODIFY_TITLE_SCREEN.getValue() && ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.TitleMenuButtonStyle.CLASSIC) {
            return height - 51;
        } else if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.TitleMenuButtonStyle.REPLACE_REALMS || ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.TitleMenuButtonStyle.SHRINK) {
            return -99999;
        } else {
            return height;
        }
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/language/I18n;get(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"))
    String modifyModdedVersionString(String id, Object[] args, Operation<String> original) {
        if (ModMenuConfig.MODIFY_TITLE_SCREEN.getValue() && ModMenuConfig.MOD_COUNT_LOCATION.getValue().isOnTitleScreen()) {
            String count = ModMenu.getDisplayedModCount();
            String specificKey = "modmenu.mods." + count;
            String replacementKey = Language.getInstance().has(specificKey) ? specificKey : "modmenu.mods.n";
            if (ModMenuConfig.EASTER_EGGS.getValue() && Language.getInstance().has(specificKey + ".secret")) {
                replacementKey = specificKey + ".secret";
            }

            return I18n.get(replacementKey, count);
        }

        return original.call(id, args);
    }
}
