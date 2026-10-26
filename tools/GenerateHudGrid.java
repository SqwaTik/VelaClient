import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;
/** Precomputed repeating grid: a handful of texture draws instead of thousands of rectangles. */
public final class GenerateHudGrid {
    public static void main(String[] args)throws Exception {
        var image=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<512;y++)for(int x=0;x<512;x++)image.setRGB(x,y,(x%8==0&&y%8==0)?0xFFFFFFFF:0x00FFFFFF);
        ImageIO.write(image,"PNG",Path.of(args[0]).toFile());
    }
}
