package dev.sqwat.vela.compat.gizmos;

/** 1.21.11's handle on a submitted gizmo. Gizmos here last one frame, so persistence and fading do nothing. */
public interface GizmoProperties {
    GizmoProperties setAlwaysOnTop();
    default GizmoProperties persistForMillis(int millis){return this;}
    default GizmoProperties fadeOut(){return this;}
}
