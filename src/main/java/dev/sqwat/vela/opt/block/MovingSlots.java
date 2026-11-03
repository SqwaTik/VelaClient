package dev.sqwat.vela.opt.block;

// a moving piston's two moving-block states (the moved block and, for a retracting piston, its base), kept between frames
public interface MovingSlots {
	Object velaopt$moving(int slot);

	void velaopt$setMoving(int slot, Object state);
}
