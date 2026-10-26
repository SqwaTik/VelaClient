import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Build-time monochrome UI assets with prefiltered raster sizes. */
public class GenerateUiIcons {
 public static void main(String[] args)throws Exception{
  Path out=Path.of(args[0]);
  for(String name:new String[]{"account","settings","chevron","microsoft","star_filled","download","save","move"}){
   BufferedImage master=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);Graphics2D g=master.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.scale(4,4);g.setColor(Color.WHITE);g.setStroke(new BasicStroke(3.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
   if(name.equals("account")){g.draw(new Ellipse2D.Double(23,7,18,18));g.draw(new Arc2D.Double(12,29,40,36,0,180,Arc2D.OPEN));g.drawLine(12,47,52,47);}
   else if(name.equals("settings")){Path2D gear=new Path2D.Double();for(int i=0;i<32;i++){double a=i*Math.PI/16,r=(i%4==1||i%4==2)?25:19;double x=32+Math.cos(a)*r,y=32+Math.sin(a)*r;if(i==0)gear.moveTo(x,y);else gear.lineTo(x,y);}gear.closePath();g.draw(gear);g.draw(new Ellipse2D.Double(23,23,18,18));}
   else if(name.equals("move")){g.drawLine(10,32,54,32);g.drawLine(32,10,32,54);g.drawLine(10,32,18,24);g.drawLine(10,32,18,40);g.drawLine(54,32,46,24);g.drawLine(54,32,46,40);g.drawLine(32,10,24,18);g.drawLine(32,10,40,18);g.drawLine(32,54,24,46);g.drawLine(32,54,40,46);}
   else if(name.equals("download")){g.drawLine(32,9,32,40);g.drawLine(20,29,32,41);g.drawLine(32,41,44,29);g.drawLine(12,44,12,54);g.drawLine(12,54,52,54);g.drawLine(52,54,52,44);}
   else if(name.equals("save")){g.draw(new RoundRectangle2D.Double(10,9,44,46,4,4));g.draw(new Rectangle2D.Double(20,9,25,17));g.draw(new Rectangle2D.Double(20,37,24,18));}
   else if(name.equals("star_filled")){Path2D p=new Path2D.Double();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5,r=i%2==0?25:11;double x=32+Math.cos(a)*r,y=32+Math.sin(a)*r;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}p.closePath();g.fill(p);}
   else if(name.equals("chevron")){Path2D p=new Path2D.Double();p.moveTo(15,24);p.lineTo(32,41);p.lineTo(49,24);g.draw(p);}
   else {g.fillRect(9,9,21,21);g.fillRect(34,9,21,21);g.fillRect(9,34,21,21);g.fillRect(34,34,21,21);}g.dispose();
   for(int size:new int[]{16,20,24,32,48,64}){BufferedImage image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);Graphics2D q=image.createGraphics();q.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);q.drawImage(master,0,0,size,size,null);q.dispose();for(int y=0;y<size;y++)for(int x=0;x<size;x++)image.setRGB(x,y,image.getRGB(x,y)|0xFFFFFF);ImageIO.write(image,"PNG",out.resolve("icon_"+name+"_"+size+".png").toFile());if(size==64)ImageIO.write(image,"PNG",out.resolve("icon_"+name+".png").toFile());}
   for(int size:new int[]{16,20,24,32,48,64})Files.writeString(out.resolve("icon_"+name+"_"+size+".png.mcmeta"),"{\"texture\":{\"blur\":true,\"clamp\":true}}");
   Files.writeString(out.resolve("icon_"+name+".png.mcmeta"),"{\"texture\":{\"blur\":true,\"clamp\":true}}");
  }
 }
}
