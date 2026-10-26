package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Feed of items entering (and optionally leaving) the inventory, drawn by HudLayoutService as the "pickups" panel.
 * Works from inventory totals, so moving items around inside the inventory produces nothing; repeats of one item
 * merge into a single row with a growing counter. Client thread only.
 */
public final class ItemPickups extends ClientModule {
    public static final int MAX_ROWS=6;
    public static final long SLIDE_MS=220,FADE_MS=450;
    /** A feed row; {@code amount} is negative for items that left the inventory. */
    public static final class Entry {
        public final Item item;public final ItemStack icon;public int amount,total;public long born,bumped;
        Entry(Item item,ItemStack icon,int amount,int total,long now){this.item=item;this.icon=icon;this.amount=amount;this.total=total;born=bumped=now;}
    }
    private final NumberSetting duration=setting(new NumberSetting("duration","Длительность","Сколько секунд строка остаётся на экране",4,1,10,.5));
    private final BooleanSetting losses=setting(new BooleanSetting("losses","Расход","Показывать и убывание: брошенные жемчуги, съеденное",false));
    private final BooleanSetting containers=setting(new BooleanSetting("containers","Из сундуков","Учитывать предметы, взятые из контейнеров",true));
    private final BooleanSetting totals=setting(new BooleanSetting("totals","Всего","Сколько таких предметов теперь в инвентаре",true));
    // Every HUD panel has this switch; HudLayoutService reads it by id.
    private final BooleanSetting transparent=setting(new BooleanSetting("transparent","Прозрачный","Заменяет обычный фон прозрачным стеклом",false));
    private final List<Entry> entries=new ArrayList<>();
    private final Map<Item,Integer> counts=new HashMap<>();
    private final Map<Item,ItemStack> samples=new HashMap<>();
    private Object player;
    /** Ticks to wait after joining or respawning: the server fills the inventory a moment later. */
    private int settle;
    public ItemPickups(){super("pickups","Item Pickups","Лента подобранных предметов с анимацией",ModuleCategory.HUD);}
    @Override protected void onDisable(){entries.clear();counts.clear();player=null;}

    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();var p=mc.player;
        long now=System.currentTimeMillis();
        entries.removeIf(e->now-e.born>lifetime()+FADE_MS);
        if(p==null||p.isSpectator()||p.isCreative()){counts.clear();player=null;return;}
        Map<Item,Integer> next=new HashMap<>();samples.clear();
        var inventory=p.getInventory();
        for(int slot=0;slot<inventory.getContainerSize();slot++)add(next,inventory.getItem(slot));
        add(next,p.containerMenu.getCarried()); // an item held on the cursor is still ours
        if(player!=p||p.isDeadOrDying()){player=p;settle=20;}
        boolean fresh=settle>0;if(settle>0)settle--;
        boolean container=mc.gui.screen() instanceof AbstractContainerScreen<?>&&!(mc.gui.screen() instanceof InventoryScreen);
        if(!fresh&&(containers.value()||!container)){
            for(var e:next.entrySet()){int delta=e.getValue()-counts.getOrDefault(e.getKey(),0);if(delta>0)push(e.getKey(),delta,e.getValue(),now);}
            if(losses.value())for(var e:counts.entrySet()){int left=next.getOrDefault(e.getKey(),0),delta=left-e.getValue();if(delta<0)push(e.getKey(),delta,left,now);}
        }
        counts.clear();counts.putAll(next);
    }
    private void add(Map<Item,Integer> into,ItemStack stack){
        if(stack.isEmpty())return;
        into.merge(stack.getItem(),stack.getCount(),Integer::sum);samples.putIfAbsent(stack.getItem(),stack);
    }
    private void push(Item item,int delta,int total,long now){
        for(var e:entries)if(e.item==item&&Integer.signum(e.amount)==Integer.signum(delta)&&now-e.born<lifetime()){
            e.amount+=delta;e.total=total;e.born=now;e.bumped=now;entries.remove(e);entries.addFirst(e);return;
        }
        var sample=samples.get(item);
        entries.addFirst(new Entry(item,sample==null?new ItemStack(item):sample.copyWithCount(1),delta,total,now));
        while(entries.size()>MAX_ROWS)entries.removeLast();
    }
    public long lifetime(){return Math.round(duration.value()*1000);}
    /** Preview rows for the HUD editor. */
    public static List<Entry> samples(){
        return List.of(new Entry(net.minecraft.world.item.Items.DIAMOND,new ItemStack(net.minecraft.world.item.Items.DIAMOND),3,27,0),
            new Entry(net.minecraft.world.item.Items.ENDER_PEARL,new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL),16,16,0),
            new Entry(net.minecraft.world.item.Items.GOLDEN_APPLE,new ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE),-1,63,0));
    }
    public boolean totals(){return totals.value();}
    public List<Entry> entries(){return entries;}
    public boolean empty(){return entries.isEmpty();}
}
