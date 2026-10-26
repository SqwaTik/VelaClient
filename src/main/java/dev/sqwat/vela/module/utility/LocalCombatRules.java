package dev.sqwat.vela.module.utility;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;

/** Vanilla built-in enchantment rules unavailable to client-only player entities. */
public final class LocalCombatRules {
    private LocalCombatRules(){}
    public static int level(ItemStack item,ResourceKey<Enchantment> key){
        for(var entry:item.getEnchantments().entrySet())if(entry.getKey().is(key))return Math.clamp(entry.getIntValue(),0,255);
        return 0;
    }
    public static float sharpness(int level){return level<=0?0:.5f*level+.5f;}
    public static int protection(ItemStack item,DamageSource source){
        if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return 0;
        int result=level(item,Enchantments.PROTECTION);
        if(source.is(DamageTypeTags.IS_FIRE))result+=2*level(item,Enchantments.FIRE_PROTECTION);
        if(source.is(DamageTypeTags.IS_EXPLOSION))result+=2*level(item,Enchantments.BLAST_PROTECTION);
        if(source.is(DamageTypeTags.IS_PROJECTILE))result+=2*level(item,Enchantments.PROJECTILE_PROTECTION);
        if(source.is(DamageTypeTags.IS_FALL))result+=3*level(item,Enchantments.FEATHER_FALLING);
        return result;
    }
}
