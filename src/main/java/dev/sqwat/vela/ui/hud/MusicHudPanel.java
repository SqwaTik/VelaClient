package dev.sqwat.vela.ui.hud;

import dev.sqwat.vela.media.BeatMeter;
import dev.sqwat.vela.media.Lyrics;
import dev.sqwat.vela.media.LyricsService;
import dev.sqwat.vela.media.MediaTrack;
import dev.sqwat.vela.media.MusicBrand;
import dev.sqwat.vela.media.MusicGlyph;
import dev.sqwat.vela.media.MusicTextures;
import dev.sqwat.vela.module.ModuleRegistry;
import dev.sqwat.vela.module.visual.MusicHud;
import dev.sqwat.vela.ui.Lang;
import dev.sqwat.vela.ui.render.UiClip;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.List;
import java.util.Locale;

/**
 * Music panel. Full mode: cover with the service's logo on it, the source, a scrolling title and the artist, a live
 * spectrum of the song over the whole width, the progress with play controls, and optionally lyrics that roll in time
 * with the song, the sung part filling with colour. Mini mode: a notch hanging from the top edge of the screen, like a
 * drop of the edge itself, with the cover and four rhythm bars, widening to show the sung line. Switching between the
 * two morphs the panel: it slides to the top centre while shrinking, and the top corners flare out into the edge.
 */
final class MusicHudPanel {
    static final int WIDTH=204,HEAD=100,LYRICS=50,MINI_HEIGHT=24;
    /** Seconds the full ⇄ mini morph takes. */
    private static final float MORPH_S=.5f;
    /** Draws the panel background: height, corner radius, and how far the top has turned into the notch (0..1). */
    interface Background{void draw(int height,int radius,float notch);}
    private static final int COVER_X=8,COVER_Y=8,COVER=44,TEXT_X=60,RIGHT=WIDTH-8,LINE=13;
    private static final int SPECTRUM_Y=58,SPECTRUM_H=18,BAR_Y=80,CONTROLS_Y=85,BUTTON=13,BARS=40;
    private static final int MINI_SHORT=60,MINI_WIDE=204,MINI_BARS=4,MINI_RADIUS=9;
    private static final int SAMPLE_ACCENT=0xFFFF8FB1,FALLBACK_ACCENT=0xFFE8ECF4;
    private static final long SAMPLE_LENGTH=200_000,SCROLL_RESET_MS=4000,LYRICS_LAG_MS=250;
    private static final List<String> SAMPLE_LINES=List.of("Здесь будет текст песни","Строки идут в такт музыке",
        "Спетая часть заливается цветом","А следующая строка уже ждёт","");
    private final ModuleRegistry modules;
    private Lyrics sampleLyrics;
    // Spectrum: shown bar heights, the meter that turns raw bands into beat-driven heights, and when they were stepped.
    private final float[] bars=new float[BARS];
    private final BeatMeter meter=new BeatMeter();
    private float[] fedBands;
    private long barsAt;
    // Lyrics: the line the view is centred on (eased towards the sung line plus the wheel offset).
    private float view=Float.NaN;
    private MediaTrack viewSong;
    private int scroll;
    private long scrolledAt;
    // Mini pill width, eased between short and wide; the full ⇄ mini morph (0 full, 1 mini) and its clock.
    private float miniWidth=MINI_SHORT;
    private float morph=Float.NaN;
    private long morphAt;
    private long miniAt,viewAt;
    private float hoverX=-1,hoverY=-1;
    private String pressed;
    private long pressedAt;

    MusicHudPanel(ModuleRegistry modules){this.modules=modules;}
    private MusicHud module(){return modules.byId("music") instanceof MusicHud m?m:null;}
    boolean hasContent(){var m=module();return m!=null&&m.visible();}
    boolean mini(){var m=module();return m!=null&&m.mini();}

