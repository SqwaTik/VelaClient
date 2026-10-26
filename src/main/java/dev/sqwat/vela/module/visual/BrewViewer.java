package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.KeySetting;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.phys.BlockHitResult;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Card over the brewing stand you look at: ingredient, three bottles, fuel item with its count and charge, brew
 * progress. Contents come from the last time you opened that stand (servers send nothing otherwise); a stand you have
 * not opened shows empty slots. Holding the pin key (Left Ctrl by default) pins the card and frees the mouse so
 * hovering a slot shows the normal Minecraft tooltip.
 */
public final class BrewViewer extends ClientModule {
    private static final int BREW_TICKS=400,LIMIT=64,FUEL_CHARGES=20;
    static final int W=148,H=36;
    record Snapshot(ItemStack[] bottles,ItemStack ingredient,ItemStack fuelItem,int fuel,int brewTicks,long capturedAt,boolean done){}
    private static final Snapshot EMPTY=new Snapshot(new ItemStack[]{ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY},ItemStack.EMPTY,ItemStack.EMPTY,0,0,0,false);
    private final KeySetting pinKey=setting(new KeySetting("pin_key","Закрепить",org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL));
    private final Map<BlockPos,Snapshot> stands=new LinkedHashMap<>(16,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<BlockPos,Snapshot> e){return size()>LIMIT;}
    };
    private Object level;
    private BrewingStandMenu menu;
    private BlockPos menuPos;
    public BrewViewer(){super("brew_viewer","Brew Viewer","Варка при наводке на зельеварку; Ctrl — закрепить",ModuleCategory.VISUALS);}
    @Override protected void onDisable(){stands.clear();level=null;menu=null;menuPos=null;}

    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.level==null){onDisable();return;}
        if(level!=mc.level){stands.clear();level=mc.level;}
        if(mc.gui.screen()==null){
            var pos=target(mc);
            if(pos!=null&&dev.sqwat.vela.ui.input.BindInput.down(mc.getWindow().handle(),pinKey.value()))mc.gui.setScreen(new PinScreen(this,pos));
        }
        if(!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)||!(screen.getMenu() instanceof BrewingStandMenu brewing)){menu=null;menuPos=null;return;}
        if(menu!=brewing){
            // The screen opens right after the click: the crosshair still points at the stand.
            menu=brewing;menuPos=target(mc);
        }
        if(menuPos==null)return;
        var bottles=new ItemStack[3];for(int i=0;i<3;i++)bottles[i]=brewing.getSlot(i).getItem().copy();
        stands.put(menuPos,new Snapshot(bottles,brewing.getSlot(3).getItem().copy(),brewing.getSlot(4).getItem().copy(),brewing.getFuel(),brewing.getBrewingTicks(),mc.level.getGameTime(),false));
    }
    private static BlockPos target(Minecraft mc){
        return mc.level!=null&&mc.hitResult instanceof BlockHitResult hit&&mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof BrewingStandBlock?hit.getBlockPos().immutable():null;
    }
    private Snapshot snapshot(BlockPos pos){
        var s=stands.get(pos);if(s==null)return EMPTY;
        var mc=Minecraft.getInstance();
        return mc.level==null?s:project(s,mc.level.getGameTime(),mc.level.potionBrewing());
    }
    /**
     * What the stand holds now, from the snapshot taken when it was last open: finished brews apply Minecraft's own
     * brewing rules (each bottle = mix(ingredient, bottle), ingredient shrinks by one), and the stand keeps brewing
     * while the ingredient still fits, spending a fuel charge per brew and a blaze powder per 20 charges. Without this
     * a finished stand still showed the water bottles from before the brew.
     */
    static Snapshot project(Snapshot s,long now,net.minecraft.world.item.alchemy.PotionBrewing brewing){
        if(s.brewTicks<=0)return s;
        long elapsed=Math.max(0,now-s.capturedAt);
        if(elapsed<s.brewTicks)return new Snapshot(s.bottles,s.ingredient,s.fuelItem,s.fuel,(int)(s.brewTicks-elapsed),now,false);
        var bottles=new ItemStack[3];for(int i=0;i<3;i++)bottles[i]=s.bottles[i].copy();
        var ingredient=s.ingredient.copy();var fuelItem=s.fuelItem.copy();int fuel=s.fuel;
        long left=elapsed-s.brewTicks;
        for(int guard=0;guard<64;guard++){
            for(int i=0;i<3;i++)if(!bottles[i].isEmpty())bottles[i]=brewing.mix(ingredient,bottles[i]);
            ingredient.shrink(1);
            if(!brewable(brewing,ingredient,bottles))return new Snapshot(bottles,ingredient,fuelItem,fuel,0,now,true);
            if(fuel<=0&&!fuelItem.isEmpty()){fuel=FUEL_CHARGES;fuelItem.shrink(1);}
            if(fuel<=0)return new Snapshot(bottles,ingredient,fuelItem,fuel,0,now,true);
            fuel--;
            if(left<BREW_TICKS)return new Snapshot(bottles,ingredient,fuelItem,fuel,(int)(BREW_TICKS-left),now,false);
            left-=BREW_TICKS;
        }
        return new Snapshot(bottles,ingredient,fuelItem,fuel,0,now,true);
    }
    private static boolean brewable(net.minecraft.world.item.alchemy.PotionBrewing brewing,ItemStack ingredient,ItemStack[] bottles){
        if(ingredient.isEmpty()||!brewing.isIngredient(ingredient))return false;
        for(var bottle:bottles)if(!bottle.isEmpty()&&brewing.hasMix(bottle,ingredient))return true;
        return false;
    }

    /** World-anchored card while aiming at a stand; drawn with the live HUD effects. */
    public void draw(GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();
        if(!enabled()||mc.player==null||mc.gui.screen()!=null)return;
        var pos=target(mc);if(pos==null)return;
        var camera=mc.gameRenderer.mainCamera();var origin=camera.position();
        var v=new org.joml.Vector4f((float)(pos.getX()+.5-origin.x),(float)(pos.getY()+1.3-origin.y),(float)(pos.getZ()+.5-origin.z),1)
            .mul(camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f()));
        if(v.w<=0)return;
        float x=(v.x/v.w+1)*g.guiWidth()/2f,y=(1-v.y/v.w)*g.guiHeight()/2f;
        float density=1.5f/Math.max(1,mc.getWindow().getGuiScale())*1.6f;
        g.pose().pushMatrix();
        try{g.pose().translate(x,y);g.pose().scale(density);g.pose().translate(-W/2f,-H);card(g,snapshot(pos),Float.NaN,Float.NaN);}
        finally{g.pose().popMatrix();}
    }

    /**
     * Draws the card at the pose origin. {@code mx,my} are the mouse in card coordinates (NaN when not hovering);
     * returns the hovered stack or EMPTY.
     */
    ItemStack card(GuiGraphicsExtractor g,Snapshot shot,float mx,float my){
        var mc=Minecraft.getInstance();
        long elapsed=mc.level==null?0:Math.max(0,mc.level.getGameTime()-shot.capturedAt);
        int remaining=shot.brewTicks>0?(int)Math.max(0,shot.brewTicks-elapsed):0;
        boolean finished=shot.done;
        String time;float progress;
        if(finished){time="готово";progress=1;}
        else if(shot.brewTicks<=0){time="—";progress=0;}
        else if(remaining<=0){time="готово";progress=1;}
        else{time=(remaining+19)/20+"с";progress=1-remaining/(float)BREW_TICKS;}
        VelaDraw.roundedOutline(g,0,0,W,H,7,(0x55<<24)|(VelaTheme.BORDER&0xFFFFFF),(0xE0<<24)|(VelaTheme.SURFACE&0xFFFFFF));
        ItemStack hovered=ItemStack.EMPTY;
        int cx=6;
        hovered=slot(g,shot.ingredient,cx,5,mx,my,hovered);cx+=18;
        // The chevron icon points down; turn it to point from the ingredient to the bottles.
        g.pose().pushMatrix();g.pose().translate(cx+5,14);g.pose().rotate(-(float)Math.PI/2);
        dev.sqwat.vela.ui.render.VelaIcons.draw(g,"chevron",-4,-4,8,VelaTheme.TEXT_MUTED);
        g.pose().popMatrix();cx+=10;
        for(int i=0;i<3;i++){hovered=slot(g,shot.bottles[i],cx,5,mx,my,hovered);cx+=20;}
        cx+=2;
        VelaDraw.roundedRect(g,cx,8,1,12,1,(0x50<<24)|(VelaTheme.BORDER&0xFFFFFF));cx+=5;
        // Fuel: the blaze powder stack in the fuel slot (with its count) and the stand's remaining charges below.
        ItemStack fuelShown=shot.fuelItem.isEmpty()&&shot.fuel>0?blaze():shot.fuelItem;
        hovered=slot(g,fuelShown,cx,5,mx,my,hovered);
        VelaDraw.roundedRect(g,cx+2,25,14,2,1,(0x90<<24)|(VelaTheme.BORDER&0xFFFFFF));
        if(shot.fuel>0)VelaDraw.roundedRect(g,cx+2,25,Math.max(1,Math.round(14f*Math.min(FUEL_CHARGES,shot.fuel)/FUEL_CHARGES)),2,1,0xFFFFB13B);
        int timeWidth=(int)Math.ceil(VelaDraw.textWidth(time,VelaDraw.Weight.REGULAR,6));
        int barRight=cx-8;
        VelaDraw.roundedRect(g,6,H-7,barRight-6,2,1,(0x90<<24)|(VelaTheme.BORDER&0xFFFFFF));
        if(progress>0)VelaDraw.roundedRect(g,6,H-7,Math.max(2,Math.round((barRight-6)*progress)),2,1,VelaTheme.PRIMARY);
        VelaDraw.text(g,time,cx+22,H-15f,progress>=1?VelaTheme.POSITIVE:VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.REGULAR,6);
        return hovered;
    }
    private static ItemStack slot(GuiGraphicsExtractor g,ItemStack stack,int x,int y,float mx,float my,ItemStack hovered){
        boolean over=mx>=x&&mx<x+18&&my>=y&&my<y+18;
        VelaDraw.roundedRect(g,x,y,18,18,4,over?(0xFF<<24)|(VelaTheme.BORDER&0xFFFFFF):(0xB0<<24)|(VelaTheme.SURFACE_DEEP&0xFFFFFF));
        if(!stack.isEmpty()){
            g.item(stack,x+1,y+1);
            g.itemDecorations(Minecraft.getInstance().font,stack,x+1,y+1);
        }
        return over&&!stack.isEmpty()?stack:hovered;
    }
    private static ItemStack blazeIcon;
    private static ItemStack blaze(){if(blazeIcon==null)blazeIcon=new ItemStack(net.minecraft.world.item.Items.BLAZE_POWDER);return blazeIcon;}

    /** Pinned card while the pin key is held: the mouse is free, hovering a slot shows the vanilla tooltip. */
    static final class PinScreen extends Screen {
        private static final float SCALE=1.5f;
        private final BrewViewer viewer;private final BlockPos pos;
        PinScreen(BrewViewer viewer,BlockPos pos){super(Component.literal("Brew Viewer"));this.viewer=viewer;this.pos=pos;}
        private boolean held(){return dev.sqwat.vela.ui.input.BindInput.down(Minecraft.getInstance().getWindow().handle(),viewer.pinKey.value());}
        @Override public void tick(){if(!held()||!viewer.enabled())onClose();}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){} // keep the world visible
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){
            float ox=width/2f-W*SCALE/2f,oy=height/2f-H*SCALE-12;
            ItemStack hovered;
            g.pose().pushMatrix();
            try{g.pose().translate(ox,oy);g.pose().scale(SCALE);hovered=viewer.card(g,viewer.snapshot(pos),(mouseX-ox)/SCALE,(mouseY-oy)/SCALE);}
            finally{g.pose().popMatrix();}
            if(!hovered.isEmpty())g.setTooltipForNextFrame(font,hovered,mouseX,mouseY);
        }
    }
}
