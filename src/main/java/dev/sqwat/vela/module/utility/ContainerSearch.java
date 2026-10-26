package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;

/** Search box on container screens (ui.screen.ContainerSearchBox): matching slots get an accent frame, the rest dim. */
public final class ContainerSearch extends ClientModule {
    private final BooleanSetting remember=setting(new BooleanSetting("remember","Запоминать запрос","Сохранять текст поиска между окнами",false));
    private String query="";
    public ContainerSearch(){super("container_search","Container Search","Поиск предметов в инвентаре и сундуках; Ctrl+F",ModuleCategory.UTILITIES);}
    public static ContainerSearch current(){
        var s=dev.sqwat.vela.VelaClient.services();
        return s!=null&&s.modules().byId("container_search") instanceof ContainerSearch m&&m.enabled()?m:null;
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
