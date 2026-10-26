import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Rasterizes the CSS surfaces supplied in Prompt Submission.zip; no text or fake profile data. */
public class GenerateAccountSurfaces {
    public static void main(String[] args)throws Exception{
        Path out=Path.of(args[0]);
        for(boolean loading:new boolean[]{false,true}){
            var image=new BufferedImage(1024,640,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<640;y++)for(int x=0;x<1024;x++){
                double fx=x/1024.0,fy=y/640.0;
                double glow=loading?Math.exp(-((fx-.5)*(fx-.5)+(fy-.45)*(fy-.45))*24)*.14:
                    Math.exp(-((fx-.19)*(fx-.19)+(fy-.18)*(fy-.18))*17)*.075
                    +Math.exp(-((fx-.82)*(fx-.82)+(fy-.82)*(fy-.82))*17)*.055;
                double grid=(x%32==0||y%32==0)?Math.max(0,1-Math.hypot(fx-.5,fy-.5)*1.8)*.018:0;
                int r=(int)(7+(117-7)*glow+145*grid),g=(int)(9+(108-9)*glow+139*grid),b=(int)(14+(255-14)*glow+255*grid);
                image.setRGB(x,y,0xff000000|r<<16|g<<8|b);
            }
            ImageIO.write(image,"png",out.resolve(loading?"loading_background.png":"account_background.png").toFile());
        }
        var hero=new BufferedImage(1224,252,BufferedImage.TYPE_INT_ARGB);var g=hero.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
        var shape=new RoundRectangle2D.Float(1,1,1222,250,60,60);g.setClip(shape);
        g.setColor(new Color(18,23,33));g.fillRect(0,0,1224,252);
        g.setPaint(new GradientPaint(0,0,new Color(117,108,255,26),900,252,new Color(117,108,255,5)));
        g.fillRect(0,0,1224,252);g.setStroke(new BasicStroke(2));g.setColor(new Color(145,139,255,18));
        g.draw(new Ellipse2D.Float(-70,-150,420,420));g.draw(new Ellipse2D.Float(-128,-208,540,540));
        g.setClip(null);g.setColor(new Color(117,108,255,48));g.draw(shape);g.dispose();
        ImageIO.write(hero,"png",out.resolve("account_hero.png").toFile());
        var modal=new BufferedImage(882,1020,BufferedImage.TYPE_INT_ARGB);g=modal.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
        var panel=new RoundRectangle2D.Float(1,1,880,1018,68,68);g.setClip(panel);
        g.setPaint(new GradientPaint(0,0,new Color(19,24,35),882,1020,new Color(11,15,22)));g.fillRect(0,0,882,1020);
        g.setPaint(new RadialGradientPaint(441,-60,480,new float[]{0,1},new Color[]{new Color(117,108,255,25),new Color(117,108,255,0)}));g.fillRect(0,0,882,1020);
        g.setClip(null);g.setColor(new Color(48,58,77));g.setStroke(new BasicStroke(2));g.draw(panel);g.dispose();
        ImageIO.write(modal,"png",out.resolve("account_modal.png").toFile());
    }
}
