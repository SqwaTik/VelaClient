package dev.sqwat.vela.opt.entity;

import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

// BakedQuad on 1.21.4 is a class with getters; packed BLOCK vertices, 8 ints each, position first
@UtilityClass
public class Quads {
	public Direction direction(BakedQuad q) {
		return q.getDirection();
	}

	// z of vertex i
	public float z(BakedQuad q, int i) {
		return Float.intBitsToFloat(q.getVertices()[i * 8 + 2]);
	}
}
