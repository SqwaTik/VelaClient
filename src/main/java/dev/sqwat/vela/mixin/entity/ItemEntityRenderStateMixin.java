package dev.sqwat.vela.mixin.entity;
import dev.sqwat.vela.render.entity.ItemEntityRenderStateAccess;

import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public abstract class ItemEntityRenderStateMixin implements ItemEntityRenderStateAccess {
    @Unique public boolean vela$grounded;
    @Unique private float vela$tilt;
    @Override public void vela$setTilt(float tilt){vela$tilt=tilt;}
    @Override public float vela$getTilt(){return vela$tilt;}
    @Override public void vela$setGrounded(boolean grounded){vela$grounded=grounded;}
    @Override public boolean vela$isGrounded(){return vela$grounded;}
}
