package dev.sqwat.vela;

import dev.sqwat.vela.render.perf.AppendOnlyRenderIndex;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AppendOnlyRenderIndexTest {
    @Test void actualPreparedMaterialsKeepTextureAndScissorIdentity(){
        var index=new AppendOnlyRenderIndex<net.minecraft.client.renderer.rendertype.PreparedRenderType>();
        var list=new ArrayList<net.minecraft.client.renderer.rendertype.PreparedRenderType>();
        var scissor=new com.mojang.blaze3d.systems.ScissorState();
        for(int i=0;i<40;i++)list.add(new net.minecraft.client.renderer.rendertype.PreparedRenderType(null,null,null,scissor,
                List.of(new net.minecraft.client.renderer.rendertype.PreparedRenderType.Texture("map"+i,null,null))));
        var same=new net.minecraft.client.renderer.rendertype.PreparedRenderType(null,null,null,scissor,list.get(23).textures());
        assertEquals(23,index.indexOf(list,same));
        var differentScissor=new net.minecraft.client.renderer.rendertype.PreparedRenderType(null,null,null,new com.mojang.blaze3d.systems.ScissorState(),same.textures());
        assertEquals(-1,index.indexOf(list,differentScissor));
    }
    record Collision(int id){@Override public int hashCode(){return 1;}}
    @Test void matchesFirstIndexWithDuplicatesAndHashCollisions(){
        var index=new AppendOnlyRenderIndex<Collision>();var list=new ArrayList<Collision>();
        for(int i=0;i<300;i++){
            list.add(new Collision(i%29));
            for(int key=0;key<35;key++)assertEquals(list.indexOf(new Collision(key)),index.indexOf(list,new Collision(key)));
        }
    }
    @Test void oversizedGroupsAndNullPreserveListSemantics(){
        var index=new AppendOnlyRenderIndex<Integer>();var list=new ArrayList<Integer>();
        for(int i=0;i<4200;i++)list.add(i);list.add(null);
        assertEquals(4000,index.indexOf(list,4000));assertEquals(4200,index.indexOf(list,null));assertEquals(-1,index.indexOf(list,-1));
    }
    static final class Key {
        static long comparisons;final int id;Key(int id){this.id=id;}
        @Override public int hashCode(){return id;}
        @Override public boolean equals(Object other){comparisons++;return other instanceof Key k&&k.id==id;}
    }
    @Test void uniqueMaterialsAvoidQuadraticEqualityWork(){
        var index=new AppendOnlyRenderIndex<Key>();var list=new ArrayList<Key>();Key.comparisons=0;
        for(int i=0;i<1000;i++){var key=new Key(i);assertEquals(-1,index.indexOf(list,key));list.add(key);}
        long indexed=Key.comparisons;Key.comparisons=0;
        var vanilla=new ArrayList<Key>();for(var key:list){vanilla.indexOf(key);vanilla.add(key);}
        assertTrue(indexed*100<Key.comparisons,"Index should eliminate the repeated full-list scan");
    }
}
