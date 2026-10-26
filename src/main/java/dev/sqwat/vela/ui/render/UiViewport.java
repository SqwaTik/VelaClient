package dev.sqwat.vela.ui.render;

/** Physical-pixel sizing independent of Minecraft's GUI scale. */
public record UiViewport(float scale,int width,int height,float pixelsPerPoint) {
    public static UiViewport fit(int guiWidth,int guiHeight,int gameScale,float preference) {
        if(!Float.isFinite(preference))preference=1;
        float pixelWidth=guiWidth*Math.max(1,gameScale),pixelHeight=guiHeight*Math.max(1,gameScale);
        float desired=1.5f*Math.max(.75f,Math.min(1.25f,preference));
        float density=Math.max(.25f,Math.min(desired,Math.min(pixelWidth/610f,pixelHeight/424f)));
        float transform=density/Math.max(1,gameScale);
        return new UiViewport(transform,Math.round(guiWidth/transform),Math.round(guiHeight/transform),density);
    }
    public double local(double coordinate){return coordinate/scale;}
}
