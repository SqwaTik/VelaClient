package dev.sqwat.vela.ui.animation;

/** Camera inertia and target fade; swing uses the game's interpolated animation directly. */
public final class CrosshairMotion {
    private boolean initialized;
    private float previousYaw,previousPitch,x,y,reaction;
    public void reset(){initialized=false;x=y=reaction=0;}
    public void update(float yaw,float pitch,boolean targeted,boolean dynamic,float dt){
        reaction=Motion.approach(reaction,targeted?1:0,10,dt);
        if(!dynamic||!Float.isFinite(yaw)||!Float.isFinite(pitch)){initialized=false;x=y=0;return;}
        if(!initialized){previousYaw=yaw;previousPitch=pitch;initialized=true;}
        float delta=(yaw-previousYaw)%360;
        if(delta>=180)delta-=360;if(delta< -180)delta+=360;
        x=Math.clamp(delta*.5f,-40,40);y=Math.clamp((pitch-previousPitch)*.5f,-40,40);
        previousYaw=yaw-delta+Motion.approach(0,delta,3.6f,dt);
        previousPitch=Motion.approach(previousPitch,pitch,3.6f,dt);
    }
    public float x(){return x;} public float y(){return y;} public float reaction(){return reaction;}
    public static float arc(float swing,boolean dynamic){
        if(!dynamic||!Float.isFinite(swing))return 1;
        return Math.abs(1-2*Math.clamp(swing,0,1));
    }
}
