package dev.sqwat.vela.compat;

import com.mojang.blaze3d.shaders.AbstractUniform;

/** A lightmap factor uniform whose value goes through {@link dev.sqwat.vela.module.utility.Gamma#boost} (1.21.4 GammaMixin). */
public final class BoostedUniform extends AbstractUniform {
    private final AbstractUniform target;
    public BoostedUniform(AbstractUniform target){this.target=target;}
    @Override public void set(float value){target.set(dev.sqwat.vela.module.utility.Gamma.boost(value));}
}
