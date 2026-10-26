package dev.sqwat.vela.ui.hud;

import dev.sqwat.vela.module.ModuleRegistry;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import java.util.function.ToIntFunction;

/**
 * Coordinates panel: dimension icon, position and (setting) the matching portal position in the other dimension.
 * The label is rebuilt only when the block position, dimension or settings change.
 */
final class CoordinatesHud {
    static final int HEIGHT=22;
    private static final Identifier OVERWORLD=Identifier.fromNamespaceAndPath("velaclient","textures/gui/dim_overworld.png"),
        NETHER=Identifier.fromNamespaceAndPath("velaclient","textures/gui/dim_nether.png");
    private static final int ICON=10,GAP=4,SPLIT=13;
    private final ModuleRegistry modules;
    private final ToIntFunction<String> labelWidth;
    private String text="",otherText="";
    private int x,y,z;
    private boolean compact,other;
    private Object dimension;

    CoordinatesHud(ModuleRegistry modules,ToIntFunction<String> labelWidth){this.modules=modules;this.labelWidth=labelWidth;}

    String label(Minecraft client){
        if(client.player==null)return "XYZ — — —";
        var module=modules.byId("coordinates");
        boolean compact=false,other=false;
        if(module!=null)for(var s:module.settings()){
            if(s.id().equals("format")&&"Compact".equals(s.value()))compact=true;
            else if(s.id().equals("other_dimension")&&Boolean.TRUE.equals(s.value()))other=true;
        }
        int x=client.player.getBlockX(),y=client.player.getBlockY(),z=client.player.getBlockZ();
        var dimension=client.level==null?null:client.level.dimension();
        if(text.isEmpty()||x!=this.x||y!=this.y||z!=this.z||compact!=this.compact||other!=this.other||dimension!=this.dimension){
            this.x=x;this.y=y;this.z=z;this.compact=compact;this.other=other;this.dimension=dimension;
            text=(compact?"":"XYZ ")+x+" "+y+" "+z;
            // Portal maths: one Nether block is eight Overworld blocks. The End has no counterpart.
            otherText=!other?"":dimension==Level.OVERWORLD?Math.floorDiv(x,8)+" "+Math.floorDiv(z,8):dimension==Level.NETHER?x*8+" "+z*8:"";
        }
        return text;
    }
    /** Earth for the Overworld, a portal for the Nether, none for the End. */
    private static Identifier icon(Object dimension){return dimension==Level.OVERWORLD?OVERWORLD:dimension==Level.NETHER?NETHER:null;}
    int width(Minecraft client){
        String main=label(client);
        int width=16+labelWidth.applyAsInt(main);
        if(client.player==null)return width;
        if(icon(dimension)!=null)width+=ICON+GAP;
        if(!otherText.isEmpty())width+=SPLIT+ICON+GAP+labelWidth.applyAsInt(otherText);
        return width;
    }
    /** {@code panel} draws the background at the given size. */
    void draw(GuiGraphicsExtractor g,Minecraft client,java.util.function.IntConsumer panel){
        panel.accept(width(client));
        String main=label(client);
        int x=8;var icon=client.player==null?null:icon(dimension);
        if(icon!=null){VelaDraw.blit(g,icon,x,6,ICON,ICON,0,0,64,64,64,64,0xFFFFFFFF);x+=ICON+GAP;}
        VelaDraw.text(g,client.font,main,x,7,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM);x+=labelWidth.applyAsInt(main);
        if(client.player==null||otherText.isEmpty())return;
        g.fill(x+6,6,x+7,16,VelaTheme.BORDER_HOVER);x+=SPLIT;
        VelaDraw.blit(g,dimension==Level.OVERWORLD?NETHER:OVERWORLD,x,6,ICON,ICON,0,0,64,64,64,64,0xFFFFFFFF);
        VelaDraw.text(g,client.font,otherText,x+ICON+GAP,7,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.MEDIUM);
    }
}
