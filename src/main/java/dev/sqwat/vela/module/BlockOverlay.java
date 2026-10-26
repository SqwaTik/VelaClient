package dev.sqwat.vela.module;

import dev.sqwat.vela.module.setting.*;
import java.util.List;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.gizmos.*;
import net.minecraft.world.phys.Vec3;

public final class BlockOverlay extends ClientModule {
    private final ChoiceSetting mode=setting(new ChoiceSetting("visibility","Отображение","Видимость выделения","Только видимое",List.of("Только видимое","Весь блок")));
    private final BooleanSetting outline=setting(new BooleanSetting("outline","Обводка","Контур блока",true));
    private final BooleanSetting fill=setting(new BooleanSetting("fill","Заливка","Полупрозрачная заливка",true));
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Использовать цвет темы",true));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет",0x8877FF));
    public BlockOverlay(){super("block_overlay","Block Overlay","Выделение блока под прицелом",ModuleCategory.UTILITIES);color.visibleWhen(this::customColor);}
    public boolean customColor(){return !theme.value();}
    public static BlockOverlay active(){var s=dev.sqwat.vela.VelaClient.services();return s!=null&&s.modules().byId("block_overlay") instanceof BlockOverlay m&&m.enabled()?m:null;}
    public void submit(BlockOutlineRenderState state){
        if(state==null)return;
        int rgb=theme.value()?dev.sqwat.vela.ui.render.VelaTheme.parseHex(dev.sqwat.vela.VelaClient.services().configs().current().accentColor):color.value();
        rgb&=0xFFFFFF;final int stroke=0xE0000000|rgb,face=0x38000000|rgb;
        boolean through=mode.value().equals("Весь блок");
        var pos=state.pos();
        if(fill.value())state.shape().forAllBoxes((x0,y0,z0,x1,y1,z1)->{
            var box=new net.minecraft.world.phys.AABB(x0,y0,z0,x1,y1,z1).move(pos).inflate(.001);
            var gizmo=Gizmos.cuboid(box,GizmoStyle.fill(face));if(through)gizmo.setAlwaysOnTop();
        });
        if(outline.value())state.shape().forAllEdges((x0,y0,z0,x1,y1,z1)->{
            var gizmo=Gizmos.line(new Vec3(pos.getX()+x0,pos.getY()+y0,pos.getZ()+z0),new Vec3(pos.getX()+x1,pos.getY()+y1,pos.getZ()+z1),stroke,2f);
            if(through)gizmo.setAlwaysOnTop();
        });
    }
}
