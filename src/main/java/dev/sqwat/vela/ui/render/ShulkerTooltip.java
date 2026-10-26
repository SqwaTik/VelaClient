package dev.sqwat.vela.ui.render;

import dev.sqwat.vela.module.ShulkerPreview;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

public record ShulkerTooltip(ShulkerPreview.Contents contents) implements ClientTooltipComponent {
    @Override public int getWidth(Font font){return 166;}
    @Override public int getHeight(Font font){return 58;}
    @Override public void extractImage(Font font,int x,int y,int width,int height,GuiGraphicsExtractor g){
        var module=ShulkerPreview.current();
        if(module!=null)module.observe(contents,x,y,net.minecraft.client.Minecraft.getInstance().gui.screen());
        for(int i=0;i<Math.min(27,contents.items().size());i++){
            int sx=x+2+i%9*18,sy=y+2+i/9*18;
            VelaDraw.roundedRect(g,sx,sy,17,17,2,VelaTheme.SURFACE_DEEP);
            var item=contents.items().get(i);
            if(!item.isEmpty()){g.item(item,sx,sy);g.itemDecorations(font,item,sx,sy);}
        }
    }
}
