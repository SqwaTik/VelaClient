package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.ui.render.Rect;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Private-message threads of the Chat module. Each tagged chat line belongs to the other player; while a thread tab is
 * selected in the chat screen only its lines are shown and plain messages are sent as private messages to that player.
 * Leaving the chat screen shows the whole chat again. Render thread only.
 */
public final class ChatThreads {
    private static final int MAX_TABS=6;
    private static final Map<Component,String> LINES=Collections.synchronizedMap(new WeakHashMap<>());
    /** Partner -> unread count, most recent last. */
    private static final LinkedHashMap<String,Integer> THREADS=new LinkedHashMap<>();
    private static String selected;
    private static final List<Map.Entry<Rect,String>> HITS=new ArrayList<>();
    private ChatThreads(){}

    public static void initialize(){
        ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
            if(!(screen instanceof ChatScreen))return;
            ScreenEvents.afterExtract(screen).register((s,g,mouseX,mouseY,partial)->draw(g,s.width,s.height,mouseX,mouseY));
            ScreenMouseEvents.allowMouseClick(screen).register((s,event)->!click(event.x(),event.y(),event.button()));
            ScreenEvents.remove(screen).register(s->select(null));
        });
    }

    /** Records which thread a finished chat line belongs to. */
    static void tag(Component line,String partner,boolean incoming){
        LINES.put(line,partner);
        THREADS.remove(partner);
        THREADS.put(partner,incoming&&!partner.equals(selected)?THREADS.getOrDefault(partner,0)+1:0);
        while(THREADS.size()>MAX_TABS)THREADS.remove(THREADS.keySet().iterator().next());
    }
    /** The partner of the open thread, or null when the whole chat is shown. */
    public static String selected(){return selected;}

    private static void select(String partner){
        selected=partner;
        var chat=Minecraft.getInstance().gui.hud.getChat();
        if(partner==null)chat.setVisibleMessageFilter(message->true);
        else{THREADS.computeIfPresent(partner,(k,v)->0);chat.setVisibleMessageFilter(message->partner.equals(LINES.get(message.content())));}
    }

    private static boolean visible(){return !THREADS.isEmpty()&&ChatTweaks.threadsEnabled();}
    private static void draw(GuiGraphicsExtractor g,int width,int height,int mouseX,int mouseY){
        HITS.clear();
        if(!visible())return;
        int x=2,y=height-14-16;
        x=tab(g,x,y,null,"Все",0,mouseX,mouseY);
        var names=new ArrayList<>(THREADS.keySet());Collections.reverse(names);
        for(String name:names)x=tab(g,x,y,name,name,THREADS.get(name),mouseX,mouseY);
    }
    private static int tab(GuiGraphicsExtractor g,int x,int y,String partner,String label,int unread,int mouseX,int mouseY){
        boolean active=partner==null?selected==null:partner.equals(selected);
        int textWidth=(int)Math.ceil(VelaDraw.textWidth(label,VelaDraw.Weight.MEDIUM,8f));
        String badge=unread>0?String.valueOf(Math.min(unread,99)):"";
        int badgeWidth=badge.isEmpty()?0:(int)Math.ceil(VelaDraw.textWidth(badge,VelaDraw.Weight.SEMIBOLD,7f))+8;
        var r=new Rect(x,y,textWidth+12+(badgeWidth>0?badgeWidth+4:0),14);
        boolean hover=r.contains(mouseX,mouseY);
        int fill=active?(VelaTheme.PRIMARY&0xFFFFFF)|0x48000000:hover?0xC0232B3D:0xB0141923;
        VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),5,fill);
        VelaDraw.text(g,label,r.x()+6,r.y()+3,active?VelaTheme.TEXT:VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.MEDIUM,8f);
        if(badgeWidth>0){
            int bx=r.x()+6+textWidth+4;
            VelaDraw.roundedRect(g,bx,r.y()+3,badgeWidth,8,4,VelaTheme.DANGER);
            VelaDraw.text(g,badge,bx+4,r.y()+3.5f,0xFFFFFFFF,VelaDraw.Weight.SEMIBOLD,7f);
        }
        HITS.add(Map.entry(r,partner==null?"":partner));
        return r.x()+r.width()+4;
    }
    /** Left click selects a thread, right click closes it. */
    private static boolean click(double x,double y,int button){
        for(var hit:HITS){
            if(!hit.getKey().contains(x,y))continue;
            String partner=hit.getValue().isEmpty()?null:hit.getValue();
            if(button==1&&partner!=null){THREADS.remove(partner);if(partner.equals(selected))select(null);}
            else select(partner);
            return true;
        }
        return false;
    }
}
