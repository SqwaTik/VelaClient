package dev.sqwat.vela.media;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Song text: timed lines from an LRC file ({@code [01:23.45] line}). Empty lines of the file are kept as pauses between
 * verses, and a pause is added where a line would otherwise hang on screen long after it was sung. Plain text without
 * times is the fallback for songs no source has timed: its lines are spread evenly over the song.
 * <p>
 * Every sung line knows how far into its text the singing is at each moment ({@link #sung}): from the word stamps of
 * "enhanced" LRC ({@code <00:12.34>}) where the source has them, otherwise estimated from the line's syllables at the
 * song's own pace, so a word held long or a fast verse is followed and a pause at the end of a line does not stretch
 * the words before it.
 */
public record Lyrics(List<Line> lines,boolean synced,boolean instrumental){
    /**
     * One line. {@code keyChars}/{@code keyTimes}: at {@code keyTimes[k]} the singing has reached character
     * {@code keyChars[k]}; between keys it moves on evenly, two keys at the same character hold it there. Null for
     * pauses and plain text.
     */
    public record Line(long timeMs,String text,int[] keyChars,long[] keyTimes){
        public Line(long timeMs,String text){this(timeMs,text,null,null);}
    }
    public static final Lyrics NONE=new Lyrics(List.of(),false,false);
    public static final Lyrics INSTRUMENTAL=new Lyrics(List.of(),false,true);
    public boolean empty(){return lines.isEmpty();}

    private static final Pattern STAMP=Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]");
    /** Word timings of "enhanced" LRC ({@code <00:12.34>}). */
    private static final Pattern WORD_STAMP=Pattern.compile("<(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?>");
    /** {@code [offset:+250]}: milliseconds the whole text should come earlier (positive) or later. */
    private static final Pattern OFFSET=Pattern.compile("(?im)^\\s*\\[offset:\\s*([+-]?\\d+)\\s*]");
    /** Latin and Cyrillic vowels (escaped: the Cyrillic ones are not interface text). */
    private static final String VOWELS="aeiouyàáâãäåæèéêëìíîïòóôõöøùúûüýÿ"
        +"\u0430\u0435\u0451\u0438\u043e\u0443\u044b\u044d\u044e\u044f\u0456\u0457\u0454\u04e9\u04af";
    /** Longest a line stays lit after it starts, by its length; past that a pause shows until the next line. */
    static long hold(String text){return Math.clamp(2000+text.length()*160L,3500,10_000);}
    /**
     * A sung line followed by a gap stays lit {@link #AFTER_SUNG_MS} past its last word (and {@link #MIN_LIT_MS}
     * at least); a gap left of {@link #MIN_PAUSE_MS} or more shows the pause dots until the next line.
     */
    static final long AFTER_SUNG_MS=900,MIN_LIT_MS=1500,MIN_PAUSE_MS=1800;

    public static Lyrics parseSynced(String lrc){
        if(lrc==null||lrc.isBlank())return NONE;
        var lines=new ArrayList<Line>();
        Matcher offsetTag=OFFSET.matcher(lrc);
        long offset=offsetTag.find()?Math.clamp(Long.parseLong(offsetTag.group(1)),-60_000,60_000):0;
        for(String raw:lrc.split("\\R")){
            Matcher m=STAMP.matcher(raw);int end=0;var times=new ArrayList<Long>();
            while(m.find()&&m.start()==end){times.add(Math.max(0,ms(m)-offset));end=m.end();}
            if(times.isEmpty())continue; // [ar:…] tags and stray text
            var words=words(raw.substring(end),offset);
            for(long time:times){
                // A line repeated under several stamps: its word times move along with each repeat.
                long[] keyTimes=null;
                if(words.times!=null){
                    keyTimes=words.times.clone();long shift=time-times.getFirst();
                    for(int k=0;k<keyTimes.length;k++)keyTimes[k]=Math.max(0,keyTimes[k]+shift);
                }
                lines.add(new Line(time,words.text,words.chars,keyTimes));
            }
        }
        lines.sort(java.util.Comparator.comparingLong(Line::timeMs));
        // Drop leading pauses, keep at most one empty line in a row.
        var clean=new ArrayList<Line>();
        for(var line:lines){
            if(line.text.isEmpty()&&(clean.isEmpty()||clean.getLast().text.isEmpty()))continue;
            clean.add(line);
        }
        while(!clean.isEmpty()&&clean.getLast().text.isEmpty())clean.removeLast();
        double rate=pace(clean);
        // A long instrumental gap after a line: the line ends shortly after it was sung and a pause fills the rest.
        var paced=new ArrayList<Line>();
        for(int i=0;i<clean.size();i++){
            var line=clean.get(i);
            if(line.text.isEmpty()){paced.add(line);continue;}
            long next=i+1<clean.size()?clean.get(i+1).timeMs:line.timeMs+hold(line.text);
            line=keyed(line,next,rate);paced.add(line);
            if(i+1>=clean.size()||clean.get(i+1).text.isEmpty())continue;
            long end=Math.max(line.timeMs+MIN_LIT_MS,line.keyTimes[line.keyTimes.length-1]+AFTER_SUNG_MS);
            if(next-end>=MIN_PAUSE_MS)paced.add(new Line(end,""));
        }
        return paced.isEmpty()?NONE:new Lyrics(List.copyOf(paced),true,false);
    }
    /** Share of plain text's spread a verse break takes, in syllables. */
    private static final int BREAK_SYLLABLES=6;
    /**
     * Plain text: its lines are spread over the song by their syllables, and a blank line between verses becomes a
     * pause. {@code timeMs} of each line is then its start as millionths of the spread ({@link #index}).
     */
    public static Lyrics parsePlain(String text){
        if(text==null||text.isBlank())return NONE;
        var texts=new ArrayList<String>();
        for(String raw:text.split("\\R")){
            String line=raw.strip();
            if(line.isEmpty()&&(texts.isEmpty()||texts.getLast().isEmpty()))continue;
            texts.add(line);
        }
        while(!texts.isEmpty()&&texts.getLast().isEmpty())texts.removeLast();
        if(texts.isEmpty())return NONE;
        double total=0;for(var t:texts)total+=t.isEmpty()?BREAK_SYLLABLES:syllables(t);
        var lines=new ArrayList<Line>();double at=0;
        for(var t:texts){lines.add(new Line(Math.round(at/total*1_000_000),t));at+=t.isEmpty()?BREAK_SYLLABLES:syllables(t);}
        return new Lyrics(List.copyOf(lines),false,false);
    }
    private static long ms(Matcher m){
        long ms=Long.parseLong(m.group(1))*60_000+Long.parseLong(m.group(2))*1000;
        String fraction=m.group(3);
        if(fraction!=null)ms+=Math.round(Long.parseLong(fraction)*Math.pow(10,3-fraction.length()));
        return ms;
    }
    private record Words(String text,int[] chars,long[] times){}
    /** The text after a line's stamps with runs of spaces made one, its word stamps turned into keys. */
    private static Words words(String rest,long offset){
        var text=new StringBuilder();var chars=new ArrayList<Integer>();var times=new ArrayList<Long>();
        Matcher m=WORD_STAMP.matcher(rest);int at=0;
        while(m.find()){
            append(text,rest.substring(at,m.start()));
            long time=Math.max(0,ms(m)-offset);
            if(!times.isEmpty())time=Math.max(time,times.getLast());
            chars.add(text.length());times.add(time);at=m.end();
        }
        append(text,rest.substring(at));
        while(!text.isEmpty()&&text.charAt(text.length()-1)==' ')text.setLength(text.length()-1);
        String line=text.toString();
        if(times.isEmpty()||line.isEmpty())return new Words(line,null,null);
        return new Words(line,chars.stream().mapToInt(c->Math.min(c,line.length())).toArray(),times.stream().mapToLong(Long::longValue).toArray());
    }
    private static void append(StringBuilder out,String part){
        for(int i=0;i<part.length();i++){
            char c=part.charAt(i);
            if(!Character.isWhitespace(c))out.append(c);
            else if(!out.isEmpty()&&out.charAt(out.length()-1)!=' ')out.append(' ');
        }
    }
    /** How much of the singing a character takes: a vowel (or a CJK syllable) the most, other letters a little. */
    static double weight(char c){
        if(VOWELS.indexOf(Character.toLowerCase(c))>=0||Character.isIdeographic(c))return 1;
        var script=Character.UnicodeScript.of(c);
        if(script==Character.UnicodeScript.HIRAGANA||script==Character.UnicodeScript.KATAKANA||script==Character.UnicodeScript.HANGUL)return 1;
        return Character.isLetterOrDigit(c)?.3:.05;
    }
    static int syllables(String text){
        int n=0;for(int i=0;i<text.length();i++)if(weight(text.charAt(i))>=1)n++;
        return Math.max(1,n);
    }
    /**
     * Milliseconds a syllable takes in this song: the median over lines sung back to back. A line followed by a pause
     * says nothing about the pace and is left out.
     */
    static double pace(List<Line> lines){
        var rates=new ArrayList<Double>();
        for(int i=0;i+1<lines.size();i++){
            var line=lines.get(i);if(line.text.isEmpty()||lines.get(i+1).text.isEmpty())continue;
            long gap=lines.get(i+1).timeMs-line.timeMs;
            if(gap>0&&gap<10_000)rates.add(gap/(double)syllables(line.text));
        }
        if(rates.isEmpty())return 260;
        rates.sort(null);
        return Math.clamp(rates.get(rates.size()/2),110,600);
    }
    /** Milliseconds {@code text} takes to sing at {@code rate}, at most until {@code limit}. */
    private static long span(String text,double rate,long limit){
        return Math.max(1,Math.min(limit,Math.max(400,Math.round(syllables(text)*rate*1.1))));
    }
    /**
     * Milliseconds a line without word stamps is swept over. A line sung up to the next one (its gap is about what
     * its syllables take) fills that gap, short of the breath before the next line; a line followed by a pause takes
     * what its syllables take at the song's pace.
     */
    static long sweep(String text,double rate,long gap){
        double natural=syllables(text)*rate;
        if(gap<=natural*1.5+600)return Math.max(Math.min(gap,400),gap-Math.min(350,Math.round(gap*.12)));
        return span(text,rate,gap);
    }
    /**
     * Gives a sung line keys up to its end. Without word stamps the singing is spread over {@link #sweep}, each
     * character by its weight. Words after the last stamp get their time from the song's pace.
     */
    private static Line keyed(Line line,long next,double rate){
        String text=line.text;int length=text.length();
        if(line.keyChars==null){
            long end=line.timeMs+Math.max(1,sweep(text,rate,next-line.timeMs));
            double total=0;for(int i=0;i<length;i++)total+=weight(text.charAt(i));
            int[] chars=new int[length+1];long[] times=new long[length+1];double sum=0;
            for(int i=0;i<=length;i++){
                chars[i]=i;times[i]=line.timeMs+Math.round((end-line.timeMs)*(total<=0?i/(double)length:sum/total));
                if(i<length)sum+=weight(text.charAt(i));
            }
            return new Line(line.timeMs,text,chars,times);
        }
        var chars=new ArrayList<Integer>();var times=new ArrayList<Long>();
        // Words before the first stamp are sung from the line's start.
        if(line.keyChars[0]>0&&line.keyTimes[0]>line.timeMs){chars.add(0);times.add(line.timeMs);}
        for(int k=0;k<line.keyChars.length;k++){chars.add(line.keyChars[k]);times.add(line.keyTimes[k]);}
        int last=chars.getLast();
        if(last<length){
            long from=times.getLast();
            chars.add(length);times.add(from+span(text.substring(last),rate,Math.max(1,next-from)));
        }
        return new Line(line.timeMs,text,chars.stream().mapToInt(Integer::intValue).toArray(),times.stream().mapToLong(Long::longValue).toArray());
    }

    /**
     * Index of the line sung at {@code positionMs}, or -1 before the first one. Plain text has no times: its lines
     * are spread over the song by their length, skipping a short intro and outro.
     */
    public int index(long positionMs,long durationMs){
        if(lines.isEmpty())return -1;
        if(!synced){
            if(durationMs<=0)return 0;
            double start=durationMs*.06,end=durationMs*.94;
            positionMs=Math.round(Math.clamp((positionMs-start)/(end-start),0,1)*1_000_000);
        }
        int low=0,high=lines.size()-1,found=synced?-1:0;
        while(low<=high){int mid=(low+high)>>>1;if(lines.get(mid).timeMs<=positionMs){found=mid;low=mid+1;}else high=mid-1;}
        return found;
    }
    /**
     * Whether timed text can really belong to a song of {@code durationMs} (0 = unknown). LRCLIB has entries with one
     * or two stamps for the whole song, stamps far past its end, or one letter per line: shown, they hang for half a
     * minute or show text that is not being sung.
     */
    public boolean plausible(long durationMs){
        if(!synced)return false;
        var sung=lines.stream().filter(l->!l.text.isEmpty()).toList();
        if(sung.size()<6)return false;
        double letters=sung.stream().mapToInt(l->l.text.codePointCount(0,l.text.length())).average().orElse(0);
        if(letters<3)return false;
        if(durationMs>0){
            long first=sung.getFirst().timeMs,last=sung.getLast().timeMs;
            if(last>durationMs+5000)return false;
            if(last-first<durationMs*.35)return false;
        }
        return true;
    }
    /** 0..1 of the time from the line's start to the next line's. */
    public float lineProgress(int index,long positionMs){
        if(!synced||index<0||index>=lines.size())return 1;
        long start=lines.get(index).timeMs,end=index+1<lines.size()?lines.get(index+1).timeMs:start+hold(lines.get(index).text);
        return end<=start?1:(float)Math.clamp((positionMs-start)/(double)(end-start),0,1);
    }
    /** 0..1 of the line's characters sung at {@code positionMs}, for the highlight sweep. */
    public float sung(int index,long positionMs){
        if(!synced||index<0||index>=lines.size())return 1;
        var line=lines.get(index);
        if(line.keyChars==null||line.text.isEmpty())return lineProgress(index,positionMs);
        int[] chars=line.keyChars;long[] times=line.keyTimes;
        int k=-1;for(int i=0;i<times.length&&times[i]<=positionMs;i++)k=i;
        double at;
        if(k<0)at=0;
        else if(k==times.length-1)at=chars[k];
        else{
            long dt=times[k+1]-times[k];
            at=dt<=0?chars[k+1]:chars[k]+(chars[k+1]-chars[k])*(positionMs-times[k])/(double)dt;
        }
        return (float)Math.clamp(at/line.text.length(),0,1);
    }

    /** What to search for: cleaned title and artist, and the title split as "Artist - Title" (video titles). */
    public record Query(String artist,String title){}
    private static final Pattern NOISE=Pattern.compile("(?i)[(\\[{【](?:[^)\\]}】]*?)(official|video|audio|lyric|lyrics|visuali[sz]er|music video|mv|m/v|hd|hq|4k|remaster(?:ed)?|live|клип|премьера|текст|lyric video|prod\\.?|explicit|clean)(?:[^)\\]}】]*)[)\\]}】]");
    private static final Pattern FEAT=Pattern.compile("(?i)\\s*[(\\[]?\\s*(?:feat\\.?|ft\\.?|featuring|при уч\\.?)\\s[^)\\]]*[)\\]]?");
    private static final Pattern TAIL=Pattern.compile("\\s+(?://|\\|).*$");
    public static String cleanTitle(String title){
        String t=NOISE.matcher(title).replaceAll(" ");
        t=TAIL.matcher(t).replaceAll("");
        t=FEAT.matcher(t).replaceAll(" ");
        t=t.replaceAll("[\"«»“”]","").replaceAll("\\s{2,}"," ").strip();
        return t.isEmpty()?title.strip():t;
    }
    public static String cleanArtist(String artist){
        String a=artist.replaceAll("(?i)\\s*-\\s*topic$","").replaceAll("(?i)vevo$","").replaceAll("(?i)\\s*official$","");
        // "Artist1, Artist2" / "Artist1 & Artist2": the first one finds the song.
        a=a.split("\\s*(?:,|;|&| x | feat\\.? | ft\\.? )\\s*")[0];
        return a.strip();
    }
    /** Every artist the line names ("A, B & C feat. D"), cleaned, in order. */
    public static List<String> artistNames(String artist){
        String a=artist.replaceAll("(?i)\\s*-\\s*topic$","").replaceAll("(?i)vevo$","").replaceAll("(?i)\\s*official$","");
        var out=new ArrayList<String>();
        for(var name:a.split("\\s*(?:,|;|&| x | feat\\.? | ft\\.? | и | and )\\s*")){name=name.strip();if(!name.isEmpty()&&!out.contains(name))out.add(name);}
        return out;
    }
    public static List<Query> queries(String artist,String title){
        var out=new ArrayList<Query>();
        String t=cleanTitle(title),a=cleanArtist(artist);
        Matcher dash=Pattern.compile("^(.+?)\\s+[-–—]\\s+(.+)$").matcher(t);
        if(dash.matches()){
            // A video titled "Artist - Song" uploaded by a channel: the title knows the artist better.
            out.add(new Query(cleanArtist(dash.group(1)),cleanTitle(dash.group(2))));
            if(!a.isEmpty())out.add(new Query(a,cleanTitle(dash.group(2))));
        }
        if(!a.isEmpty())out.add(new Query(a,t));
        // A title alone finds other songs with the same name: only when nothing names an artist at all.
        if(out.isEmpty())out.add(new Query("",t));
        return out.stream().distinct().toList();
    }
    static String key(String artist,String title){return (artist+"\u0000"+title).toLowerCase(Locale.ROOT);}
}
