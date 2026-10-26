package dev.sqwat.vela.marker;
import dev.sqwat.vela.ui.Notifications;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class MarkerNotices {
    private MarkerNotices(){}
    /** One title line (the marker itself, "Метка смерти" for death points) and a delete button; no "created" heading or divider. */
    public static void initialize(MarkerService markers){markers.onCreated(p->Notifications.show(title(p),"",p.icon(),p.color(),"Удалить",()->markers.remove(p.id()),()->markers.state().points.stream().anyMatch(point->point.id().equals(p.id())),160));}
    static String title(MarkerService.Point point){return point.death()?"Метка смерти":point.name();}
    public static boolean alive(long elapsed){return Notifications.alive(elapsed);}
    public static int reservedHeight(){return 0;}
    public static void draw(GuiGraphicsExtractor g){Notifications.draw(g);}
    public static boolean click(double x,double y,int button){return Notifications.click(x,y,button);}
}
