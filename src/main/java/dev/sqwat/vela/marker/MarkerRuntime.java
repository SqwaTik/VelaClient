package dev.sqwat.vela.marker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.sqwat.vela.ui.render.*;
import org.lwjgl.glfw.GLFW;

public final class MarkerRuntime {
    private static Object player;
    private static boolean dead,held;
    private MarkerRuntime(){}
    /** "/vela mark x y z": run locally by clicked chat coordinates (Chat module); never sent to the server. */
    public static void registerCommand(MarkerService service){
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher,registry)->dispatcher.register(
            net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("vela").then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("mark")
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("x",com.mojang.brigadier.arguments.IntegerArgumentType.integer(-30_000_000,30_000_000))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("y",com.mojang.brigadier.arguments.IntegerArgumentType.integer(-64,320))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("z",com.mojang.brigadier.arguments.IntegerArgumentType.integer(-30_000_000,30_000_000))
                .executes(ctx->{
                    var c=Minecraft.getInstance();
                    int x=ctx.getArgument("x",Integer.class),y=ctx.getArgument("y",Integer.class),z=ctx.getArgument("z",Integer.class);
                    try{service.add(service.nextName(world(c),dimension(c)),world(c),dimension(c),x,y,z,VelaTheme.PRIMARY,"markers",false);}
                    catch(IllegalArgumentException full){ctx.getSource().sendError(net.minecraft.network.chat.Component.literal(full.getMessage()));return 0;}
                    return 1;
                })))))));
    }
    public static String world(Minecraft c){return c.getCurrentServer()!=null?c.getCurrentServer().ip:c.getSingleplayerServer()!=null?c.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toString():"";}
    public static String dimension(Minecraft c){return c.level.dimension().identifier().toString();}
    public static void tick(Minecraft c,MarkerService service){
        if(c.player==null||c.level==null){player=null;dead=held=false;return;}
        boolean nowDead=!c.player.isAlive();
        if(player!=c.player){player=c.player;dead=nowDead;held=false;}
        var s=service.state();
        if(nowDead&&!dead&&s.death){try{service.add("Место смерти",world(c),dimension(c),c.player.getX(),c.player.getY(),c.player.getZ(),VelaTheme.PRIMARY,"skull",true);}catch(IllegalArgumentException ignored){}}
        dead=nowDead;
        boolean down=dev.sqwat.vela.ui.input.BindInput.down(c.getWindow().handle(),s.key);
        if(down&&!held&&s.enabled&&s.quick&&!dead&&c.gui.screen()==null&&c.isWindowActive()){var pos=c.player.pick(5,1,false).getLocation();try{service.add(service.nextName(world(c),dimension(c)),world(c),dimension(c),pos.x,pos.y,pos.z,VelaTheme.PRIMARY,"markers",false);}catch(IllegalArgumentException ignored){}}
        held=down;
    }
    public static void draw(GuiGraphicsExtractor g,MarkerService service){
        var c=Minecraft.getInstance();if(c.player==null||c.level==null||!service.state().enabled||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(c))return;
        g.pose().pushMatrix();g.pose().identity();
        try {
        var camera=c.gameRenderer.mainCamera();var origin=camera.position();
        var matrix=camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f());
        int w=c.getWindow().getGuiScaledWidth(),h=c.getWindow().getGuiScaledHeight();String world=world(c),dim=dimension(c);
        for(var p:service.state().points){
            if(!p.world().equals(world)||!p.dimension().equals(dim)||p.death()&&!service.state().death)continue;
            var v=new org.joml.Vector4f((float)(p.x()-origin.x),(float)(p.y()-origin.y),(float)(p.z()-origin.z),1).mul(matrix);
            if(v.w<=0)continue;float nx=v.x/v.w,ny=v.y/v.w;
            float anchorX=MarkerLabelGeometry.screenX(nx,w),anchorY=MarkerLabelGeometry.screenY(ny,h);
            // Project only the anchor. All label geometry uses a fixed physical-pixel density.
            float density=1.5f/Math.max(1,c.getWindow().getGuiScale());
            if(!MarkerLabelGeometry.visible(anchorX,anchorY,density,w,h))continue;
            g.pose().pushMatrix();g.pose().translate(anchorX,anchorY);g.pose().scale(density);
            String label=VelaDraw.ellipsize(p.name(),76,VelaDraw.Weight.MEDIUM,8);
            String distance=Math.round(Math.sqrt(c.player.distanceToSqr(p.x(),p.y(),p.z())))+"m";
            MarkerOverlayChrome.label(new GameUiCanvas(g),label,distance,p.icon(),p.death()?VelaTheme.PRIMARY:p.color());
            g.pose().popMatrix();
        }
        }finally{g.pose().popMatrix();}
    }
}
