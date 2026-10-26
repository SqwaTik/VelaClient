package dev.sqwat.vela.friend;
import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;
public final class FriendPresence {
    private static Object connection;
    private static final Map<String,Boolean> previous=new HashMap<>();
    private static int ticks;
    private FriendPresence(){}
    public static void tick(Minecraft mc,VelaServices services){
        Object next=mc.getConnection();boolean reset=connection!=next;connection=next;
        if(reset||next==null){previous.clear();}
        if(next==null||!reset&&++ticks%10!=0)return;
        Set<String> online=new HashSet<>();mc.getConnection().getOnlinePlayers().forEach(p->online.add(p.getProfile().name().toLowerCase(Locale.ROOT)));
        Set<String> friends=new HashSet<>();
        for(var friend:services.friends().all()){
            String key=friend.name().toLowerCase(Locale.ROOT);friends.add(key);boolean present=online.contains(key);
            Boolean old=previous.put(key,present);
            if(old!=null&&old!=present)dev.sqwat.vela.ui.Notifications.show(present?"Друг зашёл":"Друг вышел",friend.name(),"friends");
        }
        previous.keySet().retainAll(friends);
    }
    public static void draw(GuiGraphicsExtractor g){
    }
}
