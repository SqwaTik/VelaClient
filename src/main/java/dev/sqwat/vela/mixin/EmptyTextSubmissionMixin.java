package dev.sqwat.vela.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blank sign lines otherwise allocate a text submission and a matrix every frame. */
@Mixin(SubmitNodeCollection.class)
abstract class EmptyTextSubmissionMixin {
 @Inject(method="submitText",at=@At("HEAD"),cancellable=true)
 private void vela$skipEmpty(PoseStack pose,float x,float y,FormattedCharSequence text,boolean shadow,Font.DisplayMode mode,int light,int color,int background,int outline,CallbackInfo ci){
  if(background==0&&dev.sqwat.vela.render.TextSubmission.empty(text))ci.cancel();
 }
}
