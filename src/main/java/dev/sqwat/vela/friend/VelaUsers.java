package dev.sqwat.vela.friend;

import com.google.gson.reflect.TypeToken;
import dev.sqwat.vela.core.JsonFileStore;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import java.nio.file.Path;
import java.util.*;

/**
 * Who else plays with Vela. Every Vela client sets the spare bit 0x80 of the skin-parts byte it sends with its
 * settings; the server copies that byte to every client that sees the player, unchanged. The bit has no meaning for
 * vanilla (the seven skin parts use 0x01-0x40). Players are recognised when they come into view and remembered on
 * disk, so the tab list keeps their badge after they walk away. Independent of modules: it always works.
 */
public final class VelaUsers {
    public static final int MARK=0x80;
    private static final int LIMIT=4096;
    private static final Set<UUID> KNOWN=Collections.synchronizedSet(new LinkedHashSet<>());
    private static JsonFileStore<List<String>> store;
    private static boolean dirty;
    private static int tick;
    private VelaUsers(){}

    public static void initialize(Path configRoot){
        store=new JsonFileStore<>(configRoot.resolve("vela-users.json"),new TypeToken<List<String>>(){}.getType(),ArrayList::new);
        for(String id:store.load())try{KNOWN.add(UUID.fromString(id));}catch(IllegalArgumentException ignored){}
    }
    public static boolean isVela(UUID id){
        var mc=Minecraft.getInstance();
        return id!=null&&(KNOWN.contains(id)||mc.player!=null&&id.equals(mc.player.getUUID()));
    }
    /** Twice a second: look at the players in view; save new ones a little later, off the hot path. */
    public static void tick(Minecraft client){
        if(client.level==null||++tick%10!=0)return;
        for(Player player:client.level.players()){
            if(player==client.player)continue;
            byte parts=player.getEntityData().get(dev.sqwat.vela.mixin.entity.AvatarAccessor.vela$customisation());
            if((parts&MARK)!=0&&KNOWN.add(player.getUUID())){
                dirty=true;
                synchronized(KNOWN){if(KNOWN.size()>LIMIT){var it=KNOWN.iterator();it.next();it.remove();}}
            }
        }
        if(dirty&&tick%200==0)save();
    }
    private static void save(){
        dirty=false;
        if(store==null)return;
        List<String> ids;synchronized(KNOWN){ids=KNOWN.stream().map(UUID::toString).toList();}
        try{store.save(new ArrayList<>(ids));}catch(java.io.IOException error){org.slf4j.LoggerFactory.getLogger("Vela").warn("Cannot save Vela users",error);}
    }
}
