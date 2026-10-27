package dev.sqwat.vela.ui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Storage marker: a round badge with the stored item inside an accent ring that pulses outwards, the name and
 * distance in a pill above it, and a stem down to a dot exactly on the container. Drawn around the projected anchor
 * (0,0) at a fixed physical density. Plain markers keep MarkerOverlayChrome.
 */
public final class StorageMarkerChrome {
    private StorageMarkerChrome(){}
    public static void draw(GuiGraphicsExtractor g,String name,String distance,ItemStack item,int color){
        int rgb=color&0xFFFFFF;
        float t=(System.currentTimeMillis()%1600)/1600f;
        // Stem and anchor dot on the block.
        g.fill(0,-22,1,-4,0xB0000000|rgb);
        VelaDraw.roundedRect(g,-4,-4,9,9,5,0x60000000|rgb);
        VelaDraw.roundedRect(g,-2,-2,5,5,3,0xFF000000|rgb);
        // Pulse ring expanding from the badge.
        int pr=Math.round(18+t*10);int pa=Math.round((1-t)*(1-t)*140);
        if(pa>2)new GameUiCanvas(g).border(-pr+.5f,-40-pr,pr*2,pr*2,pr,(pa<<24)|rgb);
        // Badge: soft halo, accent ring, dark disc, the item.
        VelaDraw.roundedRect(g,-22,-62,45,45,22,0x2A000000|rgb);
        VelaDraw.roundedOutline(g,-18,-58,37,37,18,0xFF000000|rgb,0xF0101420);
        g.pose().pushMatrix();g.pose().translate(.5f,-39.5f);g.pose().scale(1.35f);
        if(!item.isEmpty())g.item(item,-8,-8);
        g.pose().popMatrix();
        // Name and distance pill.
        float nameW=VelaDraw.textWidth(name,VelaDraw.Weight.SEMIBOLD,8.5f),distW=VelaDraw.textWidth(distance,VelaDraw.Weight.MEDIUM,7.5f);
        int w=Math.round(nameW+distW+24),x=-w/2;
        VelaDraw.roundedOutline(g,x,-84,w,18,9,0x80000000|rgb,0xE6101420);
        VelaDraw.text(g,name,x+9,-79.5f,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,8.5f);
        VelaDraw.text(g,distance,x+15+nameW,-79f,VelaTheme.mix(0xFFFFFFFF,0xFF000000|rgb,.45f),VelaDraw.Weight.MEDIUM,7.5f);
    }
}
