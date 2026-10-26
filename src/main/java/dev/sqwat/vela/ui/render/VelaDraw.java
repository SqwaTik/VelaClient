package dev.sqwat.vela.ui.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VelaDraw {
    private static boolean subpixelText;
    public static boolean subpixelText(boolean value){boolean previous=subpixelText;subpixelText=value;return previous;}
    static boolean subpixelText(){return subpixelText;}
    public static int caretColor(){return (VelaTheme.TEXT_MUTED&0xFFFFFF)|((int)(90+55*Math.sin(System.nanoTime()/1.0e9*4))<<24);}
    public enum Weight { REGULAR, MEDIUM, SEMIBOLD }

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("velaclient", "textures/gui/logo.png");
    public static final float BODY = 26f / 3f;
    private static final FontDescription.Resource REGULAR = font("regular");
    private static final FontDescription.Resource MEDIUM = font("medium");
    private static final FontDescription.Resource SEMIBOLD = font("semibold");
    private static final Map<String, Component> TEXT_CACHE = new LinkedHashMap<>(256, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Component> eldest) { return size() > 512; }
    };

    private VelaDraw() {}

    public static void roundedRect(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int color) {
        PngChrome.draw(graphics,x,y,width,height,radius,color,false);
    }

    public static void roundedOutline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int color, int inside) {
        PngChrome.draw(graphics,x,y,width,height,radius,inside,false);
        PngChrome.draw(graphics,x,y,width,height,radius,color,true);
    }

    public static void text(GuiGraphicsExtractor graphics, Font font, String value, int x, int y, int color, Weight weight) {
        PngTextRenderer.draw(graphics,value,x,y,color,weight,BODY);
    }

    public static void centeredText(GuiGraphicsExtractor graphics, Font font, String value, int centerX, int y, int color, Weight weight) {
        PngTextRenderer.draw(graphics,value,centerX-PngTextRenderer.width(value,weight,BODY)/2,y,color,weight,BODY);
    }

    public static int textWidth(String value,Weight weight){return (int)Math.ceil(PngTextRenderer.width(value,weight,BODY));}
    public static float textWidth(String value,Weight weight,float size){return PngTextRenderer.width(value,weight,size);}
    public static void floatingText(GuiGraphicsExtractor g,String value,float center,float y,int color,float size){PngTextRenderer.floating(g,value,center,y,color,size);}
    public static void text(GuiGraphicsExtractor g,String value,float x,float y,int color,Weight weight,float size){PngTextRenderer.draw(g,value,x,y,color,weight,size);}
    public static void centeredText(GuiGraphicsExtractor g,String value,float x,float y,int color,Weight weight,float size){PngTextRenderer.draw(g,value,x-PngTextRenderer.width(value,weight,size)/2,y,color,weight,size);}
    public static void label(GuiGraphicsExtractor g,String value,Rect rect,int color,Weight weight){
        centeredText(g,value,rect.x()+rect.width()/2f,PngTextRenderer.centeredY(value,weight,BODY,rect.y(),rect.height()),color,weight,BODY);
    }
    public static String ellipsize(String value,float width,Weight weight){
        return ellipsize(value,width,weight,BODY);
    }
    public static String ellipsize(String value,float width,Weight weight,float size){
        value=dev.sqwat.vela.module.StreamerMode.display(value);
        if(PngTextRenderer.width(value,weight,size)<=width)return value;
        if(PngTextRenderer.width("…",weight,size)>width)return "";
        int end=value.length();while(end>0&&PngTextRenderer.width(value.substring(0,end)+"…",weight,size)>width)end=value.offsetByCodePoints(end,-1);
        return value.substring(0,end)+"…";
    }
    public static float pixelDensity(GuiGraphicsExtractor g){
        // Math.hypot is a slow non-intrinsic call; unrotated poses (almost every draw) need only |m00|, which hypot returns exactly.
        float m00=g.pose().m00(),m01=g.pose().m01();
        float scale=m01==0?Math.abs(m00):(float)Math.hypot(m00,m01);
        return Math.max(.01f,scale*net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale());
    }
    /** Same vertices, UVs and pipeline as a vanilla GUI_TEXTURED blit, without a pose push/scale/pop per quad. */
    public static void blit(GuiGraphicsExtractor g,Identifier texture,float x,float y,float width,float height,int u,int v,int sourceWidth,int sourceHeight,int textureWidth,int textureHeight,int color){
        if(width<=0||height<=0)return;
        quads(g,texture,new float[]{x,y,x+width,y+height,
            (float)u/textureWidth,(float)v/textureHeight,(float)(u+sourceWidth)/textureWidth,(float)(v+sourceHeight)/textureHeight},color);
    }
    /** Submits x0,y0,x1,y1,u0,v0,u1,v1 quads of one texture as a single ordered GUI element. The array is retained. */
    static void quads(GuiGraphicsExtractor g,Identifier texture,float[] quads,int color){
        var image=net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(texture);
        g.guiRenderState.addGuiElement(new PngTextRenderState(g.pose(),quads,
            net.minecraft.client.gui.render.TextureSetup.singleTexture(image.getTextureView(),image.getSampler()),color,g.scissorStack.peek()));
    }
    private static final Identifier CHROME=Identifier.fromNamespaceAndPath("velaclient","textures/gui/chrome.png");
    public static void hairline(GuiGraphicsExtractor g,float x,float y,float width,int color){
        blit(g,CHROME,x,y,width,1/pixelDensity(g),6,6,1,1,1024,1024,color);
    }

    public static Component component(String value, Weight weight) {
        String key = weight.name() + '\u0000' + value;
        synchronized (TEXT_CACHE) {
            return TEXT_CACHE.computeIfAbsent(key, ignored -> Component.literal(value).withStyle(style -> style.withFont(switch (weight) {
                case REGULAR -> REGULAR;
                case MEDIUM -> MEDIUM;
                case SEMIBOLD -> SEMIBOLD;
            })).withoutShadow());
        }
    }

    public static void logo(GuiGraphicsExtractor graphics, int x, int y, int size) {
        PngAssets.logo(graphics,x,y,size,0xFFFFFFFF);
    }

    public static void logo(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
        PngAssets.logo(graphics,x,y,size,color);
    }

    public static Identifier logoId() { return LOGO; }

    public static void toggle(GuiGraphicsExtractor graphics, int x, int y, boolean enabled, boolean hovered) {
        toggle(graphics,"position:"+x+":"+y,x,y,enabled,hovered);
    }
    private static final java.util.Map<Object,java.util.Map<String,ToggleMotion>> TOGGLES=new java.util.WeakHashMap<>();
    private static final class ToggleMotion {float value;long time;ToggleMotion(boolean enabled,long now){value=enabled?1:0;time=now;}}
    /** Use a stable key for controls inside moving panels. Boolean toggles animate by default. */
    public static void toggle(GuiGraphicsExtractor graphics,String key,int x,int y,boolean enabled,boolean hovered){
        Object screen=net.minecraft.client.Minecraft.getInstance().gui.screen();
        var states=TOGGLES.computeIfAbsent(screen,k->new java.util.LinkedHashMap<>());
        long now=System.nanoTime();
        if(states.size()>256)states.clear();
        var state=states.computeIfAbsent(key,k->new ToggleMotion(enabled,now));
        state.value=dev.sqwat.vela.ui.animation.Motion.approach(state.value,enabled?1:0,14,Math.min(.1f,(now-state.time)/1e9f));state.time=now;
        toggle(graphics,x,y,state.value,hovered);
    }
    public static void toggle(GuiGraphicsExtractor graphics,int x,int y,float progress,boolean hovered) {
        float p=Math.max(0,Math.min(1,progress));
        int track=VelaTheme.mix(hovered?VelaTheme.BORDER_HOVER:VelaTheme.BORDER,VelaTheme.PRIMARY,p);
        roundedRect(graphics, x, y, 19, 11, 6, track);
        int knobX = x+2+Math.round(8*p);
        roundedRect(graphics, knobX, y + 2, 7, 7, 4, 0xFFFFFFFF);
    }

    private static FontDescription.Resource font(String name) {
        return new FontDescription.Resource(Identifier.fromNamespaceAndPath("velaclient", name));
    }
}
