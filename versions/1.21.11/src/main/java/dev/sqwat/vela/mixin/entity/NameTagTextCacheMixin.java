package dev.sqwat.vela.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.render.entity.NameTagTextCache;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 1.21.11 lays name tags out inside Font.drawInBatch: the same draw with the cached layout. */
@Mixin(NameTagFeatureRenderer.class)
abstract class NameTagTextCacheMixin {
    @WrapOperation(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"))
    private void vela$reuseLayout(Font font,Component text,float x,float y,int color,boolean shadow,Matrix4f pose,MultiBufferSource buffers,Font.DisplayMode mode,int background,int light,Operation<Void> original){
        NameTagTextCache.prepare(font,text,x,y,color,dev.sqwat.vela.render.entity.FriendNameTags.background(text,background))
            .visit(Font.GlyphVisitor.forMultiBufferSource(buffers,pose,mode,light));
    }
}
