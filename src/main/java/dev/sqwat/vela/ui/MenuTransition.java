package dev.sqwat.vela.ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
public final class MenuTransition {
    private static Screen screen;
    private static long started;
    private static float renderedScale=1;
    private MenuTransition(){}
    public static void start(Screen next){
        var services=dev.sqwat.vela.VelaClient.services();
        screen=next;started=System.nanoTime();renderedScale=1;
    }
    public static float frame(Screen next){
        return renderedScale=1;
    }
    public static double x(double x){return inverse(x,Minecraft.getInstance().getWindow().getGuiScaledWidth());}
    public static double y(double y){return inverse(y,Minecraft.getInstance().getWindow().getGuiScaledHeight());}
    private static double inverse(double p,int length){return (p-length/2.0)/renderedScale+length/2.0;}
    public static double delta(double value){return value/renderedScale;}
    public static MouseButtonEvent mouse(MouseButtonEvent event){return new MouseButtonEvent(x(event.x()),y(event.y()),event.buttonInfo());}
}
