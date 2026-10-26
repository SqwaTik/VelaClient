package dev.sqwat.vela.ui.hud;

public final class NearDirection {
    public static final int LIMIT=5;
    private NearDirection() {}
    /** Rotation of a right-pointing icon; screen up means straight ahead. */
    public static float radians(double dx,double dz,float yawDegrees){
        return (float)(Math.atan2(dz,dx)-Math.toRadians(yawDegrees)-Math.PI);
    }
}
