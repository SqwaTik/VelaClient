package dev.sqwat.vela.ui.render;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Raster assets exported directly from the supplied Figma Make design. */
public final class VelaIcons {
    private static final java.util.Set<String> CUSTOM=java.util.Set.of("contact_options","compose","singleplayer","power","multiplayer","armor","potions","speakers","cooldowns","inventory","target");
    private static final Map<String,Identifier> TEXTURES=Stream.of("account","visuals","hud","utilities","markers","friends","configs","settings","search","arrow","back","x","plus","check","trash","copy","folder","key","star","star_filled","download","save","move","microsoft","chevron").collect(Collectors.toUnmodifiableMap(s->s,s->Identifier.fromNamespaceAndPath("velaclient","textures/gui/icon_"+s+".png")));
    private VelaIcons(){}
    public static void tab(GuiGraphicsExtractor g,String kind,int x,int y,int color){
        draw(g,kind.toLowerCase(Locale.ROOT),x,y,11,color);
    }
    public static void draw(GuiGraphicsExtractor g,String kind,int x,int y,int size,int color){
        PngAssets.icon(g,kind.equals("heart")||kind.equals("attack")||kind.equals("movement")||kind.equals("skull")||kind.equals("near_arrow")||TEXTURES.containsKey(kind)||CUSTOM.contains(kind)?kind:"settings",x,y,size,color);
    }
}
