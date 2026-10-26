import dev.sqwat.vela.ui.render.*;
import dev.sqwat.vela.module.setting.*;
import java.nio.file.*;
import java.util.List;

/** Diagnostic overlay of production checkbox geometry and its pointer target. */
public final class RenderSettingHitPreview {
    public static void main(String[] args)throws Exception {
        Path assets=Path.of(args[0]),out=Path.of(args[1]);Files.createDirectories(out);
        for(float density:new float[]{1,1.5f,2}){
            var c=new RenderPngPreview(assets,Math.round(510*density),Math.round(150*density),density);
            var setting=new BooleanSetting("transparent","Прозрачный","",false);
            for(int column=0;column<2;column++){
                int x=14+column*246,y=38;
                c.text(column==0?"Полное раскрытие":"Частичное раскрытие",x,14,9,VelaTheme.TEXT,UiCanvas.Weight.MEDIUM);
                Rect card=new Rect(x,y,230,column==0?44+ModuleSettingsGeometry.extraHeight(List.of(setting)):60);
                c.round(card.x(),card.y(),card.width(),card.height(),7,VelaTheme.SURFACE);
                c.border(card.x(),card.y(),card.width(),card.height(),7,VelaTheme.BORDER);
                c.text("Armor HUD",x+8,y+8,9,VelaTheme.TEXT,UiCanvas.Weight.MEDIUM);
                c.text("Отображение прочности брони",x+8,y+24,8,VelaTheme.TEXT_MUTED,UiCanvas.Weight.REGULAR);
                int cursor=y+52;
                var previousClip=c.graphics.getClip();
                c.graphics.clip(new java.awt.geom.Rectangle2D.Float((x+2)*density,(y+44)*density,(card.width()-4)*density,(card.height()-46)*density));
                c.text("Прозрачный",x+8,cursor,8,VelaTheme.TEXT_SECONDARY,UiCanvas.Weight.REGULAR);
                Rect checkbox=ModuleSettingsGeometry.checkbox(x+8,cursor,214);
                ControlChrome.checkbox(c,checkbox,1);
                c.graphics.setClip(previousClip);
                Rect hit=ModuleSettingsGeometry.visibleHit(ModuleSettingsGeometry.row(x+8,cursor,214,setting),card,new Rect(0,0,510,150));
                c.border(hit.x(),hit.y(),hit.width(),hit.height(),1,0xFF77DD88);
            }
            c.text("Зелёный контур — активная область клика",14,127,8,VelaTheme.TEXT_SECONDARY,UiCanvas.Weight.REGULAR);
            c.save(out.resolve("setting-hit-"+density+".png"));
        }
    }
}
