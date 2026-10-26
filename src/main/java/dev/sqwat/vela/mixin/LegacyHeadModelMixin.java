package dev.sqwat.vela.mixin;

import com.mojang.math.Transformation;
import dev.sqwat.vela.render.LegacyHeadTransform;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.special.*;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.Optional;

/** Old resource-pack skull fallbacks omit the transform moved into item JSON in 26.2. */
@Mixin(SpecialModelWrapper.Unbaked.class)
abstract class LegacyHeadModelMixin {
    @Shadow public abstract Identifier base();
    @Shadow public abstract Optional<Transformation> transformation();
    @Shadow public abstract SpecialModelRenderer.Unbaked<?> specialModel();

    @ModifyVariable(method="bake",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private Matrix4fc vela$legacyHeadTransform(Matrix4fc parent) {
        if(transformation().isPresent()||!base().getNamespace().equals("minecraft")||!base().getPath().equals("item/template_skull"))return parent;
        var model=specialModel();
        return model instanceof PlayerHeadSpecialRenderer.Unbaked||model instanceof SkullSpecialRenderer.Unbaked
            ?LegacyHeadTransform.compose(parent):parent;
    }
}
