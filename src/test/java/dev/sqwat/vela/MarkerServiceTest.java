package dev.sqwat.vela;
import dev.sqwat.vela.marker.MarkerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class MarkerServiceTest {
    @Test void allMarkerCoordinatesAreWholeBlocksAndEditedDeathIconSurvives(){
        var service=new MarkerService(root);service.state().death=true;
        service.add("Death","a","world",-1.2,64.8,3.9,0,"skull",true);
        var p=service.state().points.getFirst();assertEquals(-2,p.x());assertEquals(64,p.y());assertEquals(3,p.z());
        service.update(p.id(),p.name(),-2.2,63.9,4.1,0,"star");
        var loaded=new MarkerService(root).state().points.getFirst();assertEquals("star",loaded.icon());assertTrue(loaded.death());assertEquals(-3,loaded.x());
    }
    @Test void editPreservesIdentityAndWorld(){var s=new MarkerService(root);add(s,false,"server");String id=s.state().points.getFirst().id();s.update(id,"Новое имя",4,5,6,0xFFABCDEF,"star");var p=new MarkerService(root).state().points.getFirst();assertEquals(id,p.id());assertEquals("server",p.world());assertEquals("Новое имя",p.name());assertEquals(4,p.x());assertEquals("star",p.icon());}
    @TempDir Path root;
    private void add(MarkerService s,boolean death,String world){s.add("Точка",world,"overworld",1,64,3,0xFFFFAA33,"markers",death);}
    @Test void defaultsAreOptIn(){var s=new MarkerService(root).state();assertFalse(s.death);assertFalse(s.latest);assertFalse(s.limited);assertFalse(s.quick);assertEquals(-1,s.key);}
    @Test void persistsAndRemoves(){var s=new MarkerService(root);add(s,false,"a");var loaded=new MarkerService(root);assertEquals(1,loaded.state().points.size());loaded.remove(loaded.state().points.getFirst().id());assertTrue(new MarkerService(root).state().points.isEmpty());}
    @Test void latestDeathDoesNotDeleteOtherWorldOrManual(){var s=new MarkerService(root);s.state().death=true;s.state().latest=true;add(s,true,"a");add(s,true,"b");add(s,false,"a");add(s,true,"a");assertEquals(3,s.state().points.size());}
    @Test void disabledDeathCreatesNothing(){var s=new MarkerService(root);add(s,true,"a");assertTrue(s.state().points.isEmpty());}
    @Test void limitNeverDeletesExistingPoints(){var s=new MarkerService(root);s.state().limited=true;s.state().limit=10;for(int i=0;i<10;i++)add(s,false,"a");assertThrows(IllegalArgumentException.class,()->add(s,false,"a"));assertEquals(10,s.state().points.size());}
    @Test void rejectsNonFiniteCoordinates(){var s=new MarkerService(root);assertThrows(IllegalArgumentException.class,()->s.add("x","a","b",Double.NaN,0,0,0,"markers",false));}
}
