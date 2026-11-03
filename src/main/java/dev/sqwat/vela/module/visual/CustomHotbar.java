package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import dev.sqwat.vela.ui.render.QuadBatch;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Glass restyle of the vanilla hotbar. Only the bar, selection and offhand sprites are replaced: items, counts,
 * durability, the attack indicator, the HUD-editor position/scale and the selection slide animation stay vanilla.
 */
public final class CustomHotbar extends ClientModule {
    private final NumberSetting opacity=setting(new NumberSetting("opacity","Плотность фона","Непрозрачность панели, %",78,30,100,2));
    private final BooleanSetting cells=setting(new BooleanSetting("cells","Ячейки","Отдельная подложка под каждым предметом",true));
    private final BooleanSetting accent=setting(new BooleanSetting("accent","Цвет клиента","Подсветка выбранного слота цветом темы",true));
    private final QuadBatch batch=new QuadBatch();
    public CustomHotbar(){super("custom_hotbar","Custom Hotbar","Стеклянный хотбар в стиле клиента",ModuleCategory.VISUALS);}

    public static CustomHotbar current(){
        return dev.sqwat.vela.module.Modules.active(CustomHotbar.class);
    }
    /** Same block as the other HUD panels; the opacity setting scales it. */
    private int background(){return dev.sqwat.vela.ui.hud.HudBlock.fill(opacity.value().floatValue()/100*255/dev.sqwat.vela.ui.hud.HudBlock.FILL_ALPHA);}

    /** Draws the replacement for a vanilla hotbar sprite; false when the sprite is not one of ours to replace. */
    public boolean draw(GuiGraphicsExtractor g,String sprite,int x,int y,int width,int height){
        switch(sprite){
            case "hud/hotbar"->{
                batch.begin(g);
                batch.roundedRect(x,y,width,height,6,background());
                if(cells.value()){
                    int cell=(VelaTheme.SURFACE_DEEP&0xFFFFFF)|(0x90<<24);
                    for(int i=0;i<9;i++)batch.roundedRect(x+2+i*20,y+2,18,18,4,cell);
                }
                batch.flush();
                dev.sqwat.vela.ui.render.VelaDraw.roundedOutline(g,x,y,width,height,6,dev.sqwat.vela.ui.hud.HudBlock.border(1),0);
                return true;
            }
            case "hud/hotbar_selection"->{
                // Vanilla selection is 24x23 starting one pixel outside the bar; the cell is 20x20 inside it.
                int rgb=accent.value()?VelaTheme.PRIMARY&0xFFFFFF:0xFFFFFF;
                batch.begin(g);
                batch.roundedRect(x+2,y+2,20,20,5,(0x46<<24)|rgb);
                batch.flush();
                dev.sqwat.vela.ui.render.VelaDraw.roundedOutline(g,x+2,y+2,20,20,5,(0xD0<<24)|rgb,0);
                return true;
            }
            case "hud/hotbar_offhand_left","hud/hotbar_offhand_right"->{
                boolean left=sprite.endsWith("left");
                batch.begin(g);
                batch.roundedRect(left?x+1:x+8,y+2,20,20,5,background());
                batch.flush();
                return true;
            }
            default->{return false;}
        }
    }
}
