package dev.sqwat.vela.opt.entity;

import lombok.experimental.UtilityClass;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;

// BakedQuad accessors that differ between versions
@UtilityClass
public class Quads {
	public Direction direction(BakedQuad q) {
		return q.direction();
	}

	// z of vertex i
	public float z(BakedQuad q, int i) {
		return q.position(i).z();
	}
}