    /** Steps the morph towards the mode that is on; safe to call any number of times a frame. */
    private float morph(){
        var m=module();float target=m!=null&&m.mini()?1:0;
        long now=System.nanoTime();
        var services=dev.sqwat.vela.VelaClient.services();
        boolean instant=services==null||!services.configs().current().animations||services.configs().current().reducedMotion;
        if(Float.isNaN(morph)||instant){morph=target;morphAt=now;return morph;}
        // Not drawn for a while (the switch was flipped in a menu): the morph plays from the start once it is seen.
        float gap=(now-morphAt)/1e9f,step=gap>.25f?0:gap/MORPH_S;morphAt=now;
        morph=target>morph?Math.min(target,morph+step):Math.max(target,morph-step);
        return morph;
    }
    /** Size and place: 0 full panel where it was put, 1 the notch at the top centre. Done before the notch flares. */
    float shape(){return ease(Math.clamp(morph()/.8f,0,1));}
    /** How far the top corners have flared into the screen edge. */
    float notch(){return smooth(Math.clamp((morph()-.62f)/.38f,0,1));}
    int width(){return Math.round(WIDTH+(miniWidth-WIDTH)*shape());}
    int height(){
        var m=module();
        float s=shape();
        int full=m==null?HEAD:HEAD+(lyricsShown(m,m.track())?LYRICS:0);
        return Math.round(full+(MINI_HEIGHT-full)*s);
    }
    /** Lyrics take room only once they are found: no "searching" or "not found" placeholders. */
    private boolean lyricsShown(MusicHud m,MediaTrack track){
        if(m==null||!m.showLyrics()||MusicHud.idle(track))return false;
        return track==null||m.lyrics(track).state()==LyricsService.State.FOUND;
    }

    private static MediaTrack sample(long now){
        return new MediaTrack("Playing","Spotify.exe","Spotify","Night Drive","Vela Sound","",now%SAMPLE_LENGTH,SAMPLE_LENGTH,0);
    }
    private Lyrics sampleLyrics(){
        if(sampleLyrics==null){
            var lines=new java.util.ArrayList<Lyrics.Line>();
            for(int i=0;i<50;i++)lines.add(new Lyrics.Line(i*4000L,Lang.tr(SAMPLE_LINES.get(i%SAMPLE_LINES.size()))));
            sampleLyrics=new Lyrics(List.copyOf(lines),true,false);
        }
        return sampleLyrics;
    }
    private Lyrics lyrics(MusicHud m,MediaTrack track,boolean sample){
        if(sample)return sampleLyrics();
        var result=m.lyrics(track);
        return result.state()==LyricsService.State.FOUND?result.lyrics():null;
    }
    private long position(MusicHud m,MediaTrack track,boolean sample,long now){return sample?track.position(now):m.position(now);}
    /**
     * Where the lyrics are: a little behind the reported position. Players stamp their position late (the stamp is
     * taken after the sample), so the extrapolated time runs ahead, and a line lit before it is sung reads worse than
     * one lit a moment after.
     */
    private long lyricsPosition(MusicHud m,MediaTrack track,boolean sample,long now){return Math.max(0,position(m,track,sample,now)-LYRICS_LAG_MS);}

    void draw(GuiGraphicsExtractor g,Minecraft client,boolean editing,Background panel){
        var m=module();
        long now=System.currentTimeMillis();
        MediaTrack track=m==null?null:m.track();
        boolean sample=false;
        if(track==null)return;
        // The HUD editor shows a sample song while nothing plays, so lyrics and the spectrum can be seen while placing it.
        if(editing&&MusicHud.idle(track)){track=sample(now);sample=true;}
        float s=shape();
        if(s>0){drawMorph(g,m,track,sample,now,panel,s);return;}
        drawFull(g,m,track,sample,now,panel);
    }
    /**
     * Mid-morph: the background takes the in-between size, and the content of the nearer mode is drawn inside it,
     * clipped to it, fading out towards the middle of the morph and the other mode's content fading in after it.
     */
    private void drawMorph(GuiGraphicsExtractor g,MusicHud m,MediaTrack track,boolean sample,long now,Background panel,float s){
        if(s>=1){drawMini(g,m,track,sample,now,panel);return;}
        int w=width(),h=height(),radius=Math.round(6+(MINI_RADIUS-6)*s);
        Background shaped=(height,r,notch)->panel.draw(h,radius,notch());
        UiClip.enable(g,0,0,w,h);
        try{
            if(s<.5f)drawFull(g,m,track,sample,now,shaped);else drawMini(g,m,track,sample,now,shaped);
        }finally{g.disableScissor();}
        // Hide the switch between the two layouts behind a short fade to the panel colour.
        float cover=smooth(Math.clamp((1-Math.abs(s-.5f)/.5f)*1.8f,0,1));
        if(cover>0)VelaDraw.roundedRect(g,0,0,w,h,radius,Math.round(230*cover)<<24|0x10141C);
    }
    private void drawFull(GuiGraphicsExtractor g,MusicHud m,MediaTrack track,boolean sample,long now,Background panel){
        if(track==null)return;
        int height=HEAD+(lyricsShown(m,sample?null:track)?LYRICS:0);
        panel.draw(height,6,0);
        var textures=m==null?null:m.textures();
        int accent=sample?SAMPLE_ACCENT:textures==null?FALLBACK_ACCENT:textures.accent();
        float enter=sample||m==null?1:ease(Math.clamp((now-m.songSince())/340f,0,1));
        int alpha=Math.round(255*enter);

        // A faint wash of the cover's colour behind the cover.
        VelaDraw.roundedRect(g,COVER_X-3,COVER_Y-3,COVER+6,COVER+6,11,0x1E000000|accent&0xFFFFFF);
        drawCover(g,m,track,sample,accent,alpha,COVER_X,COVER_Y,COVER,true);
        g.pose().pushMatrix();g.pose().translate(0,(1-enter)*5);
        drawInfo(g,m,track,sample,accent,alpha,now);
        g.pose().popMatrix();
        stepBars(m,track,sample,now);
        drawSpectrum(g,accent,8,SPECTRUM_Y,WIDTH-16,SPECTRUM_H);
        drawTimeline(g,m,track,sample,accent,now);
        if(height>HEAD)drawLyrics(g,m,track,sample,now,height);
    }

