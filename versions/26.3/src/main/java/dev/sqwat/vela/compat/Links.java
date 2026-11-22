package dev.sqwat.vela.compat;

import com.mojang.blaze3d.Blaze3D;
import java.net.URI;
import java.nio.file.Path;

/** 26.3 moved {@code Util.getPlatform().openUri/openPath} to {@link Blaze3D}, which takes only URIs. */
public final class Links {
    private Links() {}
    public static void openUri(String uri) { Blaze3D.openUri(URI.create(uri)); }
    public static void openUri(URI uri) { Blaze3D.openUri(uri); }
    public static void openPath(Path path) { Blaze3D.openPath(path); }
}
