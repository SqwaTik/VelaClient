package dev.sqwat.vela.ui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Collects many rounded rectangles and text runs (same pose) and submits them as two GUI elements: chrome first,
 * then text. Use for lists with dozens of rows, where one element per row makes vanilla's GUI sort and overlap
 * tests dominate. Pixels match {@link VelaDraw#roundedRect} and {@link VelaDraw#text} for unrotated poses.
 */
public final class QuadBatch {
    private static final Identifier CHROME=Identifier.fromNamespaceAndPath("velaclient","textures/gui/chrome.png");
    private final Layer chrome=new Layer(),text=new Layer();
    private Identifier textAtlas;
    private GuiGraphicsExtractor graphics;

    private static final class Layer {
        float[] quads=new float[64*8];int[] colors=new int[64];int count;
        void add(float x0,float y0,float x1,float y1,float u0,float v0,float u1,float v1,int color){
            if(count==colors.length){quads=java.util.Arrays.copyOf(quads,quads.length*2);colors=java.util.Arrays.copyOf(colors,colors.length*2);}
            int i=count*8;quads[i]=x0;quads[i+1]=y0;quads[i+2]=x1;quads[i+3]=y1;quads[i+4]=u0;quads[i+5]=v0;quads[i+6]=u1;quads[i+7]=v1;
            colors[count++]=color;
        }
    }
    public QuadBatch begin(GuiGraphicsExtractor g){graphics=g;chrome.count=0;text.count=0;textAtlas=null;return this;}

    public void roundedRect(float x,float y,float width,float height,float radius,int color){
        if((color>>>24)==0)return;
        ChromeAtlas.draw(x,y,width,height,radius,VelaDraw.pixelDensity(graphics),false,
            (dx,dy,dw,dh,u,v,sw,sh)->chrome.add(dx,dy,dx+dw,dy+dh,u/1024f,v/1024f,(u+sw)/1024f,(v+sh)/1024f,color));
    }
    /** Single-line text. All text in one batch must use the same weight and size (one atlas). */
    public void text(String value,float x,float y,int color,VelaDraw.Weight weight,float em){
        if(value.isEmpty()||(color>>>24)==0)return;
        var placed=PngTextRenderer.layout(graphics,value,x,y,weight,em);
        if(textAtlas==null)textAtlas=placed.texture();
        else if(!textAtlas.equals(placed.texture()))throw new IllegalArgumentException("QuadBatch text must share one font atlas");
        float[] q=placed.layout().quads();
        for(int i=0;i<q.length;i+=8)text.add(q[i],q[i+1],q[i+2],q[i+3],q[i+4],q[i+5],q[i+6],q[i+7],color);
    }
    public void flush(){
        submit(chrome,CHROME);
        if(textAtlas!=null)submit(text,textAtlas);
        graphics=null;
    }
    private void submit(Layer layer,Identifier texture){
        if(layer.count==0)return;
        var image=net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(texture);
        // The state keeps exact-size copies: the scratch arrays are reused next frame.
        graphics.guiRenderState.addGuiElement(new ColoredQuadsRenderState(graphics.pose(),
            java.util.Arrays.copyOf(layer.quads,layer.count*8),java.util.Arrays.copyOf(layer.colors,layer.count),layer.count,
            net.minecraft.client.gui.render.TextureSetup.singleTexture(image.getTextureView(),image.getSampler()),graphics.scissorStack.peek()));
    }
}
