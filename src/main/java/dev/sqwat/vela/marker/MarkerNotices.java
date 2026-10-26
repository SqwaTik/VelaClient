package dev.sqwat.vela.marker;
import dev.sqwat.vela.ui.Notifications;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class MarkerNotices {
    private MarkerNotices(){}
    public static void initialize(MarkerService markers){markers.onCreated(p->Notifications.show("Создана метка",p.name(),p.icon(),p.color(),"Удалить",()->markers.remove(p.id()),()->markers.state().points.stream().anyMatch(point->point.id().equals(p.id())),160));}
    public static boolean alive(long elapsed){return Notifications.alive(elapsed);}
    public static int reservedHeight(){return 0;}
    public static void draw(GuiGraphicsExtractor g){Notifications.draw(g);}
    public static boolean click(double x,double y,int button){return Notifications.click(x,y,button);}
}
