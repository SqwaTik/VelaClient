package dev.sqwat.vela;
import dev.sqwat.vela.marker.MarkerLabelGeometry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MarkerLabelGeometryTest {
    @Test void visibleLabelSurvivesAnchorBelowScreen(){
        assertTrue(MarkerLabelGeometry.visible(400,610,1,800,600));
        assertTrue(MarkerLabelGeometry.visible(400,580,1,800,600));
        assertFalse(MarkerLabelGeometry.visible(400,640,1,800,600));
    }
}
