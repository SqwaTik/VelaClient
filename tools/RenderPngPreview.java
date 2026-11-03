import dev.sqwat.vela.ui.render.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Offline renderer of production layout and PNG assets; this does not create a Minecraft instance. */
public class RenderPngPreview implements UiCanvas {
    static final int[] FONT_SIZES={8,9,10,11,12,13,14,15,16,18,19,20,22,24,28,32,36,40,48};
    static final int[] ICON_SIZES={16,20,24,32,48,64},LOGO_SIZES={24,32,48,64,96,128,256,512,1024};
    final Path assets;
    final BufferedImage output;
    final Graphics2D graphics;
    final float density;
    final Map<String,BufferedImage> images=new HashMap<>(),tinted=new HashMap<>();
    final Map<String,PngFontMetrics> fonts=new HashMap<>();
    int quads;
    RenderPngPreview(Path assets,int width,int height,float density){
        this.assets=assets;this.density=density;output=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);graphics=output.createGraphics();
        graphics.setColor(new Color(0x090C12));graphics.fillRect(0,0,width,height);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }
    public static void main(String[] args)throws Exception {
        Path assets=Path.of(args[0]),out=Path.of(args[1]);Files.createDirectories(out);
        for(float scale:new float[]{1.125f,1.5f,1.875f,2.25f}) {
            int width=Math.round(800*scale),height=Math.round(480*scale);
            var preview=new RenderPngPreview(assets,width,height,scale);
            var layout=DashboardChrome.layout(800,480);
            DashboardChrome.draw(preview,layout,0,"Рендеринг и визуальные эффекты","",false,new float[6],false,0xFF756CFF);
            DashboardChrome.empty(preview,layout.content(),"Пока нет модулей","Добавленные функции появятся в этом разделе");
            preview.save(out.resolve("dashboard-"+scale+".png"));
            var accounts=new RenderPngPreview(assets,width,height,scale);var al=AccountChrome.layout(800,480);
            AccountChrome.draw(accounts,al,"SqwaT09",0,"",false,0,0,0xFF756CFF);
            DashboardChrome.empty(accounts,al.list(),"Нет сохранённых аккаунтов","Добавьте аккаунт кнопкой ниже");
            // Avatar region intentionally blank: real player skins are fetched only by the game.
            accounts.save(out.resolve("accounts-"+scale+".png"));
        }
        var specimen=new RenderPngPreview(assets,1080,620,1.5f);
        float y=22;
        for(Weight weight:Weight.values())for(float size:new float[]{20f/3,8,26f/3,10,38f/3}){
            specimen.text("Vela / Visuals / Accounts · Шрифт, настройки, друзья · AV To УА",20,y,size,0xFFF3F5FA,weight);y+=24;
        }
        specimen.save(out.resolve("type-specimen.png"));
    }
    void save(Path path)throws Exception {graphics.dispose();ImageIO.write(output,"PNG",path.toFile());System.out.println(path.getFileName()+" | quads="+quads+" | loaded PNGs="+images.size());}
    BufferedImage image(String key){return images.computeIfAbsent(key,k->{try{return ImageIO.read(assets.resolve("textures/gui/"+k+".png").toFile());}catch(Exception e){throw new IllegalStateException(k,e);}});}
    BufferedImage tint(String key,int color){
        if(color==0xFFFFFFFF)return image(key);
        return tinted.computeIfAbsent(key+"#"+color,k->{
            BufferedImage src=image(key),dst=new BufferedImage(src.getWidth(),src.getHeight(),BufferedImage.TYPE_INT_ARGB);
            int ca=color>>>24,cr=color>>16&255,cg=color>>8&255,cb=color&255;
            for(int y=0;y<src.getHeight();y++)for(int x=0;x<src.getWidth();x++){
                int p=src.getRGB(x,y),a=(p>>>24)*ca/255,r=(p>>16&255)*cr/255,g=(p>>8&255)*cg/255,b=(p&255)*cb/255;
                dst.setRGB(x,y,a<<24|r<<16|g<<8|b);
            }return dst;
        });
    }
    void quad(String key,float x,float y,float w,float h,int u,int v,int sw,int sh,int color){
        if(w<=0||h<=0)return;quads++;
        BufferedImage cropped=tint(key,color).getSubimage(u,v,sw,sh);
        AffineTransform transform=AffineTransform.getTranslateInstance(x*density,y*density);transform.scale(w*density/sw,h*density/sh);
        graphics.drawImage(cropped,transform,null);
    }
    public void fill(float x,float y,float w,float h,int color){quad("chrome",x,y,w,h,6,6,1,1,color);}
    public void texture(String name,Rect r,int sw,int sh,int color){quad(name,r.x(),r.y(),r.width(),r.height(),0,0,sw,sh,color);}
    public void round(float x,float y,float w,float h,float radius,int color){ChromeAtlas.draw(x,y,w,h,radius,density,false,(dx,dy,dw,dh,u,v,sw,sh)->quad("chrome",dx,dy,dw,dh,u,v,sw,sh,color));}
    public void border(float x,float y,float w,float h,float radius,int color){ChromeAtlas.draw(x,y,w,h,radius,density,true,(dx,dy,dw,dh,u,v,sw,sh)->quad("chrome",dx,dy,dw,dh,u,v,sw,sh,color));}
    public void image(String name,float x,float y,float size,int color){
        int[] sizes=name.equals("logo")?LOGO_SIZES:ICON_SIZES;int selected=sizes[sizes.length-1];for(int candidate:sizes)if(candidate>=size*density-.1f){selected=candidate;break;}
        String key=(name.equals("logo")?"logo":"icon_"+name)+"_"+selected;quad(key,x,y,size,size,0,0,selected,selected,color);
    }
    PngFontMetrics font(Weight weight,int size){
        String key=weight.name().toLowerCase(Locale.ROOT)+"_"+size;
        return fonts.computeIfAbsent(key,k->{try{
            String name=weight.name().toLowerCase(Locale.ROOT);var baked=PngFontBaker.bake(PngFontBaker.font(name),size);
            var atlas=new BufferedImage(baked.width(),baked.height(),BufferedImage.TYPE_INT_ARGB);
            for(int i=0;i<baked.alpha().length;i++)atlas.setRGB(i%baked.width(),i/baked.width(),(baked.alpha()[i]&255)<<24|0xffffff);
            images.put("font_"+k,atlas);return baked.metrics(name);
        }catch(Exception e){throw new IllegalStateException(e);}});
    }
    public void text(String value,float x,float y,float em,int color,Weight weight){
        int size=FONT_SIZES[0];for(int candidate:FONT_SIZES)if(Math.abs(candidate-em*density)<Math.abs(size-em*density))size=candidate;
        var font=font(weight,size);float factor=em/size,baseline=Math.round(y*density+size*.82f);
        String key="font_"+weight.name().toLowerCase(Locale.ROOT)+"_"+size;
        for(var placed:font.line(value).glyphs()){
            int quarter=Math.round((x+placed.x()*factor)*density*4);var glyph=font.phase(placed.glyph(),Math.floorMod(quarter,4));
            quad(key,(Math.floorDiv(quarter,4)+glyph.bearingX())/density,(baseline+glyph.bearingY()-font.baseline)/density,glyph.width()/density,glyph.height()/density,glyph.x(),glyph.y(),glyph.width(),glyph.height(),color);
        }
    }
    public float textWidth(String value,float size,Weight weight){return font(weight,18).line(value).width()*size/18;}
    public float density(){return density;}
}
