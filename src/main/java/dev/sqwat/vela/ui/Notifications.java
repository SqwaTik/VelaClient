package dev.sqwat.vela.ui;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Shared lifetime, layout, icons and motion for transient client messages. */
public final class Notifications {
    public static final long DURATION=3500;
    public static final long HUD_DURATION=6000;
    private record Notice(String title,String detail,String icon,int color,String action,Runnable run,BooleanSupplier valid,long started,int width){}
    private record Hit(Notice notice,Rect rect){}
    private static final Deque<Notice> notices=new ArrayDeque<>();
    public record HudValue(String icon,net.minecraft.world.item.ItemStack item,String value,float fraction){
        public HudValue{item=item.copy();fraction=Math.clamp(fraction,0,1);}
    }
    private static List<HudValue> hudValues=List.of();
    private static long hudStarted;
    private static final List<Hit> hits=new ArrayList<>();
    private static final UiControls controls=new UiControls();
    private static float scale=1;
    private Notifications(){}
    public static synchronized void showHudValues(List<HudValue> values){hudValues=List.copyOf(values);hudStarted=System.nanoTime()/1_000_000;}
    public static synchronized void clearHud(){hudValues=List.of();}
    public static synchronized boolean hudVisible(){
        long elapsed=System.nanoTime()/1_000_000-hudStarted;
        if(elapsed<0||elapsed>=HUD_DURATION)clearHud();return !hudValues.isEmpty();
    }
    private static int valueWidth(HudValue value){return Math.max(56,Math.round(VelaDraw.textWidth(value.value,VelaDraw.Weight.MEDIUM,9))+34);}
    public static synchronized int hudWidth(){
        if(!hudVisible())return 128;
        int width=0;for(var value:hudValues)width+=valueWidth(value);return width;
    }
    public static synchronized void drawHud(GuiGraphicsExtractor g){
        if(!hudVisible())return;int width=hudWidth();var c=new GameUiCanvas(g);
        c.round(0,0,width,32,7,VelaTheme.SURFACE);c.border(0,0,width,32,7,VelaTheme.BORDER);
        int x=0;
        for(var value:hudValues){int cell=valueWidth(value);
            if(value.item.isEmpty())c.image(value.icon,x+8,7,13,VelaTheme.DANGER);else g.item(value.item,x+6,5);
            c.text(value.value,x+27,9,9,VelaTheme.TEXT,UiCanvas.Weight.MEDIUM);
            c.round(x+8,25,cell-16,2,1,VelaTheme.BORDER);
            c.round(x+8,25,(cell-16)*value.fraction,2,1,VelaTheme.DANGER);x+=cell;
        }
    }
    public static boolean alive(long elapsed){return elapsed>=0&&elapsed<DURATION;}
    public static float reveal(long elapsed){float t=Math.clamp(Math.min(elapsed/150f,(DURATION-elapsed)/150f),0,1);return t*t*(3-2*t);}
    public static int height(boolean detail,boolean action){return NotificationChrome.height(detail,action);}
    public static void show(String title,String detail,String icon){show(title,detail,icon,VelaTheme.PRIMARY,"",null,()->true,icon.equals("configs")?0:160);}
    public static synchronized void show(String title,String detail,String icon,int color,String action,Runnable run,BooleanSupplier valid,int width){
        if(notices.size()==4)notices.removeFirst();notices.addLast(new Notice(title,detail,icon,color,action,run,valid,System.nanoTime()/1_000_000,width));
    }
    public static synchronized void draw(GuiGraphicsExtractor g){
        if(Minecraft.getInstance().gui.overlay()!=null){hits.clear();return;}
        hits.clear();long now=System.nanoTime()/1_000_000;notices.removeIf(n->!alive(now-n.started)||!n.valid.getAsBoolean());if(notices.isEmpty())return;
        var mc=Minecraft.getInstance();scale=1.5f/Math.max(1,mc.getWindow().getGuiScale());
        int width=Math.round(g.guiWidth()/scale),y=8;double mx=mc.mouseHandler.getScaledXPos(mc.getWindow())/scale,my=mc.mouseHandler.getScaledYPos(mc.getWindow())/scale;
        g.nextStratum();g.pose().pushMatrix();g.pose().identity();g.pose().scale(scale);
        try{int i=0;for(var n:notices){
            int detailInset=n.title.equals("Создана метка")?33:16;
            int desired=n.width==0?NotificationChrome.compactWidth(VelaDraw.textWidth(n.title,VelaDraw.Weight.MEDIUM,8),VelaDraw.textWidth(n.detail,VelaDraw.Weight.REGULAR,7)+detailInset-16):n.width;
            int w=Math.min(desired,width-16),h=height(!n.detail.isBlank(),n.run!=null),x=width-w-8+Math.round((1-reveal(now-n.started))*(w+10));
            NotificationChrome.draw(new GameUiCanvas(g),x,y,w,VelaDraw.ellipsize(n.title,w-34,VelaDraw.Weight.MEDIUM,8),VelaDraw.ellipsize(n.detail,w-detailInset,VelaDraw.Weight.REGULAR,7),n.icon,n.color,n.run!=null);
            if(n.run!=null){Rect action=NotificationChrome.action(x,y,w,!n.detail.isBlank());controls.button(g,"notice:"+i,action,n.action,mc.gui.screen()!=null&&action.contains(mx,my));hits.add(new Hit(n,action));}
            y+=h+6;i++;
        }}finally{g.pose().popMatrix();}
    }
    public static synchronized boolean click(double x,double y,int button){
        if(button!=0)return false;long now=System.nanoTime()/1_000_000;
        for(var hit:hits)if(hit.rect.contains(x/scale,y/scale)&&alive(now-hit.notice.started)&&hit.notice.valid.getAsBoolean()){
            hit.notice.run.run();notices.remove(hit.notice);hits.clear();return true;
        }return false;
    }
}
