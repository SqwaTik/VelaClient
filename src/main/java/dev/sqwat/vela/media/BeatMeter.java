package dev.sqwat.vela.media;

import java.util.Arrays;

/**
 * Makes the spectrum move with the music. The helper sends each band in decibels squeezed into 0..1, where a loud song
 * sits near the top all the time and every frame looks alike. Here the bands go back to linear loudness, each is
 * measured against its own recent maximum (rises at once, forgets in a few seconds), and a beat detector on the low
 * bands gives a short pulse whenever the kick or bass jumps above its running average.
 */
public final class BeatMeter {
    /** Seconds for a band's remembered maximum to fall to ~37 %: how fast quiet passages fill the bars again. */
    private static final float MEMORY=3.5f;
    /** A band is never measured against less than this share of the loudest band, so silence stays flat. */
    private static final float FLOOR=.04f;
    private float[] max=new float[0];
    private float[] heights=new float[0];
    private float bassAverage,bassBefore,levelAverage,pulse;
    private long beatAt,fedAt;

    /** Linear loudness of one band value, undoing the helper's encoding (dB + 62 over 56, high bands tilted 18 dB up). */
    static float magnitude(float value,int band,int bands){
        return (float)Math.pow(10,(value*56-62-18.0*band/bands)/20);
    }

    /** Takes one fresh spectrum from the helper; {@link #heights()} and {@link #pulse()} follow it. */
    public void feed(float[] bands,long nowMs){
        float dt=fedAt==0?.04f:Math.clamp((nowMs-fedAt)/1000f,.001f,.5f);fedAt=nowMs;
        int n=bands.length;
        if(max.length!=n){max=new float[n];heights=new float[n];Arrays.fill(max,1e-5f);}
        float[] loud=new float[n];float top=0,bass=0;
        float forget=(float)Math.exp(-dt/MEMORY);
        for(int b=0;b<n;b++){
            loud[b]=bands[b]<=0?0:magnitude(bands[b],b,n);
            max[b]=Math.max(loud[b],max[b]*forget);
            top=Math.max(top,max[b]);
            // The lowest fifth of the bands (≈90–250 Hz) carries the kick drum and the bass line.
            if(b<Math.max(1,n/5))bass+=loud[b]*loud[b];
        }
        for(int b=0;b<n;b++){
            float reference=Math.max(max[b],top*FLOOR);
            heights[b]=reference<=0?0:(float)Math.pow(Math.clamp(loud[b]/reference,0,1),.8);
        }
        beat((float)Math.sqrt(bass),dt,nowMs);
    }
    /** Without a spectrum, only the overall loudness (0..1, already in dB): a jump above its average is a beat. */
    public void feedLevel(float level,long nowMs){
        float dt=fedAt==0?.04f:Math.clamp((nowMs-fedAt)/1000f,.001f,.5f);fedAt=nowMs;
        beat((float)Math.pow(10,level*2.2),dt,nowMs);
        levelAverage+=(level-levelAverage)*(1-(float)Math.exp(-dt/.6));
    }
    private void beat(float energy,float dt,long nowMs){
        // Rising, well above the last ~0.4 s, and not right after the previous beat (no double hits on one kick).
        if(energy>bassAverage*1.3f&&energy>bassBefore&&bassAverage>0&&nowMs-beatAt>170){
            pulse=Math.min(1,.55f+(energy/bassAverage-1.3f)*.9f);beatAt=nowMs;
        }
        bassAverage=bassAverage==0?energy:bassAverage+(energy-bassAverage)*(1-(float)Math.exp(-dt/.4));
        bassBefore=energy;
    }
    /** Per-band 0..1 heights of the last spectrum. */
    public float[] heights(){return heights;}
    /** 1 on a beat, fading out over ~0.15 s. */
    public float pulse(long nowMs){return beatAt==0?0:pulse*(float)Math.exp(-(nowMs-beatAt)/150.0);}
    /** Loudness relative to its own recent average, for the level-only fallback (about 0.5 = average). */
    public float relativeLevel(float level){return Math.clamp(.5f+(level-levelAverage)*2.5f,0,1);}
    public void reset(){max=new float[0];heights=new float[0];bassAverage=bassBefore=levelAverage=pulse=0;beatAt=fedAt=0;}
}
