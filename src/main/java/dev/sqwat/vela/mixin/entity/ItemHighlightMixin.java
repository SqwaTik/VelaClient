package dev.sqwat.vela.mixin.entity;

import dev.sqwat.vela.module.visual.ItemHighlight;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
abstract class ItemHighlightMixin {
    @Inject(method="item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",at=@At("HEAD"))
    private void vela$background(LivingEntity entity,Level level,ItemStack stack,int x,int y,int seed,CallbackInfo ci){
        if(stack.isEmpty())return;
        var services=dev.sqwat.vela.VelaClient.services();
        if(services==null||!(services.modules().byId("item_highlight") instanceof ItemHighlight module)||!module.enabled())return;
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(!id.getNamespace().equals("minecraft"))return;
        int color=module.background(id.getPath());
        if(color!=0)((GuiGraphicsExtractor)(Object)this).fill(x,y,x+16,y+16,color);
    }
}
