package dev.sqwat.vela.ui.animation;
/** Retargetable transition with an exact endpoint, avoiding exponential pixel tails. */
public final class FiniteMotion {
 private float value,from,target,elapsed;
 public void reset(float next){value=from=target=next;elapsed=0;}
 public float update(float next,float dt,float duration){
  return update(next,dt,duration,false);
 }
 public float update(float next,float dt,float duration,boolean spring){
  if(next!=target){from=value;target=next;elapsed=0;}
  elapsed+=Math.max(0,dt);
  float length=spring?duration*Math.abs(target-from):duration;
  float t=length<=0?1:Math.min(1,elapsed/length);
  float eased=spring?(target>from?1-back(1-t):back(t)):t*t*(3-2*t);
  value=t>=1?target:from+(target-from)*eased;
  return value;
 }
 public static float back(float t){return 2.70158f*t*t*t-1.70158f*t*t;}
}
