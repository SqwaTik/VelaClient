package dev.sqwat.vela.neoforge.shim.loader.api;

public record Version(String getFriendlyString) {
    @Override public String toString() { return getFriendlyString; }
}
