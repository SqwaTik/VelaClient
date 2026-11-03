package dev.sqwat.vela.ui.hud;

import dev.sqwat.vela.ui.render.VelaDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The one background block every HUD panel uses, the same as the Music panel: dark fill and a soft outline. */
public final class HudBlock {
    public static final int FILL_RGB=0x10141C,BORDER_RGB=0x576578,FILL_ALPHA=184,BORDER_ALPHA=112;
    private HudBlock(){}
    /** Fill colour at {@code opacity} 0..1 of the panel's normal strength. */
    public static int fill(float opacity){return (Math.round(FILL_ALPHA*Math.clamp(opacity,0,1))<<24)|FILL_RGB;}
    public static int border(float opacity){return (Math.round(BORDER_ALPHA*Math.clamp(opacity,0,1))<<24)|BORDER_RGB;}
    public static void draw(GuiGraphicsExtractor g,int x,int y,int width,int height,int radius,float opacity){
        if(opacity<=0)return;
        VelaDraw.roundedRect(g,x,y,width,height,radius,fill(opacity));
        VelaDraw.roundedOutline(g,x,y,width,height,radius,border(opacity),0);
    }
}
