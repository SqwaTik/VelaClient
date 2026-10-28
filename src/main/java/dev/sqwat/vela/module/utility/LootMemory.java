package dev.sqwat.vela.module.utility;

import com.google.gson.reflect.TypeToken;
import dev.sqwat.vela.core.JsonFileStore;
import dev.sqwat.vela.marker.MarkerRuntime;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.ActionSetting;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import dev.sqwat.vela.ui.Lang;
import dev.sqwat.vela.ui.render.GameUiCanvas;
import dev.sqwat.vela.ui.render.UiCanvas;
import dev.sqwat.vela.ui.render.UiKit;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import java.nio.file.Path;
import java.util.*;

/**
 * Remembers what you have already looted, per world, and marks it like a Vela marker, visible from afar: every chest, barrel, dispenser and chest minecart you opened
 * (and how much you left in it), every spawner you broke and every trial vault you unlocked. Those places get a
 * marker ("Обыскан · пусто", "Спавнер сломан"), so in a dungeon, a stronghold or on a busy server you never search
 * the same spot twice. Records go away by themselves once the block is gone. Saved in config/velaclient/loot.
 */
public final class LootMemory extends ClientModule {
    private final NumberSetting range=setting(new NumberSetting("range","Дальность меток","Дальше этого метки скрыты, блоков",512,32,2048,32));
    private final NumberSetting detail=setting(new NumberSetting("detail","Подробно ближе","Ближе этого — полная подпись, дальше — компактная метка, блоков",12,4,48,2));
    private final BooleanSetting spawners=setting(new BooleanSetting("spawners","Спавнеры","Отмечать сломанные спавнеры",true));
    private final BooleanSetting vaults=setting(new BooleanSetting("vaults","Хранилища испытаний","Отмечать открытые ключом хранилища",true));
    private final BooleanSetting age=setting(new BooleanSetting("age","Давность","Показывать, когда место было обыскано",true));

    public enum Kind{CONTAINER,MINECART,SPAWNER,VAULT}
    /** {@code left}: items still inside when you closed it (containers only). */
    public record Record(String dimension,int x,int y,int z,Kind kind,int left,long at){}
    private static final int LIMIT=5000;
    private final Map<String,Record> records=new LinkedHashMap<>();
    private JsonFileStore<List<Record>> store;
    private String storeWorld;
    private boolean dirty;
    private long lastSave;
    // The container screen being watched: its key, kind and the item count seen last.
    private Object screen;private Record opening;private int openTicks,lastCount;
    private int validateCursor;
    private static boolean hooked;

    public LootMemory(){
        super("loot_memory","Loot Memory","Помнит обысканные сундуки, сломанные спавнеры и открытые хранилища",ModuleCategory.UTILITIES);
        setting(new ActionSetting("forget_world","Забыть этот мир",this::forgetWorld));
    }
    /** For diagnostics: how many places are remembered in this world. */
    public String describe(){return records.size()+" records "+records.values();}
    /** Always answers, also when nothing was remembered here: the click must visibly do something. */
    private void forgetWorld(){
        load();
        int count=records.size();
        records.clear();dirty=true;save();
        dev.sqwat.vela.ui.Notifications.show(Lang.tr("Loot Memory"),count==0?Lang.tr("В этом мире ещё ничего не отмечено")
            :Lang.pick("Forgot "+count+" places in this world","Забыто мест в этом мире: "+count),"trash");
    }
    public static LootMemory current(){return dev.sqwat.vela.module.Modules.active(LootMemory.class);}
    private static String key(String dimension,int x,int y,int z){return dimension+"|"+x+"|"+y+"|"+z;}

