package dev.sqwat.vela.opt.mixin.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.sqwat.vela.opt.entity.AnimFreeze;
import dev.sqwat.vela.opt.entity.AnimHolder;

@Getter
@Setter
@Accessors(fluent = true, chain = false)
@Mixin(Entity.class)
public class EntityAnimHolderMixin implements AnimHolder {
	@Unique
	private AnimFreeze.Holder velaopt$anim;
}
