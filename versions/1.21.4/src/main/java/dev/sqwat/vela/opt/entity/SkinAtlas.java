package dev.sqwat.vela.opt.entity;

import lombok.experimental.UtilityClass;

// the skin atlas draws through 1.21.9+ submits and GPU textures; on 1.21.4 its feature is unavailable and nothing is batched
@UtilityClass
public class SkinAtlas {
	public void endFrame() {}
}
