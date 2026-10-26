package dev.sqwat.vela.ui.render;

import dev.sqwat.vela.module.ShulkerPreview;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

/** {@code hovered} is the highlighted slot of a pinned preview, or -1. */
public record ShulkerTooltip(ShulkerPreview.Contents contents,int hovered) implements ClientTooltipComponent {
    public ShulkerTooltip(ShulkerPreview.Contents contents){this(contents,-1);}
    @Override public int getWidth(Font font){return 166;}
    @Override public int getHeight(Font font){return 58;}
    @Override public void extractImage(Font font,int x,int y,int width,int height,GuiGraphicsExtractor g){
        var module=ShulkerPreview.current();
        if(module!=null&&hovered<0)module.observe(contents,x,y,net.minecraft.client.Minecraft.getInstance().gui.screen());
        int slotColor=VelaTheme.SURFACE_DEEP;
        if(module!=null&&module.tinted())slotColor=(slotColor&0xFF000000)|(VelaTheme.mix(slotColor,0xFF000000|contents.color(),.22f)&0xFFFFFF);
        for(int i=0;i<Math.min(27,contents.items().size());i++){
            int sx=x+2+i%9*18,sy=y+2+i/9*18;
            VelaDraw.roundedRect(g,sx,sy,17,17,2,slotColor);
            if(i==hovered){VelaDraw.roundedRect(g,sx,sy,17,17,2,0x32FFFFFF);VelaDraw.roundedOutline(g,sx,sy,17,17,2,0x78FFFFFF,0);}
            var item=contents.items().get(i);
            if(!item.isEmpty()){g.item(item,sx,sy);g.itemDecorations(font,item,sx,sy);}
        }
    }
}
