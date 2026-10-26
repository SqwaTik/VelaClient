package dev.sqwat.vela.module;

import dev.sqwat.vela.VelaClient;

/**
 * Static access to modules from mixins and render hooks, which cannot receive {@link dev.sqwat.vela.core.VelaServices}.
 * Replaces the copy-pasted {@code services().modules().byId("id") instanceof X m && m.enabled()} checks: the class is
 * the key, so a renamed id can no longer silently disable a feature.
 */
public final class Modules {
    private Modules(){}
    /** The module, or null before the client has initialised. */
    public static <T extends ClientModule> T get(Class<T> type){
        var services=VelaClient.services();
        return services==null?null:services.modules().get(type);
    }
    /** The module when it is switched on and available, otherwise null. */
    public static <T extends ClientModule> T active(Class<T> type){
        T module=get(type);
        return module!=null&&module.enabled()&&module.available()?module:null;
    }
}
