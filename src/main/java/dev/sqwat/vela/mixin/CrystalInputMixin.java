package dev.sqwat.vela.mixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
abstract class CrystalInputMixin {
    @Shadow private int rightClickDelay;
    @Shadow public int missTime;
    @Unique private int vela$anchorChargeBefore=-1;
    @Unique private boolean vela$crystalUse;
    @Unique private boolean vela$optimizerReady(){
        var c=(Minecraft)(Object)this;var services=dev.sqwat.vela.VelaClient.services();
        var module=services==null?null:services.modules().byId("crystal_optimizer");
        return module!=null&&module.enabled()&&c.player!=null&&c.level!=null&&c.gameMode!=null&&c.gui.screen()==null&&c.gui.overlay()==null&&c.isWindowActive()&&!c.isPaused()&&c.player.isAlive()&&!c.player.isSpectator()&&!c.player.isUsingItem();
    }
    @Inject(method="startAttack",at=@At("HEAD"))
    private void vela$clearCrystalMiss(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci){
        var c=(Minecraft)(Object)this;
        if(vela$optimizerReady()&&c.hitResult instanceof EntityHitResult hit&&hit.getEntity() instanceof EndCrystal)missTime=0;
    }
    @Inject(method="handleKeybinds",at=@At("HEAD"))
    private void vela$crystalInput(CallbackInfo ci){
        var c=(Minecraft)(Object)this;var services=dev.sqwat.vela.VelaClient.services();
        if(services==null||!services.modules().byId("crystal_optimizer").enabled()||c.player==null||c.level==null||c.gui.screen()!=null||c.gui.overlay()!=null||!c.isWindowActive()||c.isPaused()||!c.player.isAlive()||c.player.isSpectator()||c.player.isUsingItem())return;
        if((c.hitResult instanceof BlockHitResult||c.hitResult instanceof EntityHitResult)&&
            (c.player.getMainHandItem().is(Items.END_CRYSTAL)||c.player.getOffhandItem().is(Items.END_CRYSTAL)))rightClickDelay=0;
        if(c.hitResult instanceof BlockHitResult anchorHit&&c.level.getBlockState(anchorHit.getBlockPos()).is(Blocks.RESPAWN_ANCHOR)&&
            (c.player.getMainHandItem().is(Items.GLOWSTONE)||c.player.getOffhandItem().is(Items.GLOWSTONE)))rightClickDelay=0;
        if(c.hitResult instanceof EntityHitResult hit&&hit.getEntity() instanceof EndCrystal)missTime=0;
    }

    @Redirect(method="startAttack",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;cannotAttackWithItem(Lnet/minecraft/world/item/ItemStack;I)Z"))
    private boolean vela$ignoreCrystalCharge(LocalPlayer player,ItemStack heldItem,int tolerance){
        var c=(Minecraft)(Object)this;var services=dev.sqwat.vela.VelaClient.services();
        boolean enabled=services!=null&&services.modules().byId("crystal_optimizer").enabled();
        if(enabled&&c.hitResult instanceof EntityHitResult hit&&hit.getEntity() instanceof EndCrystal)return false;
        return player.cannotAttackWithItem(heldItem,tolerance);
    }

    @Inject(method="startUseItem",at=@At("HEAD"))
    private void vela$captureAnchorCharge(CallbackInfo ci){
        var client=(Minecraft)(Object)this;
        vela$crystalUse=vela$optimizerReady()&&(client.player.getMainHandItem().is(Items.END_CRYSTAL)||client.player.getOffhandItem().is(Items.END_CRYSTAL));
        var services=dev.sqwat.vela.VelaClient.services();
        if(services!=null&&services.modules().byId("crystal_optimizer").enabled()&&client.player!=null&&client.gameMode!=null&&client.gui.screen()==null&&client.hitResult instanceof BlockHitResult&&
            (client.player.getMainHandItem().is(Items.END_CRYSTAL)||client.player.getOffhandItem().is(Items.END_CRYSTAL))&&client.gameMode.isDestroying())client.gameMode.stopDestroyBlock();
        vela$anchorChargeBefore=client.level!=null&&client.hitResult instanceof BlockHitResult hit&&client.level.getBlockState(hit.getBlockPos()).is(Blocks.RESPAWN_ANCHOR)
            ?client.level.getBlockState(hit.getBlockPos()).getValue(RespawnAnchorBlock.CHARGE):-1;
    }

    @Inject(method="startUseItem",at=@At("RETURN"))
    private void vela$queueAnchorBurst(CallbackInfo ci){
        var client=(Minecraft)(Object)this;var services=dev.sqwat.vela.VelaClient.services();
        if(vela$crystalUse&&vela$optimizerReady())rightClickDelay=0;
        vela$crystalUse=false;
        int before=vela$anchorChargeBefore;vela$anchorChargeBefore=-1;
        if(services==null||!(services.modules().byId("crystal_optimizer") instanceof dev.sqwat.vela.module.CrystalOptimizer optimizer))return;
        optimizer.afterVanillaUse(client,before);
    }
}
