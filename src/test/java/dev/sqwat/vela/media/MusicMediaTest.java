package dev.sqwat.vela.media;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MusicMediaTest {
    @Test void parsesTimedLyricsWithRepeatsTagsAndPauses(){
        var l=Lyrics.parseSynced("[ar:Someone]\n[00:00.00]\n[00:01.50]first\n[00:04.2]second\n[00:07.123][00:20.00]chorus\n[00:10.00]\n[00:11.00]\n[00:12.00]after\n[00:30.00]");
        assertTrue(l.synced());
        // "after" is not left lit through the 8 s gap before the last chorus: a pause follows once it was sung.
        assertEquals(java.util.List.of("first","second","chorus","","after","","chorus"),l.lines().stream().map(Lyrics.Line::text).toList());
        assertEquals(1500,l.lines().get(0).timeMs());assertEquals(4200,l.lines().get(1).timeMs());assertEquals(7123,l.lines().get(2).timeMs());
        long pause=l.lines().get(5).timeMs();assertTrue(pause>=13_500&&pause<=14_500,"pause at "+pause);
        assertEquals(-1,l.index(1000,0));assertEquals(0,l.index(1500,0));assertEquals(1,l.index(6000,0));assertEquals(6,l.index(99_000,0));
        assertEquals(.5f,l.lineProgress(0,2850),.001f);
        assertTrue(Lyrics.parseSynced("no stamps here").empty());
    }
    @Test void wordStampsAndOffsetAreApplied(){
        var l=Lyrics.parseSynced("[offset:+500]\n[00:10.00]<00:10.00>Hello <00:10.50>world\n[00:12.00]next");
        assertEquals("Hello world",l.lines().getFirst().text());
        assertEquals(9500,l.lines().getFirst().timeMs());
        // The sweep follows the word stamps: "Hello " until 10 s, then "world".
        assertEquals(0,l.sung(0,9400),.001f);
        assertEquals(6/11f,l.sung(0,10_000),.001f);
    }
    @Test void aHeldWordAndTheGapAfterItKeepTheirTime(){
        // NetEase word timing: "call" is held for 2 s, then nothing is sung until the next line.
        String lrc=LyricsService.yrcToLrc("{\"t\":0,\"c\":[{\"tx\":\"credits\"}]}\n[0,1000](0,1000,0)作词 : Someone\n"
            +"[10000,4000](10000,500,0)I'll (10500,500,0)wait (11000,2000,0)call\n[16000,1000](16000,1000,0)next line");
        var l=Lyrics.parseSynced(lrc);
        assertEquals("I'll wait call",l.lines().getFirst().text());
        assertEquals(10_000,l.lines().getFirst().timeMs());
        assertEquals(10/14f,l.sung(0,11_000),.001f);
        assertEquals(12/14f,l.sung(0,12_000),.001f);
        assertEquals(1,l.sung(0,13_500),.001f);
    }
    @Test void lineTimingWithoutWordsIsEstimatedFromSyllables(){
        // Short lines back to back set the pace; the long gap after the last one does not stretch its words.
        var s=new StringBuilder();
        for(int i=0;i<8;i++)s.append(String.format("[00:%02d.00]la la la la\n",i*2));
        s.append("[00:16.00]la la la la\n[00:40.00]end");
        var l=Lyrics.parseSynced(s.toString());
        int last=l.index(16_000,0);
        assertEquals("la la la la",l.lines().get(last).text());
        assertEquals(1,l.sung(last,19_000),.001f);
        assertTrue(l.sung(last,17_000)>.3f);
    }
    @Test void sparseOrBrokenTimedTextIsRejected(){
        var good=new StringBuilder();
        for(int i=0;i<20;i++)good.append(String.format("[%02d:%02d.00]line number %d\n",(10+i*9)/60,(10+i*9)%60,i));
        assertTrue(Lyrics.parseSynced(good.toString()).plausible(200_000));
        // Two stamps for a whole song, stamps far past the end, one letter per line.
        assertFalse(Lyrics.parseSynced("[00:01.00]all the words\n[00:02.00]in two lines").plausible(200_000));
        assertFalse(Lyrics.parseSynced(good.toString()).plausible(60_000));
        assertFalse(Lyrics.parseSynced(good.toString().replaceAll("line number \\d+","a")).plausible(200_000));
    }
    @Test void plainLyricsParse(){
        // A verse break stays as a pause; lines take room by their syllables.
        var l=Lyrics.parsePlain("\na\n\n\nb\nc\nd\n\n");
        assertFalse(l.synced());assertEquals(5,l.lines().size());assertEquals("",l.lines().get(1).text());
        assertEquals(0,l.index(0,100_000));assertEquals(4,l.index(99_000,100_000));assertEquals(1,l.index(40_000,100_000));
        var long_=Lyrics.parsePlain("ta\nta ta ta ta ta ta ta ta ta\nta");
        assertEquals(1,long_.index(50_000,100_000));
    }
    @Test void lineSweepFollowsTheSinging(){
        // Back-to-back lines are swept over their whole gap, short of a breath: the sweep does not run ahead.
        var l=Lyrics.parseSynced("[00:10.00]раз два три четыре\n[00:12.40]пять шесть семь восемь\n[00:14.80]девять десять\n[00:30.00]снова тут\n[00:32.00]и опять\n[00:34.00]конец");
        assertTrue(l.sung(0,11_000)>.35f&&l.sung(0,11_000)<.6f);
        assertTrue(l.sung(0,12_000)>.85f&&l.sung(0,12_000)<1);
        assertEquals(1f,l.sung(0,12_350),1e-3);
        // After the last word of a line before a long gap, the pause dots come.
        int pause=l.index(20_000,0);
        assertEquals("",l.lines().get(pause).text());
        assertEquals("девять десять",l.lines().get(l.index(15_500,0)).text());
        assertEquals("снова тут",l.lines().get(l.index(30_100,0)).text());
    }
    @Test void videoTitlesBecomeCleanQueries(){
        assertEquals("Song",Lyrics.cleanTitle("Song (Official Music Video)"));
        assertEquals("Song",Lyrics.cleanTitle("Song [Lyrics] (feat. Other)"));
        assertEquals("Песня",Lyrics.cleanTitle("Песня (премьера клипа, 2024)"));
        assertEquals("Artist",Lyrics.cleanArtist("Artist - Topic"));
        assertEquals("Artist",Lyrics.cleanArtist("ArtistVEVO"));
        assertEquals("One",Lyrics.cleanArtist("One, Two & Three"));
        var q=Lyrics.queries("Some Channel","Artist - Song (Official Video)");
        assertEquals(new Lyrics.Query("Artist","Song"),q.getFirst());
        // A title alone matches other songs: never searched when an artist is known.
        assertTrue(q.stream().noneMatch(x->x.artist().isEmpty()));
        assertEquals(java.util.List.of(new Lyrics.Query("","Song")),Lyrics.queries("","Song"));
    }
    @Test void trackParsingAndExtrapolation(){
        var first=MediaTrack.parse(JsonParser.parseString("{\"state\":\"Playing\",\"app\":\"brave.exe\",\"appName\":\"Brave\",\"title\":\"T\",\"artist\":\"A\",\"position\":1000,\"duration\":5000,\"updated\":100}").getAsJsonObject(),null);
        assertEquals("Brave",first.appName());assertEquals(3000,first.position(2100));assertEquals(5000,first.position(99_999));
        var next=MediaTrack.parse(JsonParser.parseString("{\"state\":\"Paused\",\"app\":\"brave.exe\",\"title\":\"T\",\"artist\":\"A\",\"position\":1000,\"duration\":5000,\"updated\":100}").getAsJsonObject(),first);
        assertEquals("Brave",next.appName());assertEquals(1000,next.position(9000));assertTrue(next.sameSong(first));
        assertNull(MediaTrack.parse(JsonParser.parseString("{\"state\":\"none\"}").getAsJsonObject(),first));
        assertEquals("Spotify",MediaTrack.prettyApp("Spotify.exe"));
        assertEquals("Яндекс Музыка",MediaTrack.prettyApp("A025C540.Yandex.Music_vfvw9svesycw6!App"));
    }
    private static String hit(String artist,String title,int seconds,String first){
        var lrc=new StringBuilder("["+"00:05.00]"+first+"\\n");
        for(int i=1;i<20;i++)lrc.append(String.format("[%02d:%02d.00]line number %d\\n",(5+i*9)/60,(5+i*9)%60,i));
        return "{\"artistName\":\""+artist+"\",\"trackName\":\""+title+"\",\"duration\":"+seconds+",\"syncedLyrics\":\""+lrc+"\"}";
    }
    @Test void searchTakesOnlyTimedTextOfThisSongAndLength(){
        var query=new Lyrics.Query("Band","Song");
        var hits=JsonParser.parseString("["+hit("Band","Song",210,"far")+",{\"artistName\":\"Band\",\"trackName\":\"Song\",\"duration\":201,\"plainLyrics\":\"plain\"},"
            +hit("Other","Song",200,"other artist")+","+hit("Band","Song",202,"near")+"]").getAsJsonArray();
        var best=LyricsService.best(hits,query,200);
        assertEquals(LyricsService.State.FOUND,best.state());assertEquals("near",best.lyrics().lines().getFirst().text());
        // 10 s longer is another version: every line would come late.
        assertNull(LyricsService.best(JsonParser.parseString("["+hit("Band","Song",210,"far")+"]").getAsJsonArray(),query,200));
        // Plain text only: nothing, rather than lines spread at random over the song.
        assertNull(LyricsService.best(JsonParser.parseString("[{\"artistName\":\"Band\",\"trackName\":\"Song\",\"duration\":200,\"plainLyrics\":\"plain\"}]").getAsJsonArray(),query,200));
        // Song length unknown and no artist: the title alone is not trusted.
        assertNull(LyricsService.best(JsonParser.parseString("["+hit("Band","Song",200,"x")+"]").getAsJsonArray(),new Lyrics.Query("","Song"),0));
    }
    @Test void neteaseCreditsAreDroppedAndThePickFollowsTheLength(){
        String lrc=LyricsService.stripCredits("[00:00.000] \u4f5c\u8bcd : Band\n[00:01.000] \u4f5c\u66f2 : Band\n[00:05.00]first line here");
        assertFalse(lrc.contains("Band"));assertTrue(lrc.contains("first line here"));
        var song=new StringBuilder();
        for(int i=0;i<20;i++)song.append(String.format("[%02d:%02d.00]line number %d\n",(5+i*9)/60,(5+i*9)%60,i));
        var shortVersion=new LyricsService.Candidate("Band","Song",190,song.toString(),false);
        var longVersion=new LyricsService.Candidate("Band","Song (Live)",240,song.toString().replace("line number","live line"),false);
        var queries=java.util.List.of(new Lyrics.Query("Band","Song"));
        var all=java.util.List.of(shortVersion,longVersion);
        assertEquals("line number 0",LyricsService.pick(all,queries,191).lyrics().lines().getFirst().text());
        // The player first reported another length: nothing fits until the real one comes.
        assertNull(LyricsService.pick(java.util.List.of(shortVersion),queries,300));
    }
    @Test void plainTextIsShownOnlyWhenNothingIsTimed(){
        var plain=new LyricsService.Candidate("Band","Song",190,"","one line\ntwo line\nthree line\n\nfour line\nfive line\nsix line\nseven line",false);
        var queries=java.util.List.of(new Lyrics.Query("Band","Song"));
        assertNull(LyricsService.pick(java.util.List.of(plain),queries,191));
        var shown=LyricsService.plainPick(java.util.List.of(plain),queries,191);
        assertFalse(shown.lyrics().synced());
        assertEquals("one line",shown.lyrics().lines().getFirst().text());
        // Artist and title entered the wrong way round still count; another song's text is not borrowed.
        assertNotNull(LyricsService.plainPick(java.util.List.of(new LyricsService.Candidate("Song","Band",190,"",plain.plain,false)),queries,191));
        assertNull(LyricsService.plainPick(java.util.List.of(plain),java.util.List.of(new Lyrics.Query("Band","Other")),191));
    }
    @Test void mangledRussianTextIsRepaired(){
        String russian="Мой свэг nerf";
        String mangled=new String(russian.getBytes(java.nio.charset.StandardCharsets.UTF_8),java.nio.charset.Charset.forName("windows-1251"));
        assertEquals(russian,LyricsService.unmangle(mangled));
        assertEquals(russian,LyricsService.unmangle(russian));
        assertEquals("СССР РОССИЯ plain text here",LyricsService.unmangle("СССР РОССИЯ plain text here"));
    }
    private static String timedSong(long shiftMs,String word){
        var song=new StringBuilder();
        for(int i=0;i<20;i++){long t=5000+i*9000L+shiftMs;song.append(String.format("[%02d:%02d.%03d]%s number %d\n",t/60_000,t/1000%60,t%1000,word,i));}
        return song.toString();
    }
    @Test void lyricsPickTheTimingOthersAgreeWith(){
        var queries=java.util.List.of(new Lyrics.Query("Band","Song"));
        // Closest in length, but timed 700 ms late: the two others agree with each other within 40 ms.
        var late=new LyricsService.Candidate("Band","Song",191,timedSong(700,"line"),false);
        var a=new LyricsService.Candidate("Band","Song",193,timedSong(0,"line"),false);
        var b=new LyricsService.Candidate("Band","Song",189,timedSong(40,"line"),false);
        var picked=LyricsService.pick(java.util.List.of(late,a,b),queries,191).lyrics();
        assertTrue(picked.lines().getFirst().timeMs()<1000+5000,"took the late copy");
        // The same late text found twice is still one vote.
        picked=LyricsService.pick(java.util.List.of(late,late,a),queries,191).lyrics();
        assertEquals(late.lyrics().lines().getFirst().timeMs(),picked.lines().getFirst().timeMs());
        var keysA=LyricsService.keyLines(a.lyrics());var keysLate=LyricsService.keyLines(late.lyrics());
        assertEquals(700L,LyricsService.agreement(keysA,keysLate));
        assertNull(LyricsService.agreement(keysA,LyricsService.keyLines(Lyrics.parseSynced(timedSong(0,"other")))));
    }
    @Test void qqSearchAndText(){
        var search=com.google.gson.JsonParser.parseString("{\"data\":{\"song\":{\"list\":["
            +"{\"songname\":\"Imbalance (Explicit)\",\"songmid\":\"m1\",\"interval\":117,\"singer\":[{\"name\":\"5opka\"},{\"name\":\"MellSher\"}]},"
            +"{\"songname\":\"Imbalance\",\"songmid\":\"m2\",\"interval\":200,\"singer\":[{\"name\":\"Someone\"}]}]}}}").getAsJsonObject();
        var songs=LyricsService.qqSongs(search,new Lyrics.Query("MellSher","Imbalance"));
        assertEquals(1,songs.size());assertEquals("m1",songs.getFirst().get("songmid").getAsString());
        assertEquals("I ain't & \"you\"",LyricsService.unescape("I ain&#39;t &amp; &quot;you&quot;"));
    }
}
