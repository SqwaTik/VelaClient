package dev.sqwat.vela.render.entity;
/** Added to EntityRenderState: the model is behind opaque blocks, only the name tag and leash are submitted. */
public interface OccludedRenderState {
    boolean vela$occluded();
    void vela$occluded(boolean value);
}
