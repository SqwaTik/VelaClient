package dev.sqwat.vela.media;

/**
 * Smooth song position for the progress bar and the lyrics. Players report their position rarely and late, and each
 * report would make a naive extrapolation jump back or forth; this clock runs on the wall clock and only steers
 * towards each new report over about a second. Big differences (a seek, a new song, play/pause) are taken at once.
 */
public final class PlaybackClock {
    static final long SNAP_MS=1500,TRUST_MS=30*60_000,SEEK_HOLD_MS=3000;
    private MediaTrack song;
    private long rawPosition=-1,rawUpdated=-1;
    private boolean playing;
    /** Position {@code base} was true at {@code baseAt}; the correction still to be eaten decays from {@code correctionAt}. */
    private long base,baseAt;
    private float correction;
    private long correctionAt;
    /** After a seek from the HUD the player keeps reporting the old spot for a moment; those reports are ignored. */
    private long seekTarget=-1,seekAt;

    /** Feeds a helper reading; call whenever a reading may be new. */
    public void update(MediaTrack track,long nowMs){
        if(track==null){song=null;rawPosition=-1;return;}
        boolean newSong=!track.sameSong(song),stateChanged=track.playing()!=playing;
        boolean newReport=track.positionMs()!=rawPosition||track.updatedMs()!=rawUpdated;
        song=track;
        if(!newSong&&!stateChanged&&!newReport)return;
        rawPosition=track.positionMs();rawUpdated=track.updatedMs();
        // A missing, future or ancient report time is a player clock that cannot be trusted: count from arrival.
        boolean trusted=track.updatedMs()>0&&track.updatedMs()<=nowMs+2000&&nowMs-track.updatedMs()<TRUST_MS;
        long target=trusted?track.position(nowMs):clamp(track.positionMs(),track);
        if(seekTarget>=0){
            long expected=seekTarget+(track.playing()?nowMs-seekAt:0);
            if(!newSong&&nowMs-seekAt<SEEK_HOLD_MS&&Math.abs(target-expected)>SNAP_MS)return;
            seekTarget=-1;
        }
        long shown=position(nowMs);
        playing=track.playing();
        float drift=target-shown;
        base=target;baseAt=nowMs;
        if(newSong||stateChanged||Math.abs(drift)>SNAP_MS||!playing){correction=0;return;}
        // Keep showing what was shown and ease the difference away.
        correction=-drift;correctionAt=nowMs;
    }
    /** Jumps to {@code positionMs} at once, ahead of the player confirming the seek. */
    public void seek(long positionMs,long nowMs){
        if(song==null)return;
        base=clamp(positionMs,song);baseAt=nowMs;correction=0;
        seekTarget=base;seekAt=nowMs;
    }
    /** Smoothed position at wall-clock {@code nowMs}. */
    public long position(long nowMs){
        if(song==null)return 0;
        long position=base+(playing?Math.max(0,nowMs-baseAt):0);
        if(correction!=0){
            // Slow enough that the shown time never runs backwards.
            float tau=Math.max(600,Math.abs(correction)*1.2f);
            position+=Math.round(correction*Math.exp(-(nowMs-correctionAt)/tau));
        }
        return clamp(position,song);
    }
    private static long clamp(long position,MediaTrack track){
        if(track.durationMs()>0)position=Math.min(position,track.durationMs());
        return Math.max(0,position);
    }
}
