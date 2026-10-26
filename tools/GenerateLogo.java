import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;

public final class GenerateLogo {
    public static void main(String[] args) throws Exception {
        BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setComposite(AlphaComposite.Src);
        g.setPaint(new GradientPaint(18, 108, new Color(108, 92, 255), 110, 18, new Color(63, 213, 255)));
        g.fillRoundRect(8, 8, 112, 112, 34, 34);

        Path2D.Float outer = new Path2D.Float();
        outer.moveTo(64, 21);
        outer.lineTo(76, 50);
        outer.lineTo(107, 64);
        outer.lineTo(76, 77);
        outer.lineTo(64, 107);
        outer.lineTo(52, 77);
        outer.lineTo(21, 64);
        outer.lineTo(52, 50);
        outer.closePath();
        g.setPaint(new GradientPaint(30, 30, new Color(255, 255, 255, 245), 97, 98, new Color(212, 231, 255, 210)));
        g.fill(outer);

        Path2D.Float cut = new Path2D.Float();
        cut.moveTo(64, 42);
        cut.lineTo(72, 56);
        cut.lineTo(87, 64);
        cut.lineTo(72, 72);
        cut.lineTo(64, 86);
        cut.lineTo(56, 72);
        cut.lineTo(41, 64);
        cut.lineTo(56, 56);
        cut.closePath();
        g.setComposite(AlphaComposite.Clear);
        g.fill(cut);
        g.dispose();

        File output = new File(args[0]);
        output.getParentFile().mkdirs();
        ImageIO.write(image, "PNG", output);
    }
}
