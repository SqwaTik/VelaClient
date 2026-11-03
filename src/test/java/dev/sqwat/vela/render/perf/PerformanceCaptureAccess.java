package dev.sqwat.vela.render.perf;

/** Test access to package-private PerformanceCapture helpers. */
public final class PerformanceCaptureAccess {
    private PerformanceCaptureAccess(){}
    public static boolean automatic(java.nio.file.Path configDir,String environment){return PerformanceCapture.automaticEnabled(configDir,environment);}
}
