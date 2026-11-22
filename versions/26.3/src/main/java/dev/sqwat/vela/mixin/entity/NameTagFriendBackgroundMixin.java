package dev.sqwat.vela.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sqwat.vela.render.entity.FriendNameTags;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.3: the green friend background, picked while the name is still the entity's component (render thread only). */
@Mixin(SubmitNodeCollection.class)
abstract class NameTagFriendBackgroundMixin {
    @Unique private static Component vela$name;

    @Inject(method = "submitNameTag", at = @At("HEAD"))
    private void vela$remember(PoseStack poseStack, Vec3 attachment, int offset, Component name, boolean seeThrough,
                               int lightCoords, CameraRenderState camera, CallbackInfo ci) {
        vela$name = name;
    }

    @ModifyArg(method = "submitNameTag", index = 6, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;nameTag(Lorg/joml/Matrix4f;FFLnet/minecraft/util/FormattedCharSequence;IIILnet/minecraft/client/gui/Font$DisplayMode;)Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Submit;"))
    private int vela$friendBackground(int background) {
        return FriendNameTags.background(vela$name, background);
    }
}
