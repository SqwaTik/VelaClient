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
    private static final java.util.Map<String,net.minecraft.world.item.ItemStack> ICONS=new java.util.HashMap<>();
    private static net.minecraft.world.item.ItemStack icon(String id){
        return ICONS.computeIfAbsent(id,k->{var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.tryParse(k));return item==null?net.minecraft.world.item.ItemStack.EMPTY:new net.minecraft.world.item.ItemStack(item);});
    }
    /** Whether any storage marker in this world needs its container highlighted (VisualGizmosMixin). */
    public static boolean hasHighlights(MarkerService service){
        var c=Minecraft.getInstance();if(c.level==null||!service.state().enabled)return false;
        String world=world(c),dim=dimension(c);
        for(var p:service.state().points)if(p.storage()&&p.world().equals(world)&&p.dimension().equals(dim))return true;
        return false;
    }
    /** Coloured fill and a pulsing outline on each marked container within 96 blocks; the outline shows through walls. */
    public static void submitHighlights(MarkerService service){
        var c=Minecraft.getInstance();if(c.level==null||c.player==null)return;
        String world=world(c),dim=dimension(c);
        float pulse=(float)(.5+.5*Math.sin(System.currentTimeMillis()/1000.0*Math.PI*2/1.6));
        for(var p:service.state().points){
            if(!p.storage()||!p.world().equals(world)||!p.dimension().equals(dim))continue;
            var pos=net.minecraft.core.BlockPos.containing(p.x(),p.y(),p.z());
            if(pos.distSqr(c.player.blockPosition())>96*96||!c.level.isLoaded(pos))continue;
            var state=c.level.getBlockState(pos);
            var shape=state.getShape(c.level,pos);
            var box=shape.isEmpty()?new net.minecraft.world.phys.AABB(pos):shape.bounds().move(pos);
            if(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock&&state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE){
                var other=pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state));
                var otherShape=c.level.getBlockState(other).getShape(c.level,other);
                if(!otherShape.isEmpty())box=box.minmax(otherShape.bounds().move(other));
            }
            int rgb=p.color()&0xFFFFFF;
            net.minecraft.gizmos.Gizmos.cuboid(box.inflate(.004),net.minecraft.gizmos.GizmoStyle.fill((Math.round(40+40*pulse)<<24)|rgb));
            var b=box.inflate(.006);int stroke=(Math.round(150+100*pulse)<<24)|rgb;
            double[][] corners={{b.minX,b.minY,b.minZ},{b.maxX,b.minY,b.minZ},{b.maxX,b.minY,b.maxZ},{b.minX,b.minY,b.maxZ},{b.minX,b.maxY,b.minZ},{b.maxX,b.maxY,b.minZ},{b.maxX,b.maxY,b.maxZ},{b.minX,b.maxY,b.maxZ}};
            int[][] edges={{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
            for(var e:edges){
                var a=corners[e[0]];var z=corners[e[1]];
                net.minecraft.gizmos.Gizmos.line(new net.minecraft.world.phys.Vec3(a[0],a[1],a[2]),new net.minecraft.world.phys.Vec3(z[0],z[1],z[2]),stroke,2.5f).setAlwaysOnTop();
            }
        }
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
            // Storage markers sit on the centre of the top of the container; plain markers keep their point.
            double ax=p.storage()?p.x()+.5:p.x(),ay=p.storage()?p.y()+1:p.y(),az=p.storage()?p.z()+.5:p.z();
            var v=new org.joml.Vector4f((float)(ax-origin.x),(float)(ay-origin.y),(float)(az-origin.z),1).mul(matrix);
            if(v.w<=0)continue;float nx=v.x/v.w,ny=v.y/v.w;
            float anchorX=MarkerLabelGeometry.screenX(nx,w),anchorY=MarkerLabelGeometry.screenY(ny,h);
            // Project only the anchor. All label geometry uses a fixed physical-pixel density.
            float density=1.5f/Math.max(1,c.getWindow().getGuiScale());
            if(!MarkerLabelGeometry.visible(anchorX,anchorY,density,w,h))continue;
            g.pose().pushMatrix();g.pose().translate(anchorX,anchorY);g.pose().scale(density);
            String label=VelaDraw.ellipsize(p.name(),76,VelaDraw.Weight.MEDIUM,8);
            String distance=Math.round(Math.sqrt(c.player.distanceToSqr(p.x(),p.y(),p.z())))+"m";
            if(p.storage())StorageMarkerChrome.draw(g,label,distance,icon(p.item()),p.color());
            else MarkerOverlayChrome.label(new GameUiCanvas(g),label,distance,p.icon(),p.death()?VelaTheme.PRIMARY:p.color());
            g.pose().popMatrix();
        }
        }finally{g.pose().popMatrix();}
    }
}
