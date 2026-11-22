package dev.sqwat.vela.opt.entity;

import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

// BakedQuad accessors that differ between versions: on 1.21.5 a quad is packed BLOCK vertices, 8 ints each, position first
@UtilityClass
public class Quads {
	public Direction direction(BakedQuad q) {
		return q.direction();
	}

	// z of vertex i
	public float z(BakedQuad q, int i) {
		return Float.intBitsToFloat(q.vertices()[i * 8 + 2]);
	}
}