    // ---- full panel ----

    private void drawCover(GuiGraphicsExtractor g,MusicHud m,MediaTrack track,boolean sample,int accent,int alpha,int x,int y,int size,boolean badge){
        var textures=m==null?null:m.textures();
        var cover=sample||textures==null?null:textures.cover();
        float r=size*.2f;
        MusicBrand brand=sample?MusicBrand.SPOTIFY:MusicBrand.of(track);
        if(cover!=null)VelaDraw.blit(g,cover.id(),x,y,size,size,0,0,cover.width(),cover.height(),cover.width(),cover.height(),alpha<<24|0xFFFFFF);
        else{
            VelaDraw.roundedRect(g,x,y,size,size,Math.round(r),alpha<<24|shade(accent,.22f));
            VelaDraw.roundedOutline(g,x,y,size,size,Math.round(r),(alpha*40/255)<<24|accent&0xFFFFFF,0);
            if(textures!=null){var note=textures.note();int n=size/2;VelaDraw.blit(g,note.id(),x+(size-n)/2,y+(size-n)/2,n,n,0,0,note.width(),note.height(),note.width(),note.height(),alpha<<24|accent&0xFFFFFF);}
        }
        if(track!=null&&track.paused()&&badge){
            VelaDraw.roundedRect(g,x,y,size,size,Math.round(r),0x8C000000);
            glyph(g,textures,MusicGlyph.PAUSE,x+size/2-8,y+size/2-8,16,0xF0FFFFFF);
        }
        if(!badge||track==null||MusicHud.idle(track))return;
        // The service's logo sits on the cover's corner in a dark ring, like a messenger avatar badge.
        int bx=x+size-13,by=y+size-13;
        VelaDraw.roundedRect(g,bx-2,by-2,20,20,10,0xFF10141C);
        MusicTextures.Icon icon=textures==null?null:brand!=null?textures.brand(brand):null;
        if(icon==null&&textures!=null&&!sample)icon=textures.app(track.app());
        if(icon!=null)VelaDraw.blit(g,icon.id(),bx+1,by+1,14,14,0,0,icon.width(),icon.height(),icon.width(),icon.height(),0xFFFFFFFF);
        else{
            VelaDraw.roundedRect(g,bx+1,by+1,14,14,7,0xFF000000|shade(accent,.55f));
            String name=MusicBrand.source(track);
            String letter=name.isEmpty()?"?":name.substring(0,name.offsetByCodePoints(0,1)).toUpperCase(Locale.ROOT);
            float lw=VelaDraw.literalWidth(letter,VelaDraw.Weight.SEMIBOLD,8);
            VelaDraw.literalText(g,letter,bx+8-lw/2,by+3.5f,0xFFFFFFFF,VelaDraw.Weight.SEMIBOLD,8);
        }
    }

