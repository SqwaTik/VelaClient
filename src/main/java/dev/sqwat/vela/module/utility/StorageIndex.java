package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.marker.MarkerRuntime;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.KeySetting;
import dev.sqwat.vela.module.utility.StorageStore.Container;
import dev.sqwat.vela.module.utility.StorageStore.Key;
import dev.sqwat.vela.module.utility.StorageStore.Kind;
import dev.sqwat.vela.module.utility.StorageStore.Stack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import java.nio.file.Path;
import java.util.*;

/**
 * Storage Index: remembers what was in each container the player opened (contents as left when closing, so items
 * just taken are already gone) and lets the player search all of them at once (ui.screen.StorageIndexScreen).
 * Another player may change a chest unseen; every record therefore shows when it was last seen, and records whose
 * block is gone are dropped once the chunk is loaded nearby. Client thread only; saving happens off-thread on a copy.
 */
public final class StorageIndex extends ClientModule {
    private final KeySetting openKey=setting(new KeySetting("open_key","Открыть",org.lwjgl.glfw.GLFW.GLFW_KEY_I));
    private final BooleanSetting nested=setting(new BooleanSetting("nested","Шалкеры внутри","Учитывать содержимое шалкеров в сундуках",true));
    private StorageStore store;
    private String storeWorld;
    private Object screen;
    private Key openKeyPos;private Kind openKind;private int openTicks;private List<Stack> pending;
    private boolean keyHeld;private int validateCursor;private long changedAt;
    public StorageIndex(){super("storage_index","Storage Index","Поиск по всем открытым сундукам и шалкерам",ModuleCategory.UTILITIES);}
    @Override protected void onDisable(){flush(true);store=null;storeWorld=null;screen=null;pending=null;}