    /** Block break and vault events (Fabric), registered once; they ask the active module. */
    public static void initialize(){
        if(hooked)return;hooked=true;
        net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents.AFTER.register((level,player,pos,state)->{
            var module=current();
            if(module!=null&&state.is(Blocks.SPAWNER))module.add(new Record(MarkerRuntime.dimension(Minecraft.getInstance()),pos.getX(),pos.getY(),pos.getZ(),Kind.SPAWNER,0,System.currentTimeMillis()));
        });
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            var module=current();var mc=Minecraft.getInstance();
            if(module!=null&&level.isClientSide()&&player==mc.player&&level.getBlockState(hit.getBlockPos()).is(Blocks.VAULT)){
                var held=player.getItemInHand(hand);
                if(held.is(Items.TRIAL_KEY)||held.is(Items.OMINOUS_TRIAL_KEY)){
                    var pos=hit.getBlockPos();
                    module.add(new Record(MarkerRuntime.dimension(mc),pos.getX(),pos.getY(),pos.getZ(),Kind.VAULT,0,System.currentTimeMillis()));
                }
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
    }

    private void load(){
        var mc=Minecraft.getInstance();
        String world=mc.level==null?storeWorld:MarkerRuntime.world(mc);
        if(world==null||world.equals(storeWorld))return;
        save();
        storeWorld=world;records.clear();
        String name=Integer.toHexString(world.hashCode())+"-"+world.replaceAll("[^A-Za-z0-9._-]","_");
        if(name.length()>80)name=name.substring(0,80);
        store=new JsonFileStore<>(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("velaclient/loot/"+name+".json"),
            new TypeToken<List<Record>>(){}.getType(),ArrayList::new);
        for(var r:store.load())if(r!=null&&r.kind()!=null&&r.dimension()!=null)records.put(key(r.dimension(),r.x(),r.y(),r.z()),r);
    }
    private void save(){
        if(!dirty||store==null)return;
        dirty=false;lastSave=System.currentTimeMillis();
        try{store.save(new ArrayList<>(records.values()));}catch(java.io.IOException error){org.slf4j.LoggerFactory.getLogger("Vela/Loot").warn("Cannot save loot memory",error);}
    }
    private void add(Record record){
        load();
        String k=key(record.dimension(),record.x(),record.y(),record.z());
        records.remove(k);records.put(k,record);
        while(records.size()>LIMIT){var it=records.keySet().iterator();it.next();it.remove();}
        dirty=true;
    }
    @Override protected void onDisable(){track(null);save();records.clear();storeWorld=null;store=null;}

    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){save();return;}
        load();
        track(mc);
        if(mc.player.tickCount%40==0)validate(mc);
        if(dirty&&System.currentTimeMillis()-lastSave>5000)save();
    }
    /** Follows the open container screen; on close the place is recorded with what was left in it. */
    private void track(Minecraft mc){
        var current=mc==null?null:mc.gui.screen();
        if(current!=screen){
            if(opening!=null&&openTicks>=2)add(new Record(opening.dimension(),opening.x(),opening.y(),opening.z(),opening.kind(),lastCount,System.currentTimeMillis()));
            screen=current;opening=null;openTicks=0;lastCount=0;
            if(mc!=null&&current instanceof AbstractContainerScreen<?>&&!(current instanceof InventoryScreen)&&!(current instanceof CreativeModeInventoryScreen))opening=identify(mc);
        }
        if(opening!=null&&current instanceof AbstractContainerScreen<?> container){
            openTicks++;int count=0;
            for(var slot:container.getMenu().slots)if(!(slot.container instanceof Inventory))count+=slot.getItem().getCount();
            lastCount=count;
        }
    }
    private static Record identify(Minecraft mc){
        String dimension=MarkerRuntime.dimension(mc);long now=System.currentTimeMillis();
        if(mc.hitResult instanceof EntityHitResult entity&&entity.getEntity() instanceof net.minecraft.world.entity.vehicle.minecart.MinecartChest cart){
            var pos=cart.blockPosition();
            return new Record(dimension,pos.getX(),pos.getY(),pos.getZ(),Kind.MINECART,0,now);
        }
        if(!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)return null;
        BlockPos pos=hit.getBlockPos();
        var entity=mc.level.getBlockEntity(pos);
        if(!(entity instanceof ChestBlockEntity||entity instanceof BarrelBlockEntity||entity instanceof DispenserBlockEntity))return null;
        if(entity instanceof EnderChestBlockEntity)return null;
        var state=mc.level.getBlockState(pos);
        if(state.getBlock() instanceof ChestBlock&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){
            BlockPos other=pos.relative(ChestBlock.getConnectedDirection(state));
            if(other.compareTo(pos)<0)pos=other; // one record for both halves
        }
        return new Record(dimension,pos.getX(),pos.getY(),pos.getZ(),Kind.CONTAINER,0,now);
    }
    /** A few records per call whose place is loaded nearby: dropped when the container is gone or the spawner is back. */
    private void validate(Minecraft mc){
        if(records.isEmpty())return;
        var list=new ArrayList<>(records.values());String dimension=MarkerRuntime.dimension(mc);
        for(int checked=0;checked<16&&!list.isEmpty();checked++){
            validateCursor=(validateCursor+1)%list.size();var r=list.get(validateCursor);
            if(!r.dimension().equals(dimension)||r.kind()==Kind.MINECART)continue;
            var pos=new BlockPos(r.x(),r.y(),r.z());
            if(pos.distSqr(mc.player.blockPosition())>64*64||!mc.level.isLoaded(pos))continue;
            var state=mc.level.getBlockState(pos);
            boolean gone=switch(r.kind()){
                case CONTAINER->!(mc.level.getBlockEntity(pos) instanceof ChestBlockEntity||mc.level.getBlockEntity(pos) instanceof BarrelBlockEntity||mc.level.getBlockEntity(pos) instanceof DispenserBlockEntity);
                case SPAWNER->state.is(Blocks.SPAWNER);
                case VAULT->!state.is(Blocks.VAULT);
                case MINECART->false;
            };
            if(gone){records.remove(key(r.dimension(),r.x(),r.y(),r.z()));dirty=true;}
        }
    }

    /**
     * Remembered places as markers (HUD, every frame): visible at any distance up to the range, like Vela markers —
     * a compact marker with the distance far away, the full tag (what is left, when) up close. Nearest 48 only.
     */
    public void draw(GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();
        if(records.isEmpty()||mc.player==null||mc.level==null||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        String dimension=MarkerRuntime.dimension(mc);double limit=range.value(),limitSq=limit*limit,close=detail.value();
        var camera=mc.gameRenderer.mainCamera();var origin=camera.position();
        var near=new ArrayList<Record>();
        for(var rec:records.values()){
            if(!rec.dimension().equals(dimension))continue;
            if(rec.kind()==Kind.SPAWNER&&!spawners.value()||rec.kind()==Kind.VAULT&&!vaults.value())continue;
            double dx=rec.x()+.5-origin.x,dy=rec.y()+.5-origin.y,dz=rec.z()+.5-origin.z;
            if(dx*dx+dy*dy+dz*dz<=limitSq)near.add(rec);
        }
        if(near.isEmpty())return;
        near.sort(Comparator.comparingDouble(rec->origin.distanceToSqr(rec.x()+.5,rec.y()+.5,rec.z()+.5)));
        var matrix=camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f());var point=new org.joml.Vector4f();
        float density=1.5f/Math.max(1,mc.getWindow().getGuiScale());long now=System.currentTimeMillis();
        g.pose().pushMatrix();g.pose().identity();
        try{
            for(int i=Math.min(near.size(),48)-1;i>=0;i--){ // nearest drawn last, on top
                var rec=near.get(i);
                double distance=Math.sqrt(origin.distanceToSqr(rec.x()+.5,rec.y()+.5,rec.z()+.5));
                boolean full=distance<=close;
                double top=rec.kind()==Kind.MINECART?1.1:rec.kind()==Kind.SPAWNER?.6:full?1.25:1.05;
                point.set((float)(rec.x()+.5-origin.x),(float)(rec.y()+top-origin.y),(float)(rec.z()+.5-origin.z),1).mul(matrix);
                if(point.w<=.05f)continue;
                float x=(point.x/point.w+1)*g.guiWidth()/2f,y=(1-point.y/point.w)*g.guiHeight()/2f;
                if(x<-60||y<-40||x>g.guiWidth()+60||y>g.guiHeight()+40)continue;
                g.pose().pushMatrix();
                try{
                    g.pose().translate(x,y);g.pose().scale(density);
                    if(full)tag(g,rec,now,1);
                    else dev.sqwat.vela.ui.render.MarkerOverlayChrome.label(new GameUiCanvas(g),shortTitle(rec),Math.round(distance)+Lang.pick(" m"," м"),"check",accent(rec));
                }finally{g.pose().popMatrix();}
            }
        }finally{g.pose().popMatrix();}
    }
    private static String shortTitle(Record rec){
        return switch(rec.kind()){
            case CONTAINER,MINECART->rec.left()==0?Lang.pick("Searched","Обыскан"):Lang.pick("Searched · "+rec.left(),"Обыскан · "+rec.left());
            case SPAWNER->Lang.pick("Spawner broken","Спавнер сломан");
            case VAULT->Lang.pick("Vault opened","Хранилище открыто");
        };
    }
    private static int accent(Record rec){
        return switch(rec.kind()){case CONTAINER,MINECART->rec.left()==0?0xFF8A93A6:0xFFFFA24D;case SPAWNER->VelaTheme.DANGER;case VAULT->0xFFFFC94D;};
    }
    private void tag(GuiGraphicsExtractor g,Record rec,long now,float alpha){
        String title=switch(rec.kind()){
            case CONTAINER,MINECART->rec.left()==0?Lang.pick("Searched · empty","Обыскан · пусто"):Lang.pick("Searched · "+rec.left()+" left","Обыскан · осталось "+rec.left());
            case SPAWNER->Lang.pick("Spawner broken","Спавнер сломан");
            case VAULT->Lang.pick("Vault opened","Хранилище открыто");
        };
        String detail=age.value()?UiKit.ago(now-rec.at()):"";
        var c=new GameUiCanvas(g);
        float tw=c.textWidth(title,8,UiCanvas.Weight.MEDIUM),dw=detail.isEmpty()?0:c.textWidth(detail,7,UiCanvas.Weight.REGULAR);
        float width=Math.max(tw,dw)+28,height=detail.isEmpty()?17:26;
        int accent=accent(rec);
        c.round(-width/2,-height,width,height,6,mul(VelaTheme.SURFACE,alpha*.92f));
        c.border(-width/2,-height,width,height,6,mul(VelaTheme.BORDER,alpha));
        c.image("check",-width/2+6,-height+4,9,mul(accent,alpha));
        c.text(title,-width/2+19,-height+4,8,mul(VelaTheme.TEXT,alpha),UiCanvas.Weight.MEDIUM);
        if(!detail.isEmpty())c.text(detail,-width/2+19,-height+15,7,mul(VelaTheme.TEXT_MUTED,alpha),UiCanvas.Weight.REGULAR);
    }
    private static int mul(int color,float alpha){return (Math.round(((color>>>24)&255)*Math.clamp(alpha,0,1))<<24)|(color&0xFFFFFF);}
}