    private void drawInfo(GuiGraphicsExtractor g,MusicHud m,MediaTrack track,boolean sample,int accent,int alpha,long now){
        int a=alpha<<24;
        boolean muted=!sample&&MusicBrand.muted(track);
        if(muted){
            // "Sound off" chip at the top right: the tab or app is muted, so the song plays silently.
            String label=Lang.tr("Звук выкл");
            float lw=VelaDraw.literalWidth(label,VelaDraw.Weight.MEDIUM,6.5f);
            int cw=Math.round(lw)+17,cx=RIGHT-cw;
            VelaDraw.roundedRect(g,cx,7,cw,11,5,(alpha*60/255)<<24|0xFF5A6E&0xFFFFFF);
            glyph(g,m==null?null:m.textures(),MusicGlyph.MUTED,cx+3,8,9,a|0xFFB4BE);
            VelaDraw.literalText(g,label,cx+13,9,a|0xFFD0D6,VelaDraw.Weight.MEDIUM,6.5f);
        }
        // The service shows as the badge on the cover; the title drops under the mute chip when there is one.
        float titleY=muted?20:14;
        marquee(g,MusicHud.idle(track)?Lang.tr(track.title()):track.title(),TEXT_X,titleY,RIGHT-TEXT_X,a|VelaTheme.TEXT&0xFFFFFF,9.5f,VelaDraw.Weight.MEDIUM,now-(m==null?0:m.songSince()));
        String artist=!track.artist().isEmpty()?track.artist():track.album();
        if(!artist.isEmpty())VelaDraw.literalText(g,fit(artist,RIGHT-TEXT_X,VelaDraw.Weight.REGULAR,8),TEXT_X,titleY+12,a|VelaTheme.TEXT_SECONDARY&0xFFFFFF,VelaDraw.Weight.REGULAR,8);
    }

    private void drawTimeline(GuiGraphicsExtractor g,MusicHud m,MediaTrack track,boolean sample,int accent,long now){
        int barW=WIDTH-16;
        int muted=VelaTheme.TEXT_MUTED;
        if(track.hasTimeline()){
            long position=position(m,track,sample,now);
            float done=Math.clamp(position/(float)track.durationMs(),0,1);
            VelaDraw.roundedRect(g,8,BAR_Y,barW,3,1,0x30FFFFFF);
            int fill=Math.round(barW*done);
            if(fill>0)VelaDraw.roundedRect(g,8,BAR_Y,Math.max(3,fill),3,1,0xFF000000|accent);
            VelaDraw.roundedRect(g,8+fill-2,BAR_Y-1,5,5,2,0xFFFFFFFF);
            String left=time(position),rightTime=time(track.durationMs());
            VelaDraw.literalText(g,left,8,CONTROLS_Y+3,muted,VelaDraw.Weight.REGULAR,6.5f);
            VelaDraw.literalText(g,rightTime,RIGHT-VelaDraw.literalWidth(rightTime,VelaDraw.Weight.REGULAR,6.5f),CONTROLS_Y+3,muted,VelaDraw.Weight.REGULAR,6.5f);
        }else{
            // Live streams and players without a timeline: a pulsing dot instead of a bar.
            float pulse=track.playing()?.55f+.45f*(float)Math.sin(now/1000.0*4):.4f;
            VelaDraw.roundedRect(g,8,BAR_Y,barW,3,1,0x18FFFFFF);
            VelaDraw.roundedRect(g,8,CONTROLS_Y+4,5,5,2,Math.round(pulse*255)<<24|accent&0xFFFFFF);
            VelaDraw.text(g,track.playing()?"Сейчас играет":MusicHud.idle(track)?"Тишина":"Пауза",17,CONTROLS_Y+3,0xFF000000|accent,VelaDraw.Weight.MEDIUM,6.5f);
        }
        var textures=m==null?null:m.textures();
        for(var button:buttons(track)){
            boolean enabled=sample||button.enabled;
            boolean hover=enabled&&hoverX>=button.x-2&&hoverX<button.x+button.size+2&&hoverY>=CONTROLS_Y-2&&hoverY<CONTROLS_Y+button.size+2;
            float press=button.command.equals(pressed)?1-Math.clamp((now-pressedAt)/220f,0,1):0;
            if(hover||press>0)VelaDraw.roundedRect(g,button.x-3,CONTROLS_Y-3+(BUTTON-button.size)/2,button.size+6,button.size+6,(button.size+6)/2,(hover?0x22:0)+Math.round(press*0x30)<<24|0xFFFFFF);
            int color=!enabled?0x50FFFFFF:button.main?0xFFFFFFFF:0xD8FFFFFF;
            glyph(g,textures,button.glyph,button.x,CONTROLS_Y+(BUTTON-button.size)/2,button.size,color);
        }
    }
    private record Button(String command,MusicGlyph glyph,int x,int size,boolean enabled,boolean main){}
    private static List<Button> buttons(MediaTrack track){
        int cx=WIDTH/2;
        return List.of(
            new Button("prev",MusicGlyph.PREV,cx-26,10,track.canPrev(),false),
            new Button("toggle",track.playing()?MusicGlyph.PAUSE:MusicGlyph.PLAY,cx-BUTTON/2,BUTTON,track.canToggle(),true),
            new Button("next",MusicGlyph.NEXT,cx+16,10,track.canNext(),false));
    }

