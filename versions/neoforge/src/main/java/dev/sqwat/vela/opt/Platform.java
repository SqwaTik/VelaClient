package dev.sqwat.vela.opt;

import lombok.experimental.UtilityClass;
import dev.sqwat.vela.opt.platform.Loader;
import dev.sqwat.vela.opt.platform.NeoPlatform;

import java.nio.file.Path;

// NeoForge build: the loader is always NeoForge
@UtilityClass
public class Platform {
	private final Loader LOADER = new NeoPlatform();

	public Loader loader() {
		return LOADER;
	}

	public boolean isModLoaded(String id) {
		return LOADER.isModLoaded(id);
	}

	public Path configDir() {
		return LOADER.configDir();
	}

	public boolean isDevelopment() {
		return LOADER.isDevelopment();
	}
}
