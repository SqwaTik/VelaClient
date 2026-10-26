package dev.sqwat.vela.module.utility;
import java.util.regex.Pattern;

/** Requests received in combat are discarded rather than accepted later. */
public final class TeleportAcceptPolicy {
    private static final Pattern REQUEST=Pattern.compile("^(?:Игрок )?([A-Za-z0-9_]{3,16}) (?:has requested to teleport to you|просит телепортироваться к вам|запросил телепортацию к вам)[.!]?$",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
    private String pending;
    private long requestedAt,combatUntil,lastSent=Long.MIN_VALUE/2;
    public static String requester(String text){
        if(text==null||text.length()>300)return null;
        var m=REQUEST.matcher(text.replaceAll("§[0-9a-fk-orA-FK-OR]","").strip());
        return m.matches()?m.group(1):null;
    }
    public void combat(long now,long duration){combatUntil=Math.max(combatUntil,now+Math.clamp(duration,0,300_000));pending=null;}
    public void request(String name,boolean permitted,long now){
        if(!permitted||now<combatUntil||now-lastSent<5000||name==null)return;
        pending=name;requestedAt=now;
    }
    public String take(long now,boolean serverCombat){
        if(serverCombat||now<combatUntil||now-requestedAt>5000){pending=null;return null;}
        if(pending==null||now-requestedAt<250)return null;
        String result=pending;pending=null;lastSent=now;return result;
    }
    public void reset(){pending=null;combatUntil=requestedAt=0;lastSent=Long.MIN_VALUE/2;}
    public void cancelRequest(){pending=null;}
}