    // ---- spectrum ----

    /** Steps the shown bars towards the live spectrum: quick to rise, slower to fall, kicked up on every beat. */
    private void stepBars(MusicHud m,MediaTrack track,boolean sample,long now){
        float dt=barsAt==0?0:Math.min(.1f,(now-barsAt)/1000f);barsAt=now;
        boolean live=!sample&&m!=null&&m.hearing()&&track!=null&&track.playing()&&!MusicBrand.muted(track);
        float[] bands=live?m.bands():new float[0];
        float level=live?m.level():0;
        // The helper sends a new array about every 40 ms; frames in between reuse it.
        if(live&&bands!=fedBands){fedBands=bands;if(bands.length>1)meter.feed(bands,now);else meter.feedLevel(level,now);}
        if(!live&&fedBands!=null){fedBands=null;meter.reset();}
        float pulse=live?meter.pulse(now):0;
        float[] heights=meter.heights();
        for(int i=0;i<BARS;i++){
            float target;
            if(sample)target=.25f+.6f*Math.abs((float)Math.sin(now/1000.0*(2.3+i%7*.31)+i*.7))*(1-.5f*i/BARS);
            else if(!live)target=.06f;
            else if(bands.length>1&&heights.length==bands.length){
                float at=i*(heights.length-1)/(float)(BARS-1);int lo=(int)at,hi=Math.min(heights.length-1,lo+1);
                float v=heights[lo]+(heights[hi]-heights[lo])*(at-lo);
                // A beat lifts every bar, the low ones most, so the kick reads across the whole strip.
                target=Math.clamp(v*(.82f+.18f*pulse)+pulse*.32f*(1-.6f*i/BARS),.04f,1);
            }else{
                // Only the loudness is known: a moving shape scaled by how loud it is now against its average.
                float wave=.5f+.5f*(float)Math.sin(now/1000.0*(3.1+i%5*.6)+i*1.3);
                target=Math.clamp(meter.relativeLevel(level)*(.4f+.6f*wave)*(1-.35f*i/BARS)+pulse*.45f,.04f,1);
            }
            float rate=target>bars[i]?32:9;
            bars[i]+=(target-bars[i])*(1-(float)Math.exp(-rate*dt));
            if(dt==0)bars[i]=target;
        }
    }
    private void drawSpectrum(GuiGraphicsExtractor g,int accent,int x,int y,int width,int height){
        float step=width/(float)BARS;int bw=Math.max(2,Math.round(step*.62f));
        for(int i=0;i<BARS;i++){
            float v=bars[i];
            int bh=Math.max(2,Math.round(height*v));
            int alpha=Math.round(255*(.42f+.58f*v));
            // Plain fills: a 1 px corner on a 2 px bar is invisible, and forty rounded quads a frame were the HUD's top allocation.
            int bx=Math.round(x+i*step+(step-bw)/2);
            g.fill(bx,y+height-bh,bx+bw,y+height,alpha<<24|accent&0xFFFFFF);
        }
    }
    /** {@code count} bars made of the spectrum's groups, for the mini pill. */
    private float[] grouped(int count){
        float[] out=new float[count];
        for(int i=0;i<BARS;i++)out[i*count/BARS]=Math.max(out[i*count/BARS],bars[i]);
        return out;
    }

    // ---- lyrics ----

