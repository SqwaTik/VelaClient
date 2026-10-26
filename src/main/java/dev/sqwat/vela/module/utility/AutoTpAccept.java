package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import java.util.regex.Pattern;

public final class AutoTpAccept extends ClientModule {
    private final BooleanSetting friendsOnly=setting(new BooleanSetting("friends_only","Только от друзей","Принимать запросы из списка друзей",true));
    private final TeleportAcceptPolicy policy=new TeleportAcceptPolicy();
    private Object level;
    private static final Pattern COMBAT=Pattern.compile("(?iu)^(?:.*(?:режим боя|до конца боя|combat|вы в бою)).*?(\\d{1,3})\\s*(?:сек|с\\b|sec|s\\b).*$");
    public AutoTpAccept(){super("auto_tpaccept","Auto TPAccept","Приём запросов телепортации вне боя",ModuleCategory.UTILITIES);}
    private static long now(){return System.nanoTime()/1_000_000;}
    public static AutoTpAccept instance(){var s=dev.sqwat.vela.VelaClient.services();return s!=null&&s.modules().byId("auto_tpaccept") instanceof AutoTpAccept m?m:null;}
    private void checkWorld(){var world=Minecraft.getInstance().level;if(world!=level){level=world;policy.reset();}}
    public void combat(){checkWorld();policy.combat(now(),30_000);}
    public void message(String text,boolean overlay){
        checkWorld();if(level==null)return;
        String plain=text.replaceAll("§[0-9a-fk-orA-FK-OR]","").strip();
        var timer=COMBAT.matcher(plain);
        if(overlay&&timer.matches())policy.combat(now(),Long.parseLong(timer.group(1))*1000+500);
        if(!enabled()||overlay)return;
        String name=TeleportAcceptPolicy.requester(plain);var c=Minecraft.getInstance();
        if(name==null||c.getConnection()==null||c.getConnection().getPlayerInfo(name)==null)return;
        policy.request(name,!friendsOnly.value()||dev.sqwat.vela.VelaClient.services().friends().contains(name),now());
    }
    @Override protected void onDisable(){policy.cancelRequest();}
    @Override public void onClientTick(){
        checkWorld();var c=Minecraft.getInstance();
        if(c.player==null||c.getConnection()==null||!c.player.isAlive()||c.player.isSpectator()){policy.cancelRequest();return;}
        String name=policy.take(now(),false);
        if(name!=null&&c.getConnection().getPlayerInfo(name)!=null&&(!friendsOnly.value()||dev.sqwat.vela.VelaClient.services().friends().contains(name)))c.getConnection().sendCommand("tpaccept "+name);
    }
    public static void damage(net.minecraft.network.protocol.game.ClientboundDamageEventPacket packet){
        var c=Minecraft.getInstance();var m=instance();if(m==null||c.player==null||c.level==null)return;
        var victim=c.level.getEntity(packet.entityId());var cause=c.level.getEntity(packet.sourceCauseId());
        if(victim instanceof Player&&cause instanceof Player&&victim!=cause&&(victim==c.player||cause==c.player))m.combat();
    }
}
