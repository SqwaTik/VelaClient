import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Locale;

/** Build-time raster export; Minecraft only loads the resulting PNG files. */
public class GenerateUiAssets {
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args[0]);
        Path gui=root.resolve("textures/gui"), metrics=root.resolve("ui");
        Files.createDirectories(gui); Files.createDirectories(metrics);
        for(String weight:new String[]{"regular","medium","semibold"}) {
            Font font=Font.createFont(Font.TRUETYPE_FONT,root.resolve("font/inter_"+weight+".ttf").toFile()).deriveFont(36f);
            BufferedImage atlas=new BufferedImage(2048,1024,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=atlas.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setColor(Color.WHITE);g.setFont(font);
            StringBuilder csv=new StringBuilder("2048,1024,4,9\n");
            int x=4,y=4,row=0;
            String chars="";
            StringBuilder alphabet=new StringBuilder();
            for(int cp=32;cp<=126;cp++)alphabet.appendCodePoint(cp);
            for(int cp=160;cp<=383;cp++)alphabet.appendCodePoint(cp);
            for(int cp=1024;cp<=1279;cp++)alphabet.appendCodePoint(cp);
            alphabet.append("…—–·•↔↕↑↓←→✓✕×★☆▶◀▼▲●○°");
            for(int cp:alphabet.toString().codePoints().distinct().toArray()) {
                if(!font.canDisplay(cp))continue;
                GlyphVector gv=font.createGlyphVector(g.getFontRenderContext(),new String(Character.toChars(cp)));
                Rectangle b=gv.getPixelBounds(g.getFontRenderContext(),0,0);
                int w=Math.max(1,b.width),h=Math.max(1,b.height);
                if(x+w+4>2048){x=4;y+=row+8;row=0;}
                if(y+h+4>1024)throw new IllegalStateException("Atlas overflow");
                g.drawGlyphVector(gv,x-b.x,y-b.y);
                float advance=(float)gv.getGlyphPosition(gv.getNumGlyphs()).getX()/4;
                csv.append(String.format(Locale.ROOT,"%d,%d,%d,%d,%d,%.4f,%.4f,%.4f%n",cp,x,y,w,h,b.x/4f,7.5f+b.y/4f,advance));
                x+=w+8;row=Math.max(row,h);
            }
            g.dispose();write(atlas,gui.resolve("font_"+weight+".png"));
            Files.writeString(metrics.resolve("font_"+weight+".csv"),csv);
        }
        BufferedImage mask=new BufferedImage(132,132,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=mask.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);g.fillRoundRect(0,0,132,132,128,128);g.dispose();write(mask,gui.resolve("rounded.png"));
        BufferedImage wheel=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);
        for(int yy=0;yy<512;yy++)for(int xx=0;xx<512;xx++){
            double dx=xx-255.5,dy=yy-255.5,r=Math.hypot(dx,dy)/248;
            int alpha=(int)Math.round(Math.max(0,Math.min(1,(1-r)*248))*255);
            int rgb=Color.HSBtoRGB((float)(Math.atan2(dy,dx)/(2*Math.PI)+1)%1,(float)Math.min(1,r),1);
            wheel.setRGB(xx,yy,alpha<<24|rgb&0xffffff);
        }
        write(wheel,gui.resolve("color_wheel.png"));
    }
    static void write(BufferedImage image,Path path)throws Exception {
        ImageIO.write(image,"PNG",path.toFile());
        Files.writeString(Path.of(path+".mcmeta"),"{\"texture\":{\"blur\":true,\"clamp\":true}}\n");
    }
}