    private void drawLyrics(GuiGraphicsExtractor g,MusicHud m,MediaTrack track,boolean sample,long now,int height){
        var lyrics=lyrics(m,track,sample);
        if(lyrics==null)return;
        VelaDraw.hairline(g,8,HEAD-.5f,WIDTH-16,0x24FFFFFF);
        int color=sample?SAMPLE_ACCENT:m.lyricsColor();
        float center=HEAD+LYRICS/2f-4.5f;
        long position=lyricsPosition(m,track,sample,now);
        int index=lyrics.index(position,track.durationMs());
        float target=stepView(track,index,lyrics.lines().size(),now);
        UiClip.enable(g,4,HEAD+1,WIDTH-4,height-2);
        try{
            for(int i=Math.round(target)-3;i<=Math.round(target)+3;i++){
                float y=center+(i-view)*LINE;
                float distance=Math.min(1.6f,Math.abs(y-center)/LINE);
                float fade=Math.max(0,1-.58f*distance);
                if(fade<=.02f)continue;
                boolean current=i==index;
                String text=i<0?(i==-1?"":null):i<lyrics.lines().size()?lyrics.lines().get(i).text():null;
                if(text==null)continue;
                if(text.isEmpty()){
                    // An instrumental break (or the intro): three breathing dots.
                    for(int d=0;d<3;d++){
                        float breathe=current?.45f+.55f*(float)Math.abs(Math.sin(now/1000.0*2.6-d*.6)):.4f;
                        int c=Math.round(255*fade*breathe)<<24|(current?color:VelaTheme.TEXT_MUTED)&0xFFFFFF;
                        VelaDraw.roundedRect(g,WIDTH/2-7+d*6,Math.round(y+3),3,3,1,c);
                    }
                    continue;
                }
                float size=current?9:8;VelaDraw.Weight weight=current?VelaDraw.Weight.MEDIUM:VelaDraw.Weight.REGULAR;
                float sung=current&&lyrics.synced()?lyrics.sung(index,position):current?1:0;
                lyricLine(g,text,10,WIDTH-10,y,size,weight,current?VelaTheme.TEXT:VelaTheme.TEXT_MUTED,color,fade,current,sung,lyrics.synced(),now);
            }
        }finally{g.disableScissor();}
    }
    /** Eases the view towards the sung line, or the line the wheel moved to; returns where it is headed. */
    private float stepView(MediaTrack track,int index,int count,long now){
        if(scroll!=0&&now-scrolledAt>SCROLL_RESET_MS)scroll=0;
        float target=Math.clamp(index+scroll,-1,Math.max(-1,count-1));
        if(!track.sameSong(viewSong)||Float.isNaN(view)){viewSong=track;view=target;scroll=0;viewAt=now;}
        float dt=Math.min(.1f,Math.max(0,(now-viewAt)/1000f));viewAt=now;
        if(Math.abs(target-view)>6)view=target-Math.signum(target-view)*6; // a seek: do not roll through dozens of lines
        view+=(target-view)*(1-(float)Math.exp(-12*dt));
        return target;
    }
    /**
     * One lyric line between {@code left} and {@code right}: centred when it fits, otherwise the sung line slides
     * along with the singing so the words being sung stay in view; other long lines end in "…".
     */
    private static void lyricLine(GuiGraphicsExtractor g,String text,float left,float right,float y,float size,VelaDraw.Weight weight,
                                  int base,int color,float fade,boolean current,float sung,boolean synced,long now){
        float avail=right-left,w=VelaDraw.literalWidth(text,weight,size);
        String line=text;float x;
        if(w<=avail)x=left+(avail-w)/2;
        else if(current){
            float over=w-avail;
            float t=synced?Math.clamp((sung-.18f)/.64f,0,1):(float)(.5-.5*Math.cos(now/1000.0*.9));
            x=left-over*smooth(t);
        }else{line=fit(text,avail,weight,size);w=VelaDraw.literalWidth(line,weight,size);x=left+(avail-w)/2;}
        int a=Math.round(255*fade)<<24;
        boolean clip=current&&w>avail;
        if(clip)UiClip.enable(g,(int)left,Math.round(y)-2,(int)Math.ceil(right),Math.round(y+size+4));
        try{
            VelaDraw.literalText(g,line,x,y,a|base&0xFFFFFF,weight,size);
            if(current){
                // The sung part of the line fills with the chosen colour, up to the character being sung (all of it
                // for plain text, whose words have no times).
                float[] offsets=VelaDraw.literalOffsets(line,weight,size);
                float at=sung*line.length();int i=Math.min((int)at,line.length());
                float reach=i>=line.length()?offsets[line.length()]:offsets[i]+(offsets[i+1]-offsets[i])*(at-i);
                int edge=(int)Math.ceil(x+reach);
                int from=(int)Math.floor(Math.max(x,clip?left:x))-1,to=clip?Math.min(edge,(int)Math.ceil(right)):edge;
                if(to>from){
                    UiClip.enable(g,from,Math.round(y)-2,to,Math.round(y+size+4));
                    try{VelaDraw.literalText(g,line,x,y,a|color&0xFFFFFF,weight,size);}finally{g.disableScissor();}
                }
            }
        }finally{if(clip)g.disableScissor();}
    }

