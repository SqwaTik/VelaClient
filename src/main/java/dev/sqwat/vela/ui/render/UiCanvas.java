package dev.sqwat.vela.ui.render;

/** Small raster-only drawing contract, usable both in Minecraft and offline visual tests. */
public interface UiCanvas {
    enum Weight { REGULAR,MEDIUM,SEMIBOLD }
    void fill(float x,float y,float w,float h,int color);
    void round(float x,float y,float w,float h,float radius,int color);
    void border(float x,float y,float w,float h,float radius,int color);
    void image(String name,float x,float y,float size,int color);
    void texture(String name,Rect target,int sourceWidth,int sourceHeight,int color);
    void text(String value,float x,float y,float size,int color,Weight weight);
    float textWidth(String value,float size,Weight weight);
    float density();
    default void centered(String value,float x,float y,float size,int color,Weight weight){text(value,x-textWidth(value,size,weight)/2,y,size,color,weight);}
    default void line(float x,float y,float width,int color){fill(x,y,width,1/density(),color);}
}
