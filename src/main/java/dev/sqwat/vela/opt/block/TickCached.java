package dev.sqwat.vela.opt.block;

public interface TickCached {
	Object velaopt$cached();

	long velaopt$cachedTick();

	void velaopt$setCached(Object value, long tick);
}
