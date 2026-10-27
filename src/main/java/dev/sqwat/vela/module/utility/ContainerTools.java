package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;

/**
 * Container screen tools: the search box (ui.screen.ContainerSearchBox, highlight in ContainerSearchMixin) and
 * Shift+LMB sweeping (ItemScrollerMixin).
 */
public final class ContainerTools extends ClientModule {
    private final BooleanSetting search=setting(new BooleanSetting("search","Поиск","Поле поиска над окном контейнера",true));
    private final BooleanSetting remember=setting(new BooleanSetting("remember","Запоминать запрос","Сохранять текст поиска между окнами",false));
    private final BooleanSetting scroller=setting(new BooleanSetting("scroller","Быстрый перенос","Зажмите клавишу и проведите ЛКМ по слотам",true));
    private final NumberSetting delay=setting(new NumberSetting("delay_ms","Задержка, мс","Пауза между переносом соседних слотов",20,0,100,1));
    private final dev.sqwat.vela.module.setting.KeySetting searchKey=setting(new dev.sqwat.vela.module.setting.KeySetting("search_combo","Клавиша поиска",dev.sqwat.vela.ui.input.BindInput.combo(org.lwjgl.glfw.GLFW.GLFW_KEY_F,dev.sqwat.vela.ui.input.BindInput.CTRL)));
    private final dev.sqwat.vela.module.setting.KeySetting sweepKey=setting(new dev.sqwat.vela.module.setting.KeySetting("sweep_key","Клавиша переноса",org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT));
    private String query="";
    public ContainerTools(){
        super("container_tools","Container Tools","Поиск предметов и быстрый перенос в контейнерах",ModuleCategory.UTILITIES);
        remember.visibleWhen(search::value);delay.visibleWhen(scroller::value);searchKey.visibleWhen(search::value);sweepKey.visibleWhen(scroller::value);
    }
    private static ContainerTools module(){
        return dev.sqwat.vela.module.Modules.active(ContainerTools.class);
    }
    /** The module when the search box is on, otherwise null. */
    public static ContainerTools search(){var m=module();return m!=null&&m.search.value()?m:null;}
    /** Sweep delay in ms, or -1 when sweeping is off. */
    public static int sweepDelay(){var m=module();return m!=null&&m.scroller.value()?m.delay.value().intValue():-1;}
    /** Key or combination that focuses the search box (Ctrl+F by default). */
    public int searchKey(){return searchKey.value();}
    /** Whether the quick-move key is held (Left Shift by default; either Shift then). */
    public static boolean sweepHeld(long window){
        var m=module();if(m==null)return false;int key=m.sweepKey.value();
        return dev.sqwat.vela.ui.input.BindInput.down(window,key)||key==org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT&&dev.sqwat.vela.ui.input.BindInput.down(window,org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
    public String initialQuery(){if(!remember.value())query="";return query;}
    public void query(String value){query=value==null?"":value;}
    /** Case-insensitive match on the shown name and the item id (so English ids work in a Russian client). */
    public static boolean matches(ItemStack stack,String query){
        if(query.isBlank()||stack.isEmpty())return false;
        String needle=query.strip().toLowerCase(Locale.ROOT);
        if(stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle))return true;
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().replace('_',' ').contains(needle.replace('_',' '));
    }
    /** Total item count across matching visible slots. */
    public static int count(AbstractContainerMenu menu,String query){
        int total=0;
        for(var slot:menu.slots)if(slot.isActive()&&matches(slot.getItem(),query))total+=slot.getItem().getCount();
        return total;
    }
}
