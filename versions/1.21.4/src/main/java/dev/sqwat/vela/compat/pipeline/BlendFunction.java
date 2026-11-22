package dev.sqwat.vela.compat.pipeline;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;

/** 1.21.5's blend function: colour and alpha factors. */
public record BlendFunction(SourceFactor sourceColor,DestFactor destColor,SourceFactor sourceAlpha,DestFactor destAlpha) {
    public static final BlendFunction TRANSLUCENT=new BlendFunction(SourceFactor.SRC_ALPHA,DestFactor.ONE_MINUS_SRC_ALPHA,SourceFactor.ONE,DestFactor.ONE_MINUS_SRC_ALPHA);
    public static final BlendFunction ADDITIVE=new BlendFunction(SourceFactor.ONE,DestFactor.ONE);
    public BlendFunction(SourceFactor source,DestFactor dest){this(source,dest,source,dest);}
}