    public static StorageIndex current(){return dev.sqwat.vela.module.Modules.active(StorageIndex.class);}
    /** The store for the server the player is on, loading it on first use. */
    public StorageStore store(){
        var mc=Minecraft.getInstance();
        String world=mc.level==null?storeWorld:MarkerRuntime.world(mc);
        if(world==null)return new StorageStore();
        if(!world.equals(storeWorld)){
            flush(true);storeWorld=world;
            try{store=StorageStore.load(file(world));}catch(java.io.IOException error){
                org.slf4j.LoggerFactory.getLogger("Vela/Storage").warn("Cannot read storage index",error);store=new StorageStore();}
        }
        return store;
    }
    public String world(){return storeWorld;}
    private static Path file(String world){
        String name=Integer.toHexString(world.hashCode())+"-"+world.replaceAll("[^A-Za-z0-9._-]","_");
        if(name.length()>80)name=name.substring(0,80);
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("velaclient/storage/"+name+".bin");
    }

    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){flush(true);return;}
        boolean down=dev.sqwat.vela.ui.input.BindInput.down(mc.getWindow().handle(),openKey.value());
        if(down&&!keyHeld&&mc.gui.screen()==null&&mc.isWindowActive())mc.gui.setScreen(new dev.sqwat.vela.ui.screen.StorageIndexScreen(this));
        keyHeld=down;
        track(mc);
        if(mc.player.tickCount%40==0)validate(mc);
        flush(false);
    }
    /** Follows the open container screen; its last snapshot is recorded when it closes. */
    private void track(Minecraft mc){
        var current=mc.gui.screen();
        if(current!=screen){
            if(pending!=null&&openKeyPos!=null&&openTicks>=2)record(new Container(openKeyPos,openKind,System.currentTimeMillis(),pending));
            screen=current;pending=null;openKeyPos=null;openTicks=0;
            if(current instanceof AbstractContainerScreen<?>&&!(current instanceof InventoryScreen)&&!(current instanceof CreativeModeInventoryScreen))identify(mc);
        }
        if(openKeyPos!=null&&current instanceof AbstractContainerScreen<?> container){openTicks++;pending=snapshot(container);}
    }
    /** The container block the player just opened: the block under the crosshair. */
    private void identify(Minecraft mc){
        if(!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)return;
        BlockPos pos=hit.getBlockPos();
        var kind=kind(mc.level.getBlockEntity(pos));
        if(kind==null)return;
        openKind=kind;
        if(kind==Kind.ENDER){openKeyPos=new Key("ender",0,0,0);return;}
        var state=mc.level.getBlockState(pos);
        if(state.getBlock() instanceof ChestBlock&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){
            BlockPos other=pos.relative(ChestBlock.getConnectedDirection(state));
            if(other.compareTo(pos)<0)pos=other; // one key for both halves
        }
        openKeyPos=new Key(MarkerRuntime.dimension(mc),pos.getX(),pos.getY(),pos.getZ());
    }
    static Kind kind(BlockEntity entity){
        if(entity instanceof EnderChestBlockEntity)return Kind.ENDER;
        if(entity instanceof ShulkerBoxBlockEntity)return Kind.SHULKER;
        if(entity instanceof BarrelBlockEntity)return Kind.BARREL;
        if(entity instanceof ChestBlockEntity)return Kind.CHEST;
        if(entity instanceof HopperBlockEntity||entity instanceof DispenserBlockEntity)return Kind.OTHER;
        return null;
    }
    private List<Stack> snapshot(AbstractContainerScreen<?> screen){
        Map<String,int[]> totals=new LinkedHashMap<>();
        for(var slot:screen.getMenu().slots){
            if(slot.container instanceof Inventory)continue;
            add(totals,slot.getItem(),false);
            if(nested.value()){
                var contents=slot.getItem().get(DataComponents.CONTAINER);
                if(contents!=null)contents.nonEmptyItemCopyStream().forEach(inner->add(totals,inner,true));
            }
        }
        var items=new ArrayList<Stack>(totals.size());
        for(var e:totals.entrySet()){String[] p=e.getKey().split("\u0000",3);items.add(new Stack(p[0],p[1],e.getValue()[0],p[2].equals("1")));}
        return List.copyOf(items);
    }
    private static void add(Map<String,int[]> totals,ItemStack stack,boolean nested){
        if(stack.isEmpty())return;
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String name=stack.has(DataComponents.CUSTOM_NAME)?stack.getHoverName().getString():"";
        totals.computeIfAbsent(id+"\u0000"+name+"\u0000"+(nested?1:0),k->new int[1])[0]+=stack.getCount();
    }
    private void record(Container container){
        var s=store();
        if(container.items().isEmpty())s.remove(container.key());else s.put(container);
        changedAt=System.currentTimeMillis();
    }
    /** Drops a few records per call whose block is loaded nearby and is no longer a container. */
    private void validate(Minecraft mc){
        var s=store();if(s.size()==0)return;
        var list=new ArrayList<>(s.all());String dimension=MarkerRuntime.dimension(mc);
        for(int checked=0;checked<16&&!list.isEmpty();checked++){
            validateCursor=(validateCursor+1)%list.size();var c=list.get(validateCursor);
            if(!c.key().dimension().equals(dimension))continue;
            var pos=new BlockPos(c.key().x(),c.key().y(),c.key().z());
            if(pos.distSqr(mc.player.blockPosition())>64*64||!mc.level.isLoaded(pos))continue;
            if(kind(mc.level.getBlockEntity(pos))==null){s.remove(c.key());changedAt=System.currentTimeMillis();}
        }
    }
    public void forget(Key key){if(store().remove(key))changedAt=System.currentTimeMillis();}
    /** Saves a copy off-thread, at most every few seconds; {@code now} forces it (leaving the world). */
    private void flush(boolean now){
        if(store==null||storeWorld==null||!store.dirty())return;
        if(!now&&System.currentTimeMillis()-changedAt<5000)return;
        var copy=store.copy();var target=file(storeWorld);store.clean();
        net.minecraft.util.Util.ioPool().execute(()->{
            try{copy.save(target);}catch(java.io.IOException error){org.slf4j.LoggerFactory.getLogger("Vela/Storage").warn("Cannot save storage index",error);}
        });
    }
}
