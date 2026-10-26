import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Build a display-sized atlas with transparent gutters from original artwork. */
public class BuildParticleAtlas {
    public static void main(String[] args)throws Exception{
        String[] names={"point","star","lighting","cross","crown","heart","line","rhombus","dollar","snowflake","triangle"};
        var images=new BufferedImage[names.length];int size=0;
        for(int i=0;i<names.length;i++){images[i]=ImageIO.read(Path.of(args[0],names[i]+".png").toFile());if(images[i].getWidth()!=images[i].getHeight())throw new IllegalStateException("Non-square source");size=Math.max(size,images[i].getWidth());}
        size=128;
        int cell=size+4;var atlas=new BufferedImage(cell*4,cell*3,BufferedImage.TYPE_INT_ARGB);
        var graphics=atlas.createGraphics();graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        for(int i=0;i<images.length;i++)graphics.drawImage(images[i],i%4*cell+2,i/4*cell+2,size,size,null);
        graphics.dispose();
        ImageIO.write(atlas,"png",Path.of(args[1]).toFile());System.out.println("Source="+size+" Cell="+cell+" Atlas="+atlas.getWidth()+"x"+atlas.getHeight());
    }
}
