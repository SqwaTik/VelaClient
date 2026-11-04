package dev.sqwat.vela.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 26.3 stand-in for the removed {@code PotionBrewing}: brewing is recipes now, and a client only gets the recipes in
 * singleplayer (from the integrated server). On a server it knows which items are reagents (synced property set) but
 * not the results, so finished brews keep the bottles from the last time the stand was open.
 */
public final class Brewing {
    private final RecipeManager recipes;
    private final Level level;
    private final RecipePropertySet reagents;
    private Brewing(RecipeManager recipes, Level level, RecipePropertySet reagents) {
        this.recipes = recipes; this.level = level; this.reagents = reagents;
    }

    public static Brewing current() {
        var mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        var reagents = mc.level == null ? RecipePropertySet.EMPTY : mc.level.recipeAccess().propertySet(RecipePropertySet.BREWING_REAGENTS);
        return server == null ? new Brewing(null, null, reagents) : new Brewing(server.getRecipeManager(), server.overworld(), reagents);
    }

    public boolean isIngredient(ItemStack ingredient) { return reagents.test(ingredient); }
    public boolean hasMix(ItemStack bottle, ItemStack ingredient) {
        return recipes != null && recipes.getRecipeFor(RecipeType.BREWING, new BrewingInput(bottle, ingredient), level).isPresent();
    }
    public ItemStack mix(ItemStack ingredient, ItemStack bottle) {
        if (recipes == null) return bottle;
        var input = new BrewingInput(bottle, ingredient);
        return recipes.getRecipeFor(RecipeType.BREWING, input, level).map(h -> h.value().assemble(input)).orElse(bottle);
    }
}
