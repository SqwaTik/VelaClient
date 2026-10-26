package dev.sqwat.vela.ui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class UiClip {
    private UiClip(){}
    public static void enable(GuiGraphicsExtractor g,int left,int top,int right,int bottom){
        ClipBounds bounds=ClipBounds.transform(g.pose(),left,top,right,bottom);
        // The vanilla transformed scissor floors both origin and size separately.
        // Submit precomputed outward bounds without applying that transform twice.
        g.pose().pushMatrix();
        try{
            g.pose().identity();
            g.enableScissor(bounds.left(),bounds.top(),bounds.right(),bounds.bottom());
        }finally{g.pose().popMatrix();}
    }
}
