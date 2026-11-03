package dev.sqwat.vela.ui.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * An inward (concave) shoulder: the area between two straight edges and a quarter superellipse, as where a notch
 * hanging from the screen's top edge flows into it. The superellipse meets both edges without a jump in curvature, so
 * the edge runs smoothly into the notch's side instead of ending in a small round hook. Drawn from a baked, supersampled mask with smooth filtering, so the curve stays
 * clean at any size and GUI scale instead of stepping in rows.
 */
public final class ConcaveCorner {
    private static final int SIZE=96;
    /** Exponent of the curve: 2 is a circle, higher meets the edges more gently. */
    private static final float POWER=2.6f;
    private static Identifier left,right;

    private ConcaveCorner(){}

    /**
     * Fills a {@code width}×{@code height} shoulder whose top edge is at {@code y} and whose vertical edge is at
     * {@code x}: {@code toLeft} grows it to the left of {@code x} (the left side of a notch), else to the right.
     */
    public static void draw(GuiGraphicsExtractor g,float x,float y,float width,float height,int color,boolean toLeft){
        if(width<.25f||height<.25f)return;
        var id=texture(toLeft);
        VelaDraw.blit(g,id,toLeft?x-width:x,y,width,height,0,0,SIZE,SIZE,SIZE,SIZE,color);
    }

    private static Identifier texture(boolean toLeft){
        if(toLeft&&left!=null)return left;
        if(!toLeft&&right!=null)return right;
        var id=Identifier.fromNamespaceAndPath("velaclient","notch_corner_"+(toLeft?"left":"right"));
        var image=mask(toLeft);
        Minecraft.getInstance().getTextureManager().register(id,new DynamicTexture(()->"Vela notch corner",image){
            {sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);}
        });
        if(toLeft)left=id;else right=id;
        return id;
    }
    /** White, with the alpha of how much of each pixel lies outside the curve that rounds the corner (4×4 samples). */
    static NativeImage mask(boolean toLeft){
        var image=new NativeImage(SIZE,SIZE,false);
        // The curve touches the top edge at the far side and the vertical edge at the bottom.
        float cx=toLeft?0:1,cy=1;
        for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){
            int hits=0;
            for(int sy=0;sy<4;sy++)for(int sx=0;sx<4;sx++){
                float u=(x+(sx+.5f)/4)/SIZE-cx,v=(y+(sy+.5f)/4)/SIZE-cy;
                if(Math.pow(Math.abs(u),POWER)+Math.pow(Math.abs(v),POWER)>1)hits++;
            }
            image.setPixel(x,y,Math.round(hits/16f*255)<<24|0xFFFFFF);
        }
        return image;
    }
}
