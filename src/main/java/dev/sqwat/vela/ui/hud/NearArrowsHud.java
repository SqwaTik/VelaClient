package dev.sqwat.vela.ui.hud;

import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.Map;

/** "Near" in arrow mode: eased pointers around the crosshair. Players and labels come from the HUD service. */
final class NearArrowsHud {
    private final Map<java.util.UUID,float[]> arrowMotion=new java.util.HashMap<>();
    private float arrowRadius=64;
    private long arrowTime;
    private Object arrowWorld;
    private int arrowCacheTick=Integer.MIN_VALUE;
    private final Map<java.util.UUID,String> arrowLabels=new java.util.HashMap<>();
    private final Map<java.util.UUID,String> arrowCaptions=new java.util.HashMap<>();
    private int arrowCaptionFlags=-1;
    private static final net.minecraft.resources.Identifier NEAR_ARROW=net.minecraft.resources.Identifier.fromNamespaceAndPath("velaclient","textures/effects/near_arrow.png");
    void clear(){arrowMotion.clear();arrowWorld=null;arrowTime=0;}
    void draw(GuiGraphicsExtractor g,Minecraft client,float partial,java.util.List<net.minecraft.world.entity.player.Player> players,
              Map<java.util.UUID,String> nearbyLabels,Map<java.util.UUID,String> nearbyDistances,int nearbyTick,
              int rgb,float density,boolean names,boolean distance){
        if(client.player==null)return;
        var camera=client.gameRenderer.mainCamera();var origin=camera.position();
        if(arrowWorld!=client.level){arrowMotion.clear();arrowLabels.clear();arrowCaptions.clear();arrowCacheTick=Integer.MIN_VALUE;arrowWorld=client.level;arrowTime=0;}
        long now=System.nanoTime();float dt=arrowTime==0?1/60f:Math.min(.1f,(now-arrowTime)/1e9f);arrowTime=now;
        boolean reduced=dev.sqwat.vela.VelaClient.services().configs().current().reducedMotion||!dev.sqwat.vela.VelaClient.services().configs().current().animations;
        int captionFlags=(names?1:0)|(distance?2:0);
        if(arrowCacheTick!=nearbyTick||arrowCaptionFlags!=captionFlags){
            arrowCaptionFlags=captionFlags;
            arrowCacheTick=nearbyTick;var ids=new java.util.HashSet<java.util.UUID>();
            for(var p:players){ids.add(p.getUUID());String name=nearbyLabels.getOrDefault(p.getUUID(),p.getGameProfile().name());arrowLabels.put(p.getUUID(),VelaDraw.ellipsize(name,64,VelaDraw.Weight.MEDIUM,8));}
            arrowMotion.keySet().retainAll(ids);arrowLabels.keySet().retainAll(ids);
            arrowCaptions.clear();
            for(var p:players)arrowCaptions.put(p.getUUID(),(names?arrowLabels.get(p.getUUID()):"")+(names&&distance?"\n":"")+(distance?nearbyDistances.getOrDefault(p.getUUID(),""):""));
        }
        // Fixed ring; every arrow points at its player's real direction and may overlap others, like classic arrows.
        float goal=Math.max(40,Math.min(72*density,Math.min(g.guiWidth(),g.guiHeight())*.5f-48*density));
        arrowRadius=reduced?goal:dev.sqwat.vela.ui.animation.Motion.approach(arrowRadius,goal,9,dt);
        float iconSize=22*density;
        for(int i=0;i<players.size();i++){
            var player=players.get(i);
            int playerRgb=dev.sqwat.vela.VelaClient.services().friends().contains(player.getGameProfile().name())?VelaTheme.POSITIVE&0xFFFFFF:rgb;
            var point=player.getPosition(partial);
            float target=NearDirection.radians(point.x-origin.x,point.z-origin.z,camera.yRot());
            float[] state=arrowMotion.computeIfAbsent(player.getUUID(),id->new float[]{0,target});
            state[0]=reduced?1:dev.sqwat.vela.ui.animation.Motion.approach(state[0],1,10,dt);
            // Ease along the shortest arc: smooth when the player or the camera turns, never spins the long way round.
            float delta=(float)Math.IEEEremainder(target-state[1],Math.PI*2);
            state[1]=reduced?target:state[1]+delta*(1-(float)Math.exp(-dt*22));
            float angle=state[1];
            float r=arrowRadius;
            float x=g.guiWidth()/2f+(float)Math.cos(angle)*r,y=g.guiHeight()/2f+(float)Math.sin(angle)*r;
            int alpha=Math.round(255*state[0]);
            g.pose().pushMatrix();
            try{
                g.pose().translate(x,y);g.pose().scale(density);g.pose().rotate(angle+(float)Math.PI/2);
                float size=iconSize/density;
                VelaDraw.blit(g,NEAR_ARROW,-size/2,-size/2,size,size,0,0,48,48,48,48,(alpha<<24)|playerRgb);
            }finally{g.pose().popMatrix();}
            if(!names&&!distance)continue;
            g.pose().pushMatrix();
            try{
                g.pose().translate(x,y+14*density);g.pose().scale(density);
                String caption=arrowCaptions.getOrDefault(player.getUUID(),"");
                VelaDraw.floatingText(g,caption,1,3,alpha<<24,8);
                VelaDraw.floatingText(g,caption,0,2,(alpha<<24)|0xF4F6FF,8);
            }finally{g.pose().popMatrix();}
        }
    }
}
