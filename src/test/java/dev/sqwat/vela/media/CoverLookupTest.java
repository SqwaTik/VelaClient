package dev.sqwat.vela.media;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CoverLookupTest {
    private static String video(String id,String title,String owner){
        return "\"videoRenderer\":{\"videoId\":\""+id+"\",\"thumbnail\":{},\"title\":{\"runs\":[{\"text\":\""+title+"\"}]},"
            +"\"ownerText\":{\"runs\":[{\"text\":\""+owner+"\"}]}}";
    }

    @Test
    void theVideoWithThisTitleFromThisChannelIsPicked(){
        String page="{"+video("aaaaaaaaaaa","Other song","Someone")+","+video("bbbbbbbbbbb","Blinding Lights (Official Video)","The Weeknd")+"}";
        assertEquals("bbbbbbbbbbb",CoverLookup.pickVideo(page,"The Weeknd","Blinding Lights"));
    }

    @Test
    void aVideoFromAnotherChannelIsSkipped(){
        String page="{"+video("aaaaaaaaaaa","Blinding Lights cover","Random Covers")+","+video("ccccccccccc","Blinding Lights","TheWeekndVEVO")+"}";
        assertEquals("ccccccccccc",CoverLookup.pickVideo(page,"The Weeknd","Blinding Lights"));
    }

    @Test
    void escapedTitlesAreReadAndMissesGiveNull(){
        String page="{"+video("ddddddddddd","Say \\\"Hi\\\"","Chan")+"}";
        assertEquals("ddddddddddd",CoverLookup.pickVideo(page,"","Say \"Hi\""));
        assertNull(CoverLookup.pickVideo(page,"","Nothing like it"));
    }

    @Test
    void qqHitsMatchTitleAndOneOfTheArtists(){
        var json=com.google.gson.JsonParser.parseString(("{'data':{'song':{'list':["
            +"{'songname':'PABLo (Explicit)','albummid':'','singer':[{'name':'MORGENSHTERN'}]},"
            +"{'songname':'PABLo','albummid':'111JF01C40lhiK','singer':[{'name':'Someone Else'}]},"
            +"{'songname':'PABLo (Explicit)','albummid':'000JF01C40lhiK','singer':[{'name':'Morgenshtern'},{'name':'Lil Pump'}]}]}}}").replace('\'','"')).getAsJsonObject();
        assertEquals("https://y.gtimg.cn/music/photo_new/T002R300x300M000000JF01C40lhiK.jpg",CoverLookup.pickQq(json,"MORGENSHTERN","PABLO"));
        assertNull(CoverLookup.pickQq(json,"MORGENSHTERN","Cadillac"));
        assertNull(CoverLookup.pickQq(com.google.gson.JsonParser.parseString("{\"code\":0}").getAsJsonObject(),"a","b"));
    }
}
