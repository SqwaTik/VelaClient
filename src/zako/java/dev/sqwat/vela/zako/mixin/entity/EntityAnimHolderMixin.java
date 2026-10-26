// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.sqwat.vela.zako.entity.AnimFreeze;
import dev.sqwat.vela.zako.entity.AnimHolder;

@Getter
@Setter
@Accessors(fluent = true, chain = false)
@Mixin(Entity.class)
public class EntityAnimHolderMixin implements AnimHolder {
	@Unique
	private AnimFreeze.Holder zakoopt$anim;
}
