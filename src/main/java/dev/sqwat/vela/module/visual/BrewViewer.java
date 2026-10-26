package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.phys.BlockHitResult;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hologram card over a brewing stand you look at, from what you saw the last time you opened it (servers do not send
 * brewing-stand contents otherwise). The brew countdown keeps running from the captured game time.
 */
public final class BrewViewer extends ClientModule {
    private static final int BREW_TICKS=400,LIMIT=64;
    private record Snapshot(ItemStack[] bottles,ItemStack ingredient,int fuel,int brewTicks,long capturedAt){}
    private final Map<BlockPos,Snapshot> stands=new LinkedHashMap<>(16,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<BlockPos,Snapshot> e){return size()>LIMIT;}
    };
    private Object level;
    private BrewingStandMenu menu;
    private BlockPos menuPos;
    public BrewViewer(){super("brew_viewer","Brew Viewer","Голографическое превью варки при наводке на зельеварку",ModuleCategory.VISUALS);}
    @Override protected void onDisable(){stands.clear();level=null;menu=null;menuPos=null;}

    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.level==null){onDisable();return;}
        if(level!=mc.level){stands.clear();level=mc.level;}
        if(!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)||!(screen.getMenu() instanceof BrewingStandMenu brewing)){menu=null;menuPos=null;return;}
        if(menu!=brewing){
            // The screen opens right after the click: the crosshair still points at the stand.
            menu=brewing;menuPos=mc.hitResult instanceof BlockHitResult hit&&mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof BrewingStandBlock?hit.getBlockPos().immutable():null;
        }
        if(menuPos==null)return;
        var bottles=new ItemStack[3];for(int i=0;i<3;i++)bottles[i]=brewing.getSlot(i).getItem().copy();
        stands.put(menuPos,new Snapshot(bottles,brewing.getSlot(3).getItem().copy(),brewing.getFuel(),brewing.getBrewingTicks(),mc.level.getGameTime()));
    }

    /** World-anchored, so it is drawn with the live HUD effects. */
    public void draw(GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();
        if(!enabled()||mc.level==null||mc.player==null||mc.gui.screen()!=null||!(mc.hitResult instanceof BlockHitResult hit))return;
        var pos=hit.getBlockPos();
        if(!(mc.level.getBlockState(pos).getBlock() instanceof BrewingStandBlock)){return;}
        var shot=stands.get(pos);if(shot==null)return;
        var camera=mc.gameRenderer.mainCamera();var origin=camera.position();
        var v=new org.joml.Vector4f((float)(pos.getX()+.5-origin.x),(float)(pos.getY()+1.35-origin.y),(float)(pos.getZ()+.5-origin.z),1)
            .mul(camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f()));
        if(v.w<=0)return;
        float x=(v.x/v.w+1)*g.guiWidth()/2f,y=(1-v.y/v.w)*g.guiHeight()/2f;
        long elapsed=Math.max(0,mc.level.getGameTime()-shot.capturedAt);
        int remaining=shot.brewTicks>0?(int)Math.max(0,shot.brewTicks-elapsed):0;
        float density=1.5f/Math.max(1,mc.getWindow().getGuiScale())*2;
        int w=92,h=62;
        g.pose().pushMatrix();
        try{
            g.pose().translate(x,y);g.pose().scale(density);g.pose().translate(-w/2f,-h);
            VelaDraw.roundedOutline(g,0,0,w,h,6,(0x60<<24)|(VelaTheme.BORDER&0xFFFFFF),(0xD8<<24)|(VelaTheme.SURFACE&0xFFFFFF));
            slot(g,shot.ingredient,w/2-9,4);
            for(int i=0;i<3;i++)slot(g,shot.bottles[i],w/2-9+(i-1)*24,24);
            String status;float progress;
            if(shot.brewTicks<=0){status="Не варится";progress=0;}
            else if(remaining<=0){status="Готово";progress=1;}
            else{status="Варка "+(remaining+19)/20+" с";progress=1-remaining/(float)BREW_TICKS;}
            VelaDraw.roundedRect(g,6,h-9,w-12,3,1,VelaTheme.BORDER);
            if(progress>0)VelaDraw.roundedRect(g,6,h-9,Math.max(2,Math.round((w-12)*progress)),3,1,VelaTheme.PRIMARY);
            VelaDraw.text(g,status,6,h-19,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,7);
            String fuel="Топл. "+shot.fuel;
            VelaDraw.text(g,fuel,w-6-VelaDraw.textWidth(fuel,VelaDraw.Weight.REGULAR,7),h-19,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,7);
        }finally{g.pose().popMatrix();}
    }
    private static void slot(GuiGraphicsExtractor g,ItemStack stack,int x,int y){
        VelaDraw.roundedRect(g,x,y,18,18,4,VelaTheme.SURFACE_DEEP);
        if(!stack.isEmpty())g.item(stack,x+1,y+1);
    }
}
