package dev.sqwat.vela.opt.block;

public interface CachedLight {
	int velaopt$light();

	long velaopt$lightTick();

	void velaopt$setLight(int light, long tick);

	int velaopt$pairLight();

	long velaopt$pairLightTick();

	void velaopt$setPairLight(int light, long tick);
}
