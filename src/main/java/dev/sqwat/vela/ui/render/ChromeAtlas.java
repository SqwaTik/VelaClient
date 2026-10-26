package dev.sqwat.vela.ui.render;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Pure nine-slice layout, shared by the GPU adapter and PNG visual tests. */
public final class ChromeAtlas {
    public interface Sink {void quad(float x,float y,float width,float height,int u,int v,int sourceWidth,int sourceHeight);}
    private record Sprite(int x,int y,int side){}
    private static final Sprite[][] SPRITES=load();
    private static Sprite[][] load(){
        Sprite[][] result=new Sprite[41][2];
        try(var reader=new BufferedReader(new InputStreamReader(ChromeAtlas.class.getResourceAsStream("/assets/velaclient/ui/chrome.csv"),StandardCharsets.UTF_8))){
            reader.readLine();for(String line;(line=reader.readLine())!=null;){String[] v=line.split(",");result[Integer.parseInt(v[0])][Integer.parseInt(v[1])]=new Sprite(Integer.parseInt(v[2]),Integer.parseInt(v[3]),Integer.parseInt(v[4]));}
        }catch(Exception e){throw new IllegalStateException("Missing PNG chrome atlas",e);}return result;
    }
    public static void draw(float x,float y,float width,float height,float radius,float density,boolean stroke,Sink sink){
        if(width<=0||height<=0||density<=0)return;
        // Keep opposite one-pixel edges on the same physical pixel phase.
        // Fractional GUI scales otherwise make one edge much fainter than the other.
        float right=Math.round((x+width)*density)/density;
        float bottom=Math.round((y+height)*density)/density;
        x=Math.round(x*density)/density;
        y=Math.round(y*density)/density;
        width=right-x;height=bottom-y;
        if(width<=0||height<=0)return;
        int pixels=Math.max(1,Math.min(40,Math.round(Math.min(radius,Math.min(width,height)/2)*density)));
        Sprite sprite=SPRITES[pixels][stroke?1:0];float guard=1/density,corner=(pixels+1)/density;
        float left=x-guard,top=y-guard,w=width+2*guard,h=height+2*guard;corner=Math.min(corner,Math.min(w,h)/2);
        int cap=pixels+1;
        for(int row=0;row<3;row++)for(int col=0;col<3;col++){
            if(stroke&&row==1&&col==1)continue;
            float dx=left+(col==0?0:col==1?corner:w-corner),dy=top+(row==0?0:row==1?corner:h-corner);
            float dw=col==1?w-2*corner:corner,dh=row==1?h-2*corner:corner;
            int u=sprite.x+(col==0?0:col==1?cap:sprite.side-cap),v=sprite.y+(row==0?0:row==1?cap:sprite.side-cap);
            int sw=col==1?sprite.side-2*cap:cap,sh=row==1?sprite.side-2*cap:cap;
            if(dw>0&&dh>0)sink.quad(dx,dy,dw,dh,u,v,sw,sh);
        }
    }
}
