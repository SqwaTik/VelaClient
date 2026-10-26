package dev.sqwat.vela.mixin;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.TextureContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.io.InputStream;
import java.io.IOException;
@Mixin(TextureContents.class)
public abstract class TextureDecodeMixin {
    @Redirect(method="load",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/NativeImage;read(Ljava/io/InputStream;)Lcom/mojang/blaze3d/platform/NativeImage;"))
    private static NativeImage vela$decode(InputStream input)throws IOException{
        if(Boolean.getBoolean("vela.disableTextureDecodeCache"))return NativeImage.read(input);
        byte[] prefix=input.readNBytes(8*1024*1024+1);
        if(prefix.length>8*1024*1024)return NativeImage.read(new java.io.SequenceInputStream(new java.io.ByteArrayInputStream(prefix),input));
        return dev.sqwat.vela.render.TextureDecodeCache.read(prefix);
    }
}
