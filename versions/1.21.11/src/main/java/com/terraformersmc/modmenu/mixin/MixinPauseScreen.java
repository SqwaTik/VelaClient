package com.terraformersmc.modmenu.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import com.terraformersmc.modmenu.gui.widget.ModMenuButtonWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.11: the pause menu has no icon row, so both styles put the full-width button above Disconnect. */
@Mixin(PauseScreen.class)
public abstract class MixinPauseScreen extends Screen {
    protected MixinPauseScreen(Component title) {
        super(title);
    }

    @Inject(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;I)Lnet/minecraft/client/gui/layouts/LayoutElement;", ordinal = 0))
    private void insertModMenuFullButton(CallbackInfo ci, @Local GridLayout.RowHelper helper) {
        if (!ModMenuConfig.MODIFY_GAME_MENU.getValue()) return;
        ModMenuConfig.GameMenuButtonStyle style = ModMenuConfig.GAME_MENU_BUTTON_STYLE.getValue();
        if (style == ModMenuConfig.GameMenuButtonStyle.INSERT || style == ModMenuConfig.GameMenuButtonStyle.ICON) {
            final int fullWidthButton = 204; // PauseScreen.BUTTON_WIDTH_FULL
            helper.addChild(new ModMenuButtonWidget(
                    0,
                    0,
                    fullWidthButton,
                    20,
                    ModMenuApi.createModsButtonText(),
                    this
            ), 2);
        }
    }
}
