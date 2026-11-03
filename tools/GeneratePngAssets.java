import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Offline supersampling and area prefiltering. Minecraft only reads the resulting PNGs. */
public class GeneratePngAssets {
    static final int SS=4;
    static final int[] IMAGE_SIZES={16,20,24,32,48,64,96,128,256,512,1024};
    static Path root,gui,metrics;
    public static void main(String[] args)throws Exception {
        root=Path.of(args[0]);gui=root.resolve("textures/gui");metrics=root.resolve("ui");
        Files.createDirectories(gui);Files.createDirectories(metrics);
        for(String weight:List.of("regular","medium","semibold")) {
            Font source=Font.createFont(Font.TRUETYPE_FONT,root.resolve("font/inter_"+weight+".ttf").toFile());
            exportKerning(source,weight);
        }
        exportChrome();exportWheel();
        for(String name:List.of("visuals","hud","utilities","markers","friends","configs","settings","search","arrow","back","x","plus","check","trash","copy","folder","key","star")) {
            BufferedImage original=ImageIO.read(gui.resolve("icon_"+name+".png").toFile());
            for(int size:IMAGE_SIZES)if(size<=64)write(resize(original,size,size,true),gui.resolve("icon_"+name+"_"+size+".png"));
        }
        BufferedImage logo=ImageIO.read(gui.resolve("logo.png").toFile());
        for(int size:IMAGE_SIZES)if(size>=24)write(resize(logo,size,size,false),gui.resolve("logo_"+size+".png"));
        System.out.println("PNG export complete: kerning, 40 corner radii, prefiltered icons and logo. Glyph atlases are baked in game by PngFontBaker.");
    }
    static FontRenderContext frc(){return new FontRenderContext(null,true,true);}
    static void exportKerning(Font source,String weight)throws Exception {
        Font font=source.deriveFont(128f).deriveFont(Map.of(TextAttribute.KERNING,TextAttribute.KERNING_ON));
        int[] chars="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя0123456789.,:;!?-()«»".codePoints().toArray();
        Map<Integer,Float> widths=new HashMap<>();for(int cp:chars){var v=font.createGlyphVector(frc(),new String(Character.toChars(cp)));widths.put(cp,(float)v.getGlyphPosition(v.getNumGlyphs()).getX());}
        StringBuilder csv=new StringBuilder();
        for(int left:chars)for(int right:chars){
            char[] pair={(char)left,(char)right};var v=font.layoutGlyphVector(frc(),pair,0,2,Font.LAYOUT_LEFT_TO_RIGHT);
            float adjustment=((float)v.getGlyphPosition(v.getNumGlyphs()).getX()-widths.get(left)-widths.get(right))/128f;
            if(Math.abs(adjustment)>.0001)csv.append(String.format(Locale.ROOT,"%d,%d,%.6f%n",left,right,adjustment));
        }Files.writeString(metrics.resolve("kerning_"+weight+".csv"),csv);
    }
    static void exportChrome()throws Exception {
        BufferedImage atlas=new BufferedImage(1024,1024,BufferedImage.TYPE_INT_ARGB);Graphics2D target=atlas.createGraphics();
        StringBuilder csv=new StringBuilder("1024,1024\n");int x=2,y=2,row=0;
        for(int r=1;r<=40;r++)for(int stroke=0;stroke<=1;stroke++){
            int side=2*r+6;if(x+side+2>1024){x=2;y+=row+2;row=0;}
            BufferedImage high=new BufferedImage(side*SS,side*SS,BufferedImage.TYPE_INT_ARGB);Graphics2D g=high.createGraphics();hints(g);g.scale(SS,SS);g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Float(1,1,side-2,side-2,2*r,2*r));
            if(stroke==1){g.setComposite(AlphaComposite.Clear);g.fill(new RoundRectangle2D.Float(2,2,side-4,side-4,2*Math.max(0,r-1),2*Math.max(0,r-1)));}
            g.dispose();target.drawImage(resize(high,side,side,true),x,y,null);
            csv.append(r+","+stroke+","+x+","+y+","+side+"\n");x+=side+2;row=Math.max(row,side);
        }
        if(y+row>1024)throw new IllegalStateException("Chrome atlas overflow");target.dispose();whiteRgb(atlas);write(atlas,gui.resolve("chrome.png"));Files.writeString(metrics.resolve("chrome.csv"),csv);
    }
    static void exportWheel()throws Exception {
        BufferedImage image=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<256;y++)for(int x=0;x<256;x++){
            double dx=x-127.5,dy=y-127.5,r=Math.hypot(dx,dy)/124;
            int a=(int)Math.round(Math.max(0,Math.min(1,(1-r)*124))*255);
            int rgb=Color.HSBtoRGB((float)(Math.atan2(dy,dx)/(2*Math.PI)+1)%1,(float)Math.min(1,r),1);image.setRGB(x,y,a<<24|rgb&0xffffff);
        }write(image,gui.resolve("color_wheel_256.png"));
    }
    static void hints(Graphics2D g){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);}
    static BufferedImage resize(BufferedImage src,int width,int height,boolean white){
        // Area filtering in premultiplied alpha avoids black seams and missing thin strokes.
        BufferedImage dst=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);double sx=src.getWidth()/(double)width,sy=src.getHeight()/(double)height;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            double a=0,r=0,g=0,b=0,total=0;
            for(int yy=(int)(y*sy);yy<Math.ceil((y+1)*sy);yy++)for(int xx=(int)(x*sx);xx<Math.ceil((x+1)*sx);xx++){
                double area=(Math.min(xx+1,(x+1)*sx)-Math.max(xx,x*sx))*(Math.min(yy+1,(y+1)*sy)-Math.max(yy,y*sy));
                if(area<=0)continue;int p=src.getRGB(Math.min(xx,src.getWidth()-1),Math.min(yy,src.getHeight()-1));double alpha=(p>>>24)*area;
                total+=area;a+=alpha;r+=(p>>16&255)*alpha;g+=(p>>8&255)*alpha;b+=(p&255)*alpha;
            }
            int alpha=(int)Math.round(a/total);int rgb=white?0xffffff:a>0?((int)Math.round(r/a)<<16|(int)Math.round(g/a)<<8|(int)Math.round(b/a)):0;dst.setRGB(x,y,alpha<<24|rgb);
        }return dst;
    }
    static void whiteRgb(BufferedImage image){for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)image.setRGB(x,y,image.getRGB(x,y)|0xffffff);}
    static void write(BufferedImage image,Path path)throws Exception {ImageIO.write(image,"PNG",path.toFile());Files.writeString(Path.of(path+".mcmeta"),"{\"texture\":{\"blur\":true,\"clamp\":true}}\n");}
}
