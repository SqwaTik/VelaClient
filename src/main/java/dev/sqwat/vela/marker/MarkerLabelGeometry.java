package dev.sqwat.vela.marker;
public final class MarkerLabelGeometry {
    private MarkerLabelGeometry(){}
    public static float screenX(float normalized,int width){return (normalized+1)*width*.5f;}
    public static float screenY(float normalized,int height){return (1-normalized)*height*.5f;}
    public static int titleWidth(float textWidth){return Math.max(36,(int)Math.ceil(textWidth)+21);}
    public static int distanceWidth(float textWidth){return Math.max(22,(int)Math.ceil(textWidth)+8);}
    public static boolean visible(float x,float y,float density,int width,int height){
        return x+61*density>=0&&x-61*density<=width&&y+15*density>=0&&y-37*density<=height;
    }
}