    // ---- mini pill ----

    private void drawMini(GuiGraphicsExtractor g,MusicHud m,MediaTrack track,boolean sample,long now,Background panel){
        Lyrics lyrics=track==null||!m.showLyrics()||MusicHud.idle(track)?null:lyrics(m,track,sample);
        long position=track==null?0:lyricsPosition(m,track,sample,now);
        int index=lyrics==null?-1:lyrics.index(position,track.durationMs());
        String text=index>=0&&index<lyrics.lines().size()?lyrics.lines().get(index).text():"";
        float dt=miniAt==0?0:Math.min(.1f,(now-miniAt)/1000f);
        int targetWidth=lyrics!=null?MINI_WIDE:MINI_SHORT;
        miniWidth+=(targetWidth-miniWidth)*(1-(float)Math.exp(-9*dt));
        if(Math.abs(targetWidth-miniWidth)<.5f)miniWidth=targetWidth;
        miniAt=now;
        if(lyrics!=null)stepView(track,index,lyrics.lines().size(),now);
        int w=width(),h=MINI_HEIGHT;
        // Softer bottom corners than a full pill leave the side a straight run for the shoulders to flow into.
        panel.draw(h,MINI_RADIUS,notch());
        var textures=m.textures();
        int accent=sample?SAMPLE_ACCENT:track==null?FALLBACK_ACCENT:textures.accent();
        drawCover(g,m,track,sample,accent,track==null||MusicHud.idle(track)?140:255,4,4,16,false);
        if(track!=null&&track.paused())glyph(g,textures,MusicGlyph.PAUSE,7,7,10,0xE0FFFFFF);
        stepBars(m,track,sample,now);
        float[] groups=grouped(MINI_BARS);
        int bx=w-10-(MINI_BARS*3+(MINI_BARS-1)*2);
        for(int i=0;i<MINI_BARS;i++){
            int bh=Math.max(2,Math.round(12*groups[i]));
            VelaDraw.roundedRect(g,bx+i*5,h/2-bh/2,3,bh,1,0xFF000000|accent);
        }
        if(lyrics==null||w<MINI_WIDE*.6f)return;
        // The sung line, between the cover and the bars, fading in as the pill opens.
        float open=Math.clamp((w-MINI_SHORT)/(float)(MINI_WIDE-MINI_SHORT),0,1);
        float left=26,right=bx-6;
        UiClip.enable(g,(int)left,1,(int)right,h-1);
        try{
            if(text.isEmpty()){
                for(int d=0;d<3;d++){
                    float breathe=.45f+.55f*(float)Math.abs(Math.sin(now/1000.0*2.6-d*.6));
                    VelaDraw.roundedRect(g,Math.round((left+right)/2)-7+d*6,h/2-1,3,3,1,Math.round(255*open*breathe)<<24|m.lyricsColor()&0xFFFFFF);
                }
            }else{
                float sung=lyrics.synced()?lyrics.sung(index,position):1;
                // The wheel can step through lines here too; the line shown is then the scrolled one.
                int shown=Math.clamp(index+scroll,0,lyrics.lines().size()-1);
                String line=shown==index?text:lyrics.lines().get(shown).text();
                lyricLine(g,line,left,right,h/2f-4,8,VelaDraw.Weight.MEDIUM,VelaTheme.TEXT,sample?SAMPLE_ACCENT:m.lyricsColor(),open,shown==index,sung,lyrics.synced(),now);
            }
        }finally{g.disableScissor();}
    }

    // ---- input (chat open) ----

