package dev.sqwat.vela.ui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector2f;

/** Uses the active resource-pack model, framed around the creature's eye height. */
public final class MobPortrait {
    private MobPortrait() {}
    public static void draw(GuiGraphicsExtractor g,LivingEntity entity,int x,int y,int size) {
        // Picture-in-picture entities do not inherit the GUI pose in Minecraft 26.2.
        var top=g.pose().transformPosition(new Vector2f(x,y));
        var bottom=g.pose().transformPosition(new Vector2f(x+size,y+size));
        float scale=Math.abs(bottom.x-top.x)/size;
        float modelSize=Math.clamp(size*.7f/Math.max(.3f,entity.getBbWidth()),16,48);
        InventoryScreen.extractEntityInInventoryFollowsMouse(g,Math.round(top.x),Math.round(top.y),
            Math.round(bottom.x),Math.round(bottom.y),Math.max(1,Math.round(modelSize*scale)),
            entity.getEyeHeight()-entity.getBbHeight()/2f,(top.x+bottom.x)/2,(top.y+bottom.y)/2,entity);
    }
}
