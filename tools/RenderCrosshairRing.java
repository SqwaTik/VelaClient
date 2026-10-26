import dev.sqwat.vela.ui.render.*;
import java.awt.*;
import java.awt.geom.*;
import java.nio.file.*;

/** Offline inspection of the same emitted ring geometry at several swing phases and scales. */
public class RenderCrosshairRing {
    public static void main(String[] args)throws Exception{
        var c=new RenderPngPreview(Path.of(args[0]),900,400,1);
        c.graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        float[] sweeps={1,.75f,.5f,.25f,0};
        for(int row=0;row<3;row++)for(int i=0;i<sweeps.length;i++){
            float scale=row+1,x=90+i*175,y=65+row*115;
            c.text("Дуга "+Math.round(sweeps[i]*100)+"%",x-30,y+23*scale,8,VelaTheme.TEXT,UiCanvas.Weight.MEDIUM);
            var polygon=new Path2D.Float();int[] n={0},alpha={0},red={0},green={0},blue={0};
            CrosshairRing.emit((px,py,col)->{
                if(n[0]%4==0){polygon.reset();polygon.moveTo(px,py);}else polygon.lineTo(px,py);
                alpha[0]+=col>>>24;red[0]+=col>>16&255;green[0]+=col>>8&255;blue[0]+=col&255;
                if(++n[0]%4==0){polygon.closePath();c.graphics.setColor(new Color(red[0]/4,green[0]/4,blue[0]/4,alpha[0]/4));c.graphics.fill(polygon);alpha[0]=red[0]=green[0]=blue[0]=0;}
            },x,y,5*scale,1.6f*scale,sweeps[i],row==2?VelaTheme.DANGER:VelaTheme.PRIMARY,1);
        }
        c.save(Path.of(args[1]).resolve("crosshair-ring.png"));
    }
}
