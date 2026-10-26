package dev.sqwat.vela.render;

import dev.sqwat.vela.ui.render.Rect;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/** Reference region around the neutral vanilla hand anchor, not an item silhouette. */
public final class HandOriginGuide {
    private HandOriginGuide(){}
    public static Rect bounds(Matrix4fc projection,boolean right,int width,int height){
        float minX=Float.POSITIVE_INFINITY,minY=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX;
        for(float x:new float[]{.36f,.76f})for(float y:new float[]{-.52f,-.08f}){
            var p=new Vector4f(right?x:-x,y,-.72f,1).mul(projection);
            if(p.w<=0||!Float.isFinite(p.w))return new Rect(0,0,0,0);
            float sx=(p.x/p.w+1)*width/2,sy=(1-p.y/p.w)*height/2;
            minX=Math.min(minX,sx);maxX=Math.max(maxX,sx);minY=Math.min(minY,sy);maxY=Math.max(maxY,sy);
        }
        int left=Math.clamp((int)Math.floor(minX),0,width),top=Math.clamp((int)Math.floor(minY),0,height);
        int rightEdge=Math.clamp((int)Math.ceil(maxX),0,width),bottom=Math.clamp((int)Math.ceil(maxY),0,height);
        return new Rect(left,top,rightEdge-left,bottom-top);
    }
}
