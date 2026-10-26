package dev.sqwat.vela.ui.render;

import dev.sqwat.vela.module.setting.*;
import java.util.List;

/** One layout contract for card height, setting rows and clipped pointer targets. */
public final class ModuleSettingsGeometry {
    public static final int HEADER_HEIGHT=44;
    public static final int CONTENT_OFFSET=47;
    public static final int FIRST_ROW_OFFSET=5;
    private ModuleSettingsGeometry(){}
    public static int rowHeight(Setting<?> setting){return setting instanceof NumberSetting||setting instanceof ActionSetting?35:setting instanceof ChoiceSetting||setting instanceof TextSetting?44:20;}
    public static int extraHeight(List<Setting<?>> settings){return CONTENT_OFFSET+FIRST_ROW_OFFSET-HEADER_HEIGHT+settings.stream().mapToInt(ModuleSettingsGeometry::rowHeight).sum();}
    public static Rect row(int x,int cursor,int width,Setting<?> setting){return new Rect(x,cursor-2,width,rowHeight(setting)-3);}
    public static Rect checkbox(int x,int cursor,int width){return new Rect(x+width-13,cursor-2,11,11);}
    public static Rect visibleHit(Rect target,Rect card,Rect viewport){
        int left=Math.max(target.x(),Math.max(card.x()+2,viewport.x()));
        int top=Math.max(target.y(),Math.max(card.y()+HEADER_HEIGHT,viewport.y()));
        int right=Math.min(target.x()+target.width(),Math.min(card.x()+card.width()-2,viewport.x()+viewport.width()));
        int bottom=Math.min(target.y()+target.height(),Math.min(card.y()+card.height()-2,viewport.y()+viewport.height()));
        return new Rect(left,top,Math.max(0,right-left),Math.max(0,bottom-top));
    }
}
