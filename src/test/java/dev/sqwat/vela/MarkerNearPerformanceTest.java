package dev.sqwat.vela;

import dev.sqwat.vela.marker.*;
import dev.sqwat.vela.ui.hud.NearPlayers;
import dev.sqwat.vela.render.entity.SectionVisibilityCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MarkerNearPerformanceTest {
    @TempDir Path root;
    @Test void notificationExpiresAtThreeAndAHalfSeconds(){assertTrue(MarkerNotices.alive(0));assertTrue(MarkerNotices.alive(3499));assertFalse(MarkerNotices.alive(3500));}
    @Test void noticeOnlyFollowsSuccessfulCreationAndDeletePersists(){
        var service=new MarkerService(root);var notices=new ArrayList<MarkerService.Point>();service.onCreated(notices::add);
        service.add("Метка 1","world","dim",1,2,3,-1,"markers",false);
        assertEquals(1,notices.size());assertEquals(service.state().points.getFirst(),notices.getFirst());
        service.update(notices.getFirst().id(),"Edit",1,2,3,-1,"star");assertEquals(1,notices.size());
        assertThrows(IllegalArgumentException.class,()->service.add("Bad","world","dim",Double.NaN,2,3,-1,"markers",false));assertEquals(1,notices.size());
        service.remove(notices.getFirst().id());assertTrue(new MarkerService(root).state().points.isEmpty());
    }
    @Test void labelUsesFractionalAnchorAndTightTextBounds(){
        float a=MarkerLabelGeometry.screenX(.123f,960),b=MarkerLabelGeometry.screenX(.1235f,960);
        assertEquals(.24f,b-a,.0001f);assertNotEquals(Math.round(a),a);
        assertEquals(51,MarkerLabelGeometry.titleWidth(30));assertEquals(24,MarkerLabelGeometry.distanceWidth(16));
    }
    @Test void listedRealPlayersAreImmediateButUnlistedEntitiesAreRejected(){
        UUID id=UUID.randomUUID();
        assertTrue(NearPlayers.eligible(true,id,"Alex",id,"Alex"));
        assertFalse(NearPlayers.eligible(false,id,"Alex",id,"Alex"));
        assertFalse(NearPlayers.eligible(true,id,"NPC",id,"Alex"));
        assertFalse(NearPlayers.eligible(true,id,"Alex",UUID.randomUUID(),"Alex"));
    }
    @Test void boundedSelectionMatchesFullSortForDenseWorld(){
        var entries=new ArrayList<Integer>();for(int i=0;i<10000;i++)entries.add(i);
        Collections.shuffle(entries,new Random(72));
        assertEquals(entries.stream().filter(i->i%3!=0&&i<=10000).sorted().limit(5).toList(),NearPlayers.closest(entries,5,i->i%3!=0,i->i));
        assertEquals(List.of(),NearPlayers.closest(entries,0,i->true,i->i));
    }
    @Test void storageSectionTestsAreSharedAndRefreshOnCameraTurn(){
        var cache=new SectionVisibilityCache();assertFalse(cache.outside(0,0,64));
        var projection=new org.joml.Matrix4f().perspective((float)Math.toRadians(70),16f/9f,.1f,256);
        var front=new net.minecraft.client.renderer.culling.Frustum(new org.joml.Matrix4f(),projection);front.prepare(0,0,0);
        cache.beginFrame(front,net.minecraft.world.phys.Vec3.ZERO);
        for(int i=0;i<1000;i++)assertTrue(cache.outside(i%16,0,64));
        assertEquals(1,cache.tests());assertFalse(cache.outside(-1,0,-1));
        var back=new net.minecraft.client.renderer.culling.Frustum(new org.joml.Matrix4f().rotateY((float)Math.PI),projection);back.prepare(0,0,0);
        cache.beginFrame(back,net.minecraft.world.phys.Vec3.ZERO);assertFalse(cache.outside(0,0,64));
        cache.clear();assertFalse(cache.outside(0,0,64));
    }
}
