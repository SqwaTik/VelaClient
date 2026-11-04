package dev.sqwat.vela.opt.entity;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.rendertype.RenderType;

@UtilityClass
public class TypeBuilders {
	public BufferBuilder create(ByteBufferBuilder buffer, RenderType type) {
		return new BufferBuilder(buffer, type.mode(), type.format());
	}

	public int positionOffset(VertexFormat format) {
		return format.getOffset(VertexFormatElement.POSITION);
	}
}
