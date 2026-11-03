package dev.sqwat.vela.opt.block;

// a chest's lid and pair-light functions for the current tick; they only tie the two halves together, the partial tick is
// applied to them every frame
public interface ChestFunctions {
	Object velaopt$openFn(long tick);

	Object velaopt$lightFn(long tick);

	void velaopt$setOpenFn(Object fn, long tick);

	void velaopt$setLightFn(Object fn, long tick);
}
