package dev.sqwat.vela.ui.render;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class ReloadNotice {
    public static float progress;
    public static final int WIDTH=128,HEIGHT=24;
    private ReloadNotice(){}
    public static void icon(GuiGraphicsExtractor g,int x,int y,int color){
        VelaDraw.roundedOutline(g,x+2,y+1,7,8,4,color,0);
        VelaDraw.roundedRect(g,x+1,y+8,9,1,1,color);
        VelaDraw.roundedRect(g,x+4,y+10,3,1,1,color);
    }
    public static void draw(GuiGraphicsExtractor g){
        VelaDraw.roundedOutline(g,0,0,WIDTH,HEIGHT,6,VelaTheme.BORDER,VelaTheme.WINDOW);
        ProgressSpinner.draw(new GameUiCanvas(g),12,12,5,System.nanoTime());
        VelaDraw.text(g,"Загрузка РП",24,8,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,8);
        VelaDraw.text(g,Math.round(Math.clamp(progress,0,1)*100)+"%",101,9,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,7);
    }
}
