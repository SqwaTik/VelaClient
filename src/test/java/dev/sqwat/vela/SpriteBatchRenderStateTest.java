package dev.sqwat.vela;

import dev.sqwat.vela.ui.render.SpriteBatchRenderState;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpriteBatchRenderStateTest {
    private static float[] quad(){return new float[]{-1.2f,2.5f,0,0,-1.2f,6.1f,0,1,4.2f,6.1f,1,1,4.2f,2.5f,1,0};}
    @Test void boundsRoundOutwardAndRespectScissor(){
        var state=new SpriteBatchRenderState(quad(),new int[]{-1},null,null,null);
        assertEquals(new ScreenRectangle(-2,2,7,5),state.bounds());
        var clip=new ScreenRectangle(0,3,2,2);
        assertEquals(clip,new SpriteBatchRenderState(quad(),new int[]{-1},null,null,clip).bounds());
    }
    @Test void emptyOrEntirelyClippedBatchesHaveNoBounds(){
        assertNull(new SpriteBatchRenderState(new float[0],new int[0],null,null,null).bounds());
        assertNull(new SpriteBatchRenderState(quad(),new int[]{-1},null,null,new ScreenRectangle(50,50,5,5)).bounds());
    }
}
