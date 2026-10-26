import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Deterministic geometry assets; no source images or external artwork. */
public class GenerateVisualStrokes {
    public static void main(String[] args)throws Exception{
        Path dir=Path.of(args[0]);Files.createDirectories(dir);
        BufferedImage arrow=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=arrow.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);g.setStroke(new BasicStroke(12,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        Path2D path=new Path2D.Float();path.moveTo(20,12);path.lineTo(44,32);path.lineTo(20,52);g.draw(path);g.dispose();
        ImageIO.write(arrow,"png",dir.resolve("near_chevron.png").toFile());
        BufferedImage stroke=new BufferedImage(64,16,BufferedImage.TYPE_INT_ARGB);
        g=stroke.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255,255,255,48));g.fill(new RoundRectangle2D.Float(0,1,64,14,14,14));
        g.setColor(Color.WHITE);g.fill(new RoundRectangle2D.Float(0,4,64,8,8,8));g.dispose();
        ImageIO.write(stroke,"png",dir.resolve("trajectory_stroke.png").toFile());
    }
}
