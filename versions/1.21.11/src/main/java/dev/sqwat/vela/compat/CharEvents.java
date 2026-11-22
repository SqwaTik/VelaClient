package dev.sqwat.vela.compat;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Fabric API 26.x's ScreenKeyboardEvents.allowCharType, which 1.21.11's Fabric API lacks (CharTypedMixin asks it). Like Fabric's screen events, a screen's listeners reset when it is initialized again. */
public final class CharEvents {
    public interface AllowCharType {boolean allowCharType(Screen screen,CharacterEvent event);}
    public interface Registrar {void register(AllowCharType listener);}
    private static final Map<Screen,List<AllowCharType>> LISTENERS=new WeakHashMap<>();
    static{ScreenEvents.BEFORE_INIT.register((client,screen,width,height)->LISTENERS.remove(screen));}
    private CharEvents(){}
    public static Registrar allowCharType(Screen screen){return listener->LISTENERS.computeIfAbsent(screen,s->new ArrayList<>()).add(listener);}
    public static boolean allow(Screen screen,CharacterEvent event){
        var list=LISTENERS.get(screen);
        if(list!=null)for(var listener:List.copyOf(list))if(!listener.allowCharType(screen,event))return false;
        return true;
    }
}
