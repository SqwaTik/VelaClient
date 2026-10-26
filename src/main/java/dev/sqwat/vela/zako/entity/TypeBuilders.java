// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.entity;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.rendertype.RenderType;

@UtilityClass
public class TypeBuilders {
	public BufferBuilder create(ByteBufferBuilder buffer, RenderType type) {
		return new BufferBuilder(buffer, type.primitiveTopology(), type.format());
	}

	public int positionOffset(VertexFormat format) {
		return format.getElement(DefaultVertexFormat.POSITION_SEMANTIC_NAME).offset();
	}
}
