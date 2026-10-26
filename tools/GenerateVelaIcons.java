import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** Vela's shared 24-unit, rounded-stroke icon family. Generated before packaging. */
public class GenerateVelaIcons {
    static Graphics2D g;
    static void line(double... p){Path2D path=new Path2D.Double();path.moveTo(p[0],p[1]);for(int i=2;i<p.length;i+=2)path.lineTo(p[i],p[i+1]);g.draw(path);}
    static void circle(double x,double y,double r){g.draw(new Ellipse2D.Double(x-r,y-r,r*2,r*2));}
    static void box(double x,double y,double w,double h){g.draw(new RoundRectangle2D.Double(x,y,w,h,3,3));}
    static void person(double x,double y){circle(x,y,3);g.draw(new Arc2D.Double(x-5,y+4,10,9,0,180,Arc2D.OPEN));}
    static void star(boolean fill){Path2D p=new Path2D.Double();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5,r=i%2==0?9:4.1;double x=12+Math.cos(a)*r,y=12+Math.sin(a)*r;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}p.closePath();if(fill)g.fill(p);else g.draw(p);}
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);
        String[] names={"account","visuals","hud","utilities","markers","friends","configs","settings","search","arrow","back","x","plus","check","trash","copy","folder","key","star","star_filled","download","save","move","chevron","contact_options","compose","singleplayer","power","multiplayer","armor","potions","speakers","cooldowns","inventory","target"};
        if(args.length>1)names=new String[]{args[1]};
        for(String name:names){
            BufferedImage master=new BufferedImage(192,192,BufferedImage.TYPE_INT_ARGB);g=master.createGraphics();GeneratePngAssets.hints(g);g.scale(8,8);g.setColor(Color.WHITE);g.setStroke(new BasicStroke(1.7f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            switch(name){
                case "heart" -> {Path2D p=new Path2D.Double();p.moveTo(12,21);p.curveTo(9,18,2,13,2,8);p.curveTo(2,2,9,1,12,6);p.curveTo(15,1,22,2,22,8);p.curveTo(22,13,15,18,12,21);g.draw(p);}
                case "multiplayer" -> {circle(12,12,9);g.draw(new Ellipse2D.Double(8,3,8,18));line(3,12,21,12);}
                case "armor" -> line(8,3,6,4,2,9,6,12,7,10,7,21,17,21,17,10,18,12,22,9,18,4,16,3,14,6,10,6,8,3);
                case "potions" -> {line(9,3,15,3);line(10,3,10,9,5,17,6,21,18,21,19,17,14,9,14,3);line(8,15,16,15);}
                case "speakers" -> {box(9,2,6,13);g.draw(new Arc2D.Double(5,7,14,12,180,180,Arc2D.OPEN));line(12,19,12,22);line(8,22,16,22);}
                case "cooldowns" -> {circle(12,13,8);line(9,2,15,2);line(12,5,12,2);line(12,8,12,13,16,13);}
                case "inventory" -> {box(3,3,18,18);line(9,3,9,21);line(15,3,15,21);line(3,9,21,9);line(3,15,21,15);}
                case "target" -> {circle(12,12,7);circle(12,12,2);line(12,2,12,6);line(12,18,12,22);line(2,12,6,12);line(18,12,22,12);}
                case "skull" -> {g.draw(new Arc2D.Double(4,2,16,16,0,180,Arc2D.OPEN));line(4,10,4,15,7,17,7,21,17,21,17,17,20,15,20,10);circle(8,11,1.5);circle(16,11,1.5);line(11,15,12,13,13,15);line(10,18,10,21);line(14,18,14,21);}
                case "attack" -> {line(5,20,18,7,21,3,17,4,4,17);line(3,13,11,21);line(3,21,6,18);}
                case "movement" -> {circle(15,4,2);line(4,10,9,7,14,10,19,11);line(14,10,11,16,17,20);line(11,16,5,21);}
                case "account" -> person(12,7);
                case "friends" -> {person(9,7);g.draw(new Arc2D.Double(14,4,6,6,-90,180,Arc2D.OPEN));g.draw(new Arc2D.Double(14,11,8,10,0,90,Arc2D.OPEN));}
                case "visuals" -> {line(2,12,6,7,12,5,18,7,22,12,18,17,12,19,6,17,2,12);circle(12,12,3);}
                case "hud" -> {box(3,4,18,14);line(8,21,16,21);line(12,18,12,21);}
                case "utilities" -> {line(4,19,13,10,16,12,21,7,20,3,16,7,13,4,9,9,11,12,3,20);}
                case "markers" -> {circle(12,9,3);g.draw(new Arc2D.Double(5,2,14,14,0,180,Arc2D.OPEN));line(5,9,6,14,12,22,18,14,19,9);}
                case "configs","compose" -> {box(5,3,14,18);line(9,8,15,8);line(9,12,15,12);if(name.equals("compose")){line(10,17,14,17);line(12,15,12,19);}else line(9,16,13,16);}
                case "contact_options" -> {for(int i=0;i<3;i++){int x=i==1?8:16,y=6+i*6;line(3,y,x-2,y);line(x+2,y,21,y);circle(x,y,2);}}
                case "settings" -> {Path2D p=new Path2D.Double();for(int i=0;i<32;i++){double a=i*Math.PI/16,r=i%4<2?9:7;double x=12+Math.cos(a)*r,y=12+Math.sin(a)*r;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}p.closePath();g.draw(p);circle(12,12,3);}
                case "search" -> {circle(10,10,6);line(15,15,21,21);}
                case "arrow","near_arrow" -> {if(name.equals("near_arrow"))g.setStroke(new BasicStroke(2.55f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));line(3,12,21,12);line(15,6,21,12,15,18);}
                case "back" -> {line(21,12,3,12);line(9,6,3,12,9,18);}
                case "x" -> {g.setStroke(new BasicStroke(1.8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));line(6,6,18,18);line(18,6,6,18);}
                case "plus" -> {line(4,12,20,12);line(12,4,12,20);}
                case "check" -> line(4,12,9,17,20,6);
                case "trash" -> {line(4,6,20,6);line(9,3,15,3);line(6,6,7,21,17,21,18,6);line(10,10,10,17);line(14,10,14,17);}
                case "copy" -> {box(8,8,13,13);line(16,5,16,3,3,3,3,16,5,16);}
                case "folder" -> line(3,7,3,4,10,4,13,7,21,7,20,20,3,20,3,7);
                case "key" -> {circle(8,8,5);line(12,12,21,21);line(17,17,20,14);}
                case "star" -> star(false);
                case "star_filled" -> star(true);
                case "download" -> {line(12,3,12,15);line(7,10,12,15,17,10);line(4,16,4,21,20,21,20,16);}
                case "save" -> {line(3,3,18,3,21,6,21,21,3,21,3,3);box(7,3,9,6);box(7,14,10,7);}
                case "move" -> {line(3,12,21,12);line(12,3,12,21);line(6,9,3,12,6,15);line(18,9,21,12,18,15);line(9,6,12,3,15,6);line(9,18,12,21,15,18);}
                case "chevron" -> line(5,9,12,16,19,9);
                case "singleplayer" -> {line(12,2,21,7,21,17,12,22,3,17,3,7,12,2);line(3,7,12,12,21,7);line(12,12,12,22);}
                case "power" -> {g.draw(new Arc2D.Double(4,4,16,17,135,270,Arc2D.OPEN));line(12,2,12,11);}
            }
            g.dispose();GeneratePngAssets.write(master,root.resolve("icon_"+name+".png"));
            for(int s=8;s<=64;s++)GeneratePngAssets.write(GeneratePngAssets.resize(master,s,s,true),root.resolve("icon_"+name+"_"+s+".png"));
        }
    }
}
