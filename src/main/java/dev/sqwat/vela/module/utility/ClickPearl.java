package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.KeySetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

/** Select, use and restore on separate ticks using normal game input. */
public final class ClickPearl extends ClientModule {
    private final KeySetting key=setting(new KeySetting("key","Клавиша"));
    private boolean held;
    private boolean queued;
    private int phase,waitTicks,previous=-1,pearl=-1;
    private Object owner,world;
    public ClickPearl(){super("click_pearl","Click Pearl","Бросок жемчуга с возвратом выбранного слота",ModuleCategory.UTILITIES);}
    public static void pressed(int code){
        var c=Minecraft.getInstance();var services=dev.sqwat.vela.VelaClient.services();
        if(services!=null&&c.gui.screen()==null&&c.isWindowActive()&&services.modules().byId("click_pearl") instanceof ClickPearl pearl&&pearl.enabled()&&pearl.key.value()==code)pearl.queued=true;
    }
    @Override public void onClientTick(){
        var c=Minecraft.getInstance();
        boolean pressed=dev.sqwat.vela.ui.input.BindInput.down(c.getWindow().handle(),key.value());
        boolean tapped=queued||pressed&&!held;queued=false;held=pressed;
        if(c.player==null||c.level==null||c.gameMode==null||c.gui.screen()!=null||c.gui.overlay()!=null||!c.isWindowActive()||c.isPaused()||!c.player.isAlive()||c.player.isSpectator()||c.player.containerMenu!=c.player.inventoryMenu){cancel(c);return;}
        if(phase!=0){
            if(owner!=c.player||world!=c.level||c.player.getInventory().getSelectedSlot()!=pearl){clear();return;}
            if(phase==2){cancel(c);return;}
            if(c.player.isUsingItem()||!c.player.getMainHandItem().is(Items.ENDER_PEARL)||c.player.getCooldowns().isOnCooldown(c.player.getMainHandItem())){cancel(c);return;}
            // Let the slot change render, but do not depend on hand-animation settings.
            if(++waitTicks<2)return;
            var result=c.gameMode.useItem(c.player,net.minecraft.world.InteractionHand.MAIN_HAND);
            if(result.consumesAction())c.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            phase=2;return;
        }
        if(!tapped||c.player.isUsingItem()||c.gameMode.isDestroying())return;
        int slot=findHotbarSlot(i->c.player.getInventory().getItem(i).is(Items.ENDER_PEARL));
        if(slot<0||c.player.getCooldowns().isOnCooldown(c.player.getInventory().getItem(slot)))return;
        previous=c.player.getInventory().getSelectedSlot();pearl=slot;owner=c.player;world=c.level;
        c.player.getInventory().setSelectedSlot(slot);phase=1;
    }
    public static int findHotbarSlot(java.util.function.IntPredicate pearlAt){for(int i=0;i<9;i++)if(pearlAt.test(i))return i;return -1;}
    private void cancel(Minecraft c){
        if(phase!=0&&owner==c.player&&world==c.level&&c.player!=null&&c.player.getInventory().getSelectedSlot()==pearl)c.player.getInventory().setSelectedSlot(previous);
        clear();
    }
    private void clear(){phase=waitTicks=0;previous=pearl=-1;owner=world=null;}
    @Override protected void onDisable(){cancel(Minecraft.getInstance());held=queued=false;}
}