    void hover(float x,float y){hoverX=x;hoverY=y;}
    /** A click at panel-local ({@code x},{@code y}); true when it hit a control. */
    boolean click(float x,float y){
        var m=module();if(m==null)return false;
        var track=m.track();if(track==null)return false;
        if(m.mini()){
            if(x<0||y<0||x>width()||y>MINI_HEIGHT||!track.canToggle())return false;
            press(m,"toggle");return true;
        }
        if(track.hasTimeline()&&y>=BAR_Y-4&&y<=BAR_Y+7&&x>=4&&x<=WIDTH-4){
            m.seek(Math.round(Math.clamp((x-8)/(WIDTH-16f),0,1)*track.durationMs()));return true;
        }
        if(y<CONTROLS_Y-3||y>CONTROLS_Y+BUTTON+3)return false;
        for(var button:buttons(track))if(button.enabled&&x>=button.x-4&&x<button.x+button.size+4){press(m,button.command);return true;}
        return false;
    }
    private void press(MusicHud m,String command){m.command(command);pressed=command;pressedAt=System.currentTimeMillis();}
    /** The wheel over the lyrics steps through the lines; true when it was used. */
    boolean scroll(float x,float y,double amount){
        var m=module();if(m==null||!m.showLyrics()||amount==0)return false;
        var track=m.track();if(track==null||MusicHud.idle(track)||m.lyrics(track).state()!=LyricsService.State.FOUND)return false;
        if(x<0||x>width())return false;
        if(m.mini()?y<0||y>MINI_HEIGHT:y<HEAD||y>HEAD+LYRICS)return false;
        scroll+=amount>0?-1:1;scrolledAt=System.currentTimeMillis();
        return true;
    }

    // ---- helpers ----

    private static void glyph(GuiGraphicsExtractor g,MusicTextures textures,MusicGlyph glyph,int x,int y,int size,int color){
        if(textures==null)return;
        var icon=textures.glyph(glyph);
        VelaDraw.blit(g,icon.id(),x,y,size,size,0,0,icon.width(),icon.height(),icon.width(),icon.height(),color);
    }
    /** One line that slides left and back when it does not fit, pausing at the start of each pass. */
    private static void marquee(GuiGraphicsExtractor g,String text,float x,float y,float width,int color,float size,VelaDraw.Weight weight,long age){
        float textWidth=VelaDraw.literalWidth(text,weight,size);
        if(textWidth<=width){VelaDraw.literalText(g,text,x,y,color,weight,size);return;}
        float gap=28,travel=textWidth+gap,speed=24;
        long pause=2200,cycle=pause+(long)(travel/speed*1000);
        long phase=Math.floorMod(age,cycle);
        float shift=phase<pause?0:(phase-pause)/1000f*speed;
        UiClip.enable(g,(int)x,(int)y-2,(int)Math.ceil(x+width),(int)Math.ceil(y+size+4));
        try{
            VelaDraw.literalText(g,text,x-shift,y,color,weight,size);
            if(shift>0)VelaDraw.literalText(g,text,x-shift+travel,y,color,weight,size);
        }finally{g.disableScissor();}
    }
    /** {@code text} cut with "…" to fit; song names are shown as they are, without translation. */
    static String fit(String text,float width,VelaDraw.Weight weight,float size){
        if(VelaDraw.literalWidth(text,weight,size)<=width)return text;
        int[] points=text.codePoints().toArray();
        int low=0,high=points.length-1;
        while(low<high){
            int mid=(low+high+1)>>>1;
            if(VelaDraw.literalWidth(new String(points,0,mid).stripTrailing()+"…",weight,size)<=width)low=mid;else high=mid-1;
        }
        return new String(points,0,low).stripTrailing()+"…";
    }
    static String time(long ms){
        long s=ms/1000;
        return s>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",s/3600,s/60%60,s%60):String.format(Locale.ROOT,"%d:%02d",s/60,s%60);
    }
    private static float ease(float t){float u=1-t;return 1-u*u*u;}
    private static float smooth(float t){return t*t*(3-2*t);}
    /** {@code color} darkened to {@code amount} of its brightness, as RGB. */
    private static int shade(int color,float amount){
        int r=Math.round((color>>16&255)*amount),gr=Math.round((color>>8&255)*amount),b=Math.round((color&255)*amount);
        return r<<16|gr<<8|b;
    }
}
