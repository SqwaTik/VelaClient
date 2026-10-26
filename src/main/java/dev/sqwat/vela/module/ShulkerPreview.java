package dev.sqwat.vela.module;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import java.util.List;

/** Uses only container contents already supplied to the client. */
public final class ShulkerPreview extends ClientModule {
    /** Undyed shulker boxes use the vanilla purple. */
    public static final int UNDYED=0x8F5E9C;
    public record Contents(List<ItemStack> items,int color) implements TooltipComponent {
        public Contents(List<ItemStack> items){this(items,UNDYED);}
    }
    private final dev.sqwat.vela.module.setting.BooleanSetting tint=setting(new dev.sqwat.vela.module.setting.BooleanSetting("tint","Цвет шалкера","Окрашивать превью в цвет красителя шалкера",true));
    public boolean tinted(){return tint.value();}
    private ItemContainerContents cached;
    private Contents preview;
    private Contents candidate,pinned;
    private Object screen,world;
    private boolean seen;
    private int x,y;
    public ShulkerPreview(){super("shulker_preview","Shulker Preview","Содержимое шалкера; удерживайте Ctrl для просмотра предметов",ModuleCategory.UTILITIES);}
    @Override protected void onDisable(){cached=null;preview=null;candidate=pinned=null;screen=world=null;seen=false;}
    @Override public void onClientTick(){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level!=world){onDisable();world=mc.level;}if(mc.level==null||mc.gui.screen()==null)onDisable();}
    public static ShulkerPreview current(){var s=dev.sqwat.vela.VelaClient.services();return s!=null&&s.modules().byId("shulker_preview") instanceof ShulkerPreview p&&p.enabled()?p:null;}
    public void frame(Object owner,boolean control){
        if(screen!=owner){candidate=pinned=null;seen=false;screen=owner;}
        if(!control)pinned=null;else if(pinned==null&&seen)pinned=candidate;
        seen=false;
    }
    public void observe(Contents contents,int x,int y,Object owner){if(pinned!=null)return;screen=owner;candidate=contents;this.x=x;this.y=y;seen=true;}
    public boolean pinned(){return pinned!=null;}
    public boolean contains(double mx,double my){return pinned()&&mx>=x-5&&mx<x+171&&my>=y-5&&my<y+63;}
    public static int slotAt(int x,int y,double mx,double my){
        int dx=(int)Math.floor(mx-x-2),dy=(int)Math.floor(my-y-2);
        if(dx<0||dy<0||dx>=162||dy>=54||dx%18>=17||dy%18>=17)return -1;
        return dy/18*9+dx/18;
    }
    public void drawPinned(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my){
        if(pinned==null)return;var mc=net.minecraft.client.Minecraft.getInstance();
        x=Math.clamp(x,5,Math.max(5,g.guiWidth()-171));y=Math.clamp(y,5,Math.max(5,g.guiHeight()-63));
        int border=dev.sqwat.vela.ui.render.VelaTheme.BORDER,surface=dev.sqwat.vela.ui.render.VelaTheme.SURFACE;
        if(tint.value()){
            int rgb=pinned.color()&0xFFFFFF;
            border=(border&0xFF000000)|(dev.sqwat.vela.ui.render.VelaTheme.mix(border,0xFF000000|rgb,.65f)&0xFFFFFF);
            surface=(surface&0xFF000000)|(dev.sqwat.vela.ui.render.VelaTheme.mix(surface,0xFF000000|rgb,.18f)&0xFFFFFF);
        }
        int slot=slotAt(x,y,mx,my);
        g.nextStratum();dev.sqwat.vela.ui.render.VelaDraw.roundedOutline(g,x-5,y-5,176,68,5,border,surface);
        new dev.sqwat.vela.ui.render.ShulkerTooltip(pinned,slot).extractImage(mc.font,x,y,166,58,g);
        if(slot>=0&&slot<pinned.items().size()){
            var item=pinned.items().get(slot);
            if(!item.isEmpty())g.setTooltipForNextFrame(mc.font,item,mx,my);
        }
    }
    public static Contents tooltip(ItemStack item){
        var s=dev.sqwat.vela.VelaClient.services();
        if(s==null||!(s.modules().byId("shulker_preview") instanceof ShulkerPreview module)||!module.enabled()
            ||!(item.getItem() instanceof BlockItem block)||!(block.getBlock() instanceof ShulkerBoxBlock))return null;
        var contents=item.get(DataComponents.CONTAINER);if(contents==null)return null;
        if(module.cached!=contents){
            var items=NonNullList.withSize(27,ItemStack.EMPTY);contents.copyInto(items);
            var dye=((ShulkerBoxBlock)block.getBlock()).getColor();
            module.preview=new Contents(List.copyOf(items),dye==null?UNDYED:dye.getTextureDiffuseColor()&0xFFFFFF);module.cached=contents;
        }
        return module.preview;
    }
}
