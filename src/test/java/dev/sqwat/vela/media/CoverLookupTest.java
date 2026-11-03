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
}
