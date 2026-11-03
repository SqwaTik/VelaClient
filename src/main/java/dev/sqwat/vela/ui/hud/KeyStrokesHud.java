package dev.sqwat.vela.ui.hud;

import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Movement/click keys with eased press highlight and clicks-per-second. Layout is pure so bounds match drawing. */
public final class KeyStrokesHud {
    static final int KEY=22,GAP=2,PAD=6,SPACE=12,WIDTH=PAD*2+KEY*3+GAP*2;
    private final float[] press=new float[7];
    private final ClickCounter left=new ClickCounter(),right=new ClickCounter();
    private boolean leftDown,rightDown;
    private long last;

    public static int height(boolean mouse,boolean space){return PAD*2+KEY*2+GAP+(mouse?GAP+KEY:0)+(space?GAP+SPACE:0);}

    /** Samples click edges once per frame; called even when the panel is hidden by its reveal animation. */
    public void sample(Minecraft client){
        long now=System.nanoTime();
        boolean l=client.options.keyAttack.isDown(),r=client.options.keyUse.isDown();
        if(l&&!leftDown)left.click(now);
        if(r&&!rightDown)right.click(now);
        leftDown=l;rightDown=r;
    }

    public void draw(GuiGraphicsExtractor g,Minecraft client,boolean mouse,boolean space,boolean cps,boolean immediate){
        long now=System.nanoTime();float dt=last==0?0:Math.min(.1f,(now-last)/1e9f);last=now;
        var o=client.options;
        KeyMapping[] keys={o.keyUp,o.keyLeft,o.keyDown,o.keyRight,o.keyAttack,o.keyUse,o.keyJump};
        for(int i=0;i<keys.length;i++){
            float goal=keys[i].isDown()?1:0;
            press[i]=immediate?goal:dev.sqwat.vela.ui.animation.Motion.approach(press[i],goal,18,dt);
        }
        int row=PAD;
        key(g,PAD+KEY+GAP,row,KEY,KEY,label(o.keyUp),press[0],null);
        row+=KEY+GAP;
        key(g,PAD,row,KEY,KEY,label(o.keyLeft),press[1],null);
        key(g,PAD+KEY+GAP,row,KEY,KEY,label(o.keyDown),press[2],null);
        key(g,PAD+2*(KEY+GAP),row,KEY,KEY,label(o.keyRight),press[3],null);
        row+=KEY+GAP;
        if(mouse){
            int half=(KEY*3+GAP*2-GAP)/2;
            key(g,PAD,row,half,KEY,"ЛКМ",press[4],cps?left.perSecond(now)+" CPS":null);
            key(g,PAD+half+GAP,row,half,KEY,"ПКМ",press[5],cps?right.perSecond(now)+" CPS":null);
            row+=KEY+GAP;
        }
        if(space)key(g,PAD,row,KEY*3+GAP*2,SPACE,null,press[6],null);
    }
    private static void key(GuiGraphicsExtractor g,int x,int y,int w,int h,String label,float pressed,String caption){
        VelaDraw.roundedRect(g,x,y,w,h,4,VelaTheme.mix(VelaTheme.SURFACE_DEEP,VelaTheme.PRIMARY,pressed*.85f));
        int text=VelaTheme.mix(VelaTheme.TEXT_SECONDARY,0xFFFFFFFF,pressed);
        if(label==null){VelaDraw.roundedRect(g,x+w/2-12,y+h/2-1,24,2,1,text);return;}
        if(caption==null){VelaDraw.centeredText(g,label,x+w/2f,y+(h-8)/2f,text,VelaDraw.Weight.MEDIUM,8);return;}
        VelaDraw.centeredText(g,label,x+w/2f,y+3,text,VelaDraw.Weight.MEDIUM,7);
        VelaDraw.centeredText(g,caption,x+w/2f,y+12,VelaTheme.mix(VelaTheme.TEXT_MUTED,0xFFFFFFFF,pressed*.6f),VelaDraw.Weight.REGULAR,6);
    }
    /**
     * The bound key's short name, so rebound keys show what is actually pressed. Letter keys are named by their place on
     * the keyboard (W A S D), not by the active layout's letter there (Ц Ф Ы В).
     */
    private static String label(KeyMapping key){
        var bound=((dev.sqwat.vela.mixin.input.KeyMappingAccessor)key).vela$getKey();
        int code=bound.getValue();
        if(bound.getType()==com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM&&code>=org.lwjgl.glfw.GLFW.GLFW_KEY_A&&code<=org.lwjgl.glfw.GLFW.GLFW_KEY_Z)
            return String.valueOf((char)('A'+code-org.lwjgl.glfw.GLFW.GLFW_KEY_A));
        String name=key.getTranslatedKeyMessage().getString();
        return name.length()<=3?name.toUpperCase(java.util.Locale.ROOT):name.substring(0,1).toUpperCase(java.util.Locale.ROOT);
    }

    /** Clicks within the last second; fixed ring buffer, no allocation per click. */
    static final class ClickCounter {
        private final long[] times=new long[64];
        private int head,size;
        void click(long now){times[head]=now;head=(head+1)%times.length;if(size<times.length)size++;}
        int perSecond(long now){
            int count=0;
            for(int i=0;i<size;i++){long t=times[Math.floorMod(head-1-i,times.length)];if(now-t>1_000_000_000L)break;count++;}
            return count;
        }
    }
}
