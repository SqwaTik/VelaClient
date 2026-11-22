package dev.sqwat.vela.neoforge.shim.api;

import java.lang.annotation.*;

/** No effect: the NeoForge jar is client-only. */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
public @interface Environment {
    EnvType value();
}
