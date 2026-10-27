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
    /** {@code severity}: 0 at the warning threshold, 1 when nothing is left; drives colour and heartbeat. */
    public record HudValue(String icon,net.minecraft.world.item.ItemStack item,String value,float fraction,float severity){
        public HudValue{item=item.copy();fraction=Math.clamp(fraction,0,1);severity=Math.clamp(severity,0,1);}
        public HudValue(String icon,net.minecraft.world.item.ItemStack item,String value,float fraction){this(icon,item,value,fraction,1-fraction);}
    }
    private static List<HudValue> hudValues=List.of();
    private static long hudStarted,hudPulse;
    /** Per cell: shown bar, trailing "ghost" bar, time the value last dropped (ms). Animated by wall clock. */
    private static final Map<String,float[]> bars=new HashMap<>();
    /** Per cell: the previous text and when it changed (ms), so a new value rolls in over the old one. */
    private static final Map<String,String> lastText=new HashMap<>();
    private static final Map<String,Long> textChanged=new HashMap<>();
    private static final Map<String,String> outgoingText=new HashMap<>();
    private static long lastHudDraw;
    private static final int WARN=0xFFFFA24D;
    private static final List<Hit> hits=new ArrayList<>();
    private static final UiControls controls=new UiControls();
    private static float scale=1;
    private Notifications(){}
    public static synchronized void showHudValues(List<HudValue> values){
        long now=System.nanoTime()/1_000_000;
        if(hudValues.isEmpty()){hudStarted=now;bars.clear();}
        else hudStarted=now-240; // already open: stay fully in, just restart the lifetime
        hudValues=List.copyOf(values);hudPulse=now;
    }
    /** Live values for an open panel; keeps its lifetime, so the panel still closes on time. */
    public static synchronized void updateHudValues(List<HudValue> values){if(!hudValues.isEmpty()&&!values.isEmpty())hudValues=List.copyOf(values);}
    public static synchronized void clearHud(){hudValues=List.of();bars.clear();lastText.clear();textChanged.clear();outgoingText.clear();}
    public static synchronized boolean hudVisible(){
        long elapsed=System.nanoTime()/1_000_000-hudStarted;
        if(elapsed<0||elapsed>=HUD_DURATION)clearHud();return !hudValues.isEmpty();
    }
    private static int valueWidth(HudValue value){return Math.max(56,Math.round(VelaDraw.textWidth(value.value,VelaDraw.Weight.MEDIUM,9))+34);}
    public static synchronized int hudWidth(){
        if(!hudVisible())return 128;
        int width=0;for(var value:hudValues)width+=valueWidth(value);return width;
    }
    private static int alpha(int color,float a){return (Math.round(((color>>>24)&255)*Math.clamp(a,0,1))<<24)|(color&0xFFFFFF);}
    private static float smooth(float t){t=Math.clamp(t,0,1);return t*t*(3-2*t);}
    /** Pops in with a slight overshoot, flashes on every new warning, bars slide with a trailing ghost, fades out. */
    public static synchronized void drawHud(GuiGraphicsExtractor g){
        if(!hudVisible())return;int width=hudWidth();var c=new GameUiCanvas(g);
        long now=System.nanoTime()/1_000_000;float dt=lastHudDraw==0?0:Math.min(.1f,(now-lastHudDraw)/1000f);lastHudDraw=now;
        var config=dev.sqwat.vela.VelaClient.services()==null?null:dev.sqwat.vela.VelaClient.services().configs().current();
        boolean still=config!=null&&(config.reducedMotion||!config.animations);
        long age=now-hudStarted;
        float in=still?1:Math.clamp(age/240f,0,1),pop=still?1:1+(float)(Math.sin(in*Math.PI)*.06);
        float a=still?1:Math.min(smooth(age/180f),smooth((HUD_DURATION-age)/380f));
        float pulse=still?0:Math.clamp(1-(now-hudPulse)/650f,0,1);
        float worst=0;for(var value:hudValues)worst=Math.max(worst,value.severity);
        // A fresh warning shakes the panel (harder when worse); critical values keep it breathing.
        float since=(now-hudPulse)/1000f;
        float shake=still||since>.5f?0:(float)(Math.sin(since*48)*Math.exp(-since*9))*(3+5*worst);
        float breath=still?0:(float)(.5+.5*Math.sin(now/1000.0*Math.PI*2*(.6+1.4*worst)))*worst;
        g.pose().pushMatrix();
        g.pose().translate(width/2f+shake,16+(1-smooth(in))*-6);g.pose().scale(.94f+.06f*smooth(in)*pop);g.pose().translate(-width/2f,-16);
        try{
            int tone=VelaTheme.mix(WARN,VelaTheme.DANGER,worst);
            if(breath>.02f)for(int spread=4;spread>=1;spread--)c.round(-spread,-spread,width+spread*2,32+spread*2,7+spread,alpha(tone,a*breath*(5-spread)*.05f));
            c.round(0,0,width,32,7,alpha(VelaTheme.SURFACE,a));
            c.round(0,0,width,32,7,alpha(tone,a*(.05f+.18f*pulse+.08f*breath)));
            c.border(0,0,width,32,7,alpha(VelaTheme.mix(VelaTheme.BORDER,tone,.35f+.65f*pulse),a));
            if(pulse>0){float grow=(1-pulse)*5;c.border(-grow,-grow,width+grow*2,32+grow*2,7+grow,alpha(tone,a*pulse*.7f));}
            int x=0,index=0;
            for(var value:hudValues){
                int cell=valueWidth(value);int color=VelaTheme.mix(WARN,VelaTheme.DANGER,value.severity);
                float[] bar=bars.computeIfAbsent(value.icon+index,k->new float[]{value.fraction,value.fraction,0});
                if(value.fraction<bar[0]-.001f)bar[2]=now;
                bar[0]+=(value.fraction-bar[0])*(still?1:1-(float)Math.exp(-14*dt));
                if(bar[1]<bar[0]||still)bar[1]=bar[0];
                else if(now-bar[2]>350)bar[1]+=(bar[0]-bar[1])*(1-(float)Math.exp(-4*dt));
                // Heartbeat once the value is in the lower half of its warning range.
                float beat=still||value.severity<.5f?0:(float)Math.pow(Math.max(0,Math.sin(now/1000.0*Math.PI*2*(1+value.severity))),6)*value.severity;
                float bounce=1+.18f*pulse+.12f*beat;
                g.pose().pushMatrix();g.pose().translate(x+14.5f,13.5f);g.pose().scale(bounce);g.pose().translate(-(x+14.5f),-13.5f);
                if(value.item.isEmpty())c.image(value.icon,x+8,7,13,alpha(color,a));else if(a>.5f)g.item(value.item,x+6,5);
                g.pose().popMatrix();
                // Ring bursting out of the icon on a new warning.
                if(pulse>0){float ring=(1-pulse)*12;c.border(x+14.5f-6-ring,13.5f-6-ring,12+ring*2,12+ring*2,6+ring,alpha(color,a*pulse*.8f));}
                // The number rolls: the old value slides down and fades while the new one drops in from above.
                String key=value.icon+index,previous=lastText.get(key);
                if(previous==null){lastText.put(key,value.value);previous=value.value;}
                else if(!previous.equals(value.value)){outgoingText.put(key,previous);textChanged.put(key,now);lastText.put(key,value.value);}
                float roll=still?1:smooth((now-textChanged.getOrDefault(key,0L))/260f);
                int textColor=VelaTheme.mix(VelaTheme.TEXT,color,.35f*beat+.25f*pulse);
                String outgoing=outgoingText.get(key);
                if(roll<1&&outgoing!=null)c.text(outgoing,x+27,9+roll*7,9,alpha(textColor,a*(1-roll)),UiCanvas.Weight.MEDIUM);
                c.text(value.value,x+27,9-(1-roll)*7,9,alpha(textColor,a*roll),UiCanvas.Weight.MEDIUM);
                float track=cell-16;
                c.round(x+8,25,track,2,1,alpha(VelaTheme.BORDER,a));
                if(bar[1]>bar[0])c.round(x+8,25,track*bar[1],2,1,alpha(0xFFFFFFFF,a*.45f));
                c.round(x+8,25,track*bar[0],2,1,alpha(color,a));
                x+=cell;index++;
            }
        }finally{g.pose().popMatrix();}
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
            int detailInset=16;
            int desired=n.width==0?NotificationChrome.compactWidth(VelaDraw.textWidth(n.title,VelaDraw.Weight.MEDIUM,8),VelaDraw.textWidth(n.detail,VelaDraw.Weight.REGULAR,7)+detailInset-16):n.width;
            int w=Math.min(desired,width-16),h=height(!n.detail.isBlank(),n.run!=null),x=width-w-8+Math.round((1-reveal(now-n.started))*(w+10));
            NotificationChrome.draw(new GameUiCanvas(g),x,y,w,VelaDraw.ellipsize(n.title,w-34,VelaDraw.Weight.MEDIUM,8),VelaDraw.ellipsize(n.detail,w-detailInset,VelaDraw.Weight.REGULAR,7),n.icon,n.color,n.run!=null);
            if(n.icon.startsWith("item:")){g.pose().pushMatrix();g.pose().translate(x+6,y+6);g.pose().scale(1);g.item(dev.sqwat.vela.marker.MarkerRuntime.icon(n.icon.substring(5)),0,0);g.pose().popMatrix();}
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
