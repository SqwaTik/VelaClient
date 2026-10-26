package dev.sqwat.vela;
import dev.sqwat.vela.render.resource.TextureDecodeCache;
import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class TextureDecodeCacheTest {
    private byte[] png(int color)throws Exception{
        var image=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<256;y++)for(int x=0;x<256;x++)image.setRGB(x,y,color^(x<<8)^y);
        var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();
    }
    @Test void copiesAreIndependentAndChangedBytesInvalidate()throws Exception{
        TextureDecodeCache.close();
        try{
            byte[] first=png(0xFF123456),second=png(0xFF654321);
            int original;
            try(var a=TextureDecodeCache.read(first)){original=a.getPixel(0,0);a.setPixel(0,0,0);}
            try(var b=TextureDecodeCache.read(first)){assertEquals(original,b.getPixel(0,0));}
            try(var c=TextureDecodeCache.read(second)){assertNotEquals(original,c.getPixel(0,0));}
            assertTrue(TextureDecodeCache.statistics()[0]>=1);
            assertTrue(TextureDecodeCache.statistics()[2]<=64L*1024*1024);
        }finally{TextureDecodeCache.close();}
        assertEquals(0,TextureDecodeCache.statistics()[2]);
    }
    @Test void compareWarmDecodeWork()throws Exception{
        byte[] png=png(0xFF246880);
        for(int i=0;i<20;i++){try(var image=NativeImage.read(png)){}try(var image=TextureDecodeCache.read(png)){}}
        long start=System.nanoTime();for(int i=0;i<100;i++)try(var image=NativeImage.read(png)){}
        long nativeNs=System.nanoTime()-start;
        start=System.nanoTime();for(int i=0;i<100;i++)try(var image=TextureDecodeCache.read(png)){}
        long cachedNs=System.nanoTime()-start;
        System.out.println("PNG microbenchmark 100x256px: decode="+nativeNs/1e6+"ms cached="+cachedNs/1e6+"ms");
        TextureDecodeCache.close();
    }
}
