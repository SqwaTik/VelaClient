package dev.sqwat.vela.neoforge.shim.fabric.api.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** A Fabric event: listeners in registration order, run by NeoEvents from the matching NeoForge event. */
public final class Event<T> {
    private final List<T> listeners = new CopyOnWriteArrayList<>();

    public void register(T listener) { listeners.add(listener); }

    public List<T> listeners() { return listeners; }
}
