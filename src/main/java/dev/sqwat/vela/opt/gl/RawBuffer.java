package dev.sqwat.vela.opt.gl;

import com.mojang.blaze3d.vertex.VertexFormat;

public interface RawBuffer {
	boolean velaopt$accepts(VertexFormat format);

	void velaopt$push(long src, int count);
}
