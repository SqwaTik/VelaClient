package dev.sqwat.vela.opt.gl;

import com.mojang.blaze3d.vertex.VertexFormat;

public interface RawBuffer {
	boolean velaopt$accepts(VertexFormat format);

	void velaopt$push(long src, int count);

	// room for count whole vertices, filled in later by the caller (a worker may write it until the mesh is uploaded)
	long velaopt$reserve(int count);

	// grows the backing memory now so the next reserves totalling bytes never move it: addresses already handed out stay valid
	void velaopt$ensureFree(long bytes);
}
