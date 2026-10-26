package dev.sqwat.vela.render.resource;

/** A reload can begin before joining a world, but must not follow a later world change. */
public final class ReloadWorldBinding {
    private Object world;
    public boolean visible(Object current,boolean eligible) {
        return visible(current,eligible,true);
    }
    public boolean visible(Object current,boolean eligible,boolean ready) {
        if(!eligible||!ready||current==null)return false;
        if(world==null)world=current;
        return world==current;
    }
}
