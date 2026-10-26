package dev.sqwat.vela.module.utility;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.mixin.input.MinecraftActions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.BlockHitResult;
public final class CrystalOptimizer extends ClientModule {
    private BlockPos anchorBurstPosition;
    private int anchorBurstActions;
    private boolean runningAnchorBurstAction;
    public CrystalOptimizer(){super("crystal_optimizer","Crystal Optimizer","Снижает задержку установки кристалла",ModuleCategory.UTILITIES);}
    public static boolean ignoreLocalAttackCharge(boolean enabled,boolean mainHandCrystal,boolean attackTargetPresent){
        return enabled&&mainHandCrystal&&attackTargetPresent;
    }
    public static int remainingAnchorUses(int currentCharges){return Math.max(0,5-Math.max(0,Math.min(4,currentCharges)));}
    public static boolean anchorChargeAdvanced(int before,int after){return before>=0&&after>before&&after<=4;}
    @Override protected void onDisable(){anchorBurstPosition=null;anchorBurstActions=0;}

    public void afterVanillaUse(Minecraft client,int beforeCharges){
        if(!enabled()||runningAnchorBurstAction||client.player==null||client.level==null||client.gui.screen()!=null||!(client.hitResult instanceof BlockHitResult hit))return;
        var state=client.level.getBlockState(hit.getBlockPos());
        if(!state.is(Blocks.RESPAWN_ANCHOR)||!anchorChargeAdvanced(beforeCharges,state.getValue(RespawnAnchorBlock.CHARGE)))return;
        anchorBurstPosition=hit.getBlockPos().immutable();
        anchorBurstActions=remainingAnchorUses(state.getValue(RespawnAnchorBlock.CHARGE));
    }

    @Override public void onClientTick(){
        if(anchorBurstActions<=0||anchorBurstPosition==null)return;
        var client=Minecraft.getInstance();
        if(client.player==null||client.level==null||client.gameMode==null||client.gui.screen()!=null||client.gui.overlay()!=null||
            !client.isWindowActive()||client.isPaused()||!client.player.isAlive()||client.player.isSpectator()||client.player.isUsingItem()||client.player.isHandsBusy()||
            !(client.player.getMainHandItem().is(net.minecraft.world.item.Items.GLOWSTONE)||client.player.getOffhandItem().is(net.minecraft.world.item.Items.GLOWSTONE))||!client.level.getBlockState(anchorBurstPosition).is(Blocks.RESPAWN_ANCHOR)||
            !(client.hitResult instanceof BlockHitResult hit)||!hit.getBlockPos().equals(anchorBurstPosition)){
            anchorBurstPosition=null;anchorBurstActions=0;return;
        }
        runningAnchorBurstAction=true;
        try{((MinecraftActions)client).vela$use();}finally{runningAnchorBurstAction=false;}
        if(--anchorBurstActions==0)anchorBurstPosition=null;
    }
}
