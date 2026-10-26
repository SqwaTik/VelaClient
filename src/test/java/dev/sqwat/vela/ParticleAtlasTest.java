package dev.sqwat.vela;
import dev.sqwat.vela.render.effect.ParticleAtlas;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ParticleAtlasTest {
    @Test void allShapesShareSmallAtlasAndHaveTransparentGutters()throws Exception{
        try(var in=getClass().getResourceAsStream("/assets/velaclient/textures/effects/particles/atlas.png")){
            assertNotNull(in);var image=ImageIO.read(in);
            assertEquals(ParticleAtlas.WIDTH,image.getWidth());assertEquals(ParticleAtlas.HEIGHT,image.getHeight());
            assertTrue((long)image.getWidth()*image.getHeight()*4<1_000_000);
            for(int i=0;i<ParticleAtlas.COUNT;i++){
                assertTrue(ParticleAtlas.u(i)>0&&ParticleAtlas.v(i)>0&&ParticleAtlas.u1(i)<1&&ParticleAtlas.v1(i)<1);
                int x=i%4*ParticleAtlas.CELL,y=i/4*ParticleAtlas.CELL;
                for(int offset=0;offset<ParticleAtlas.CELL;offset++){assertEquals(0,image.getRGB(x+offset,y)>>>24);assertEquals(0,image.getRGB(x,y+offset)>>>24);}
            }
        }
    }
}
