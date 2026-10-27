package dev.sqwat.vela.mixin.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The synced skin-parts byte; its spare bit marks Vela players (VelaUsers). */
@Mixin(Avatar.class)
public interface AvatarAccessor {
    @Accessor("DATA_PLAYER_MODE_CUSTOMISATION")
    static EntityDataAccessor<Byte> vela$customisation(){throw new AssertionError();}
}
