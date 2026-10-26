import dev.sqwat.vela.ui.render.*;
import dev.sqwat.vela.ui.animation.AnimationState;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;

/** Contact sheets use the same geometry and painters as the running client. */
public class RenderMarkerPreview {
    private static final String[] ICONS={"markers","star","account","hud","utilities","visuals","friends","move","skull"};
    public static void main(String[] args)throws Exception{
        Path assets=Path.of(args[0]),out=Path.of(args[1]);Files.createDirectories(out);VelaTheme.setPrimary(0xFFEF68F0);
        for(float density:new float[]{1,1.5f,2}){
            var c=new RenderPngPreview(assets,Math.round(720*density),Math.round(620*density),density);
            creation(c,new MarkerGeometry.Creation(14,14,430,1));
            settings(c,new Rect(460,14,180,278),1);
            creation(c,new MarkerGeometry.Creation(14,310,190,1));
            creation(c,new MarkerGeometry.Creation(220,310,190,.5f));
            creation(c,new MarkerGeometry.Creation(426,310,190,0));
            c.save(out.resolve("markers-layout-"+density+".png"));
        }
        var sheet=new RenderPngPreview(assets,1200,180,1.5f);var time=new AtomicLong();var anim=new AnimationState(time::get);anim.value("check",0,false);anim.value("slider",.1f,false);
        for(int i=0;i<8;i++){time.set(i*40_000_000L);float p=anim.value("check",1,false),s=anim.value("slider",.9f,false);int x=12+i*98;sheet.text(i*40+" ms",x,12,8,VelaTheme.TEXT,UiCanvas.Weight.REGULAR);ControlChrome.checkbox(sheet,new Rect(x,32,15,15),p);ControlChrome.slider(sheet,new Rect(x,63,78,5),s);var transform=sheet.graphics.getTransform();sheet.graphics.rotate(ControlChrome.disclosureAngle(p),(x+8)*sheet.density,99*sheet.density);sheet.image("chevron",x+1,92,14,VelaTheme.TEXT_SECONDARY);sheet.graphics.setTransform(transform);}
        sheet.save(out.resolve("animation-frames.png"));
    }
    private static void creation(RenderPngPreview c,MarkerGeometry.Creation layout){
        MarkerChrome.frame(c,layout.frame());c.image("markers",layout.x()+10,layout.y()+12,16,VelaTheme.PRIMARY);c.text("Создание метки",layout.x()+34,layout.y()+10,9,VelaTheme.TEXT,UiCanvas.Weight.MEDIUM);c.text("Создавайте новые метки",layout.x()+34,layout.y()+26,8,VelaTheme.TEXT_SECONDARY,UiCanvas.Weight.REGULAR);
        if(layout.expansion()>0){java.awt.Shape savedClip=c.graphics.getClip();c.graphics.clip(new java.awt.geom.Rectangle2D.Float(layout.x()*c.density,(layout.y()+44)*c.density,layout.width()*c.density,layout.editorHeight()*c.density));var e=layout.editor();MarkerChrome.editor(c,e);field(c,e.name(),"Метка 3");for(int i=0;i<3;i++)field(c,e.coordinate(i),new String[]{"23","66","0"}[i]);Rect color=e.color();c.round(color.x()+5,color.y()+5,12,12,6,0xFFFF8F33);c.text("#FF8F33",color.x()+22,color.y()+7,8,VelaTheme.TEXT_SECONDARY,UiCanvas.Weight.REGULAR);Rect chosen=e.icon(0,ICONS.length);int tile=Math.min(24,chosen.width()-2);c.round(chosen.x()+(chosen.width()-tile)/2,chosen.y(),tile,24,5,0xFFFF8F33);for(int i=0;i<ICONS.length;i++){Rect r=e.icon(i,ICONS.length);c.image(ICONS[i],r.x()+(r.width()-13)/2,r.y()+5,13,i==0?0xFFFFFFFF:VelaTheme.TEXT_SECONDARY);}c.graphics.setClip(savedClip);}
        Rect action=layout.action();c.round(action.x(),action.y(),action.width(),action.height(),8,VelaTheme.SURFACE);c.border(action.x(),action.y(),action.width(),action.height(),8,VelaTheme.BORDER);c.centered("Создать",action.x()+action.width()/2f,action.y()+7,8,VelaTheme.TEXT,UiCanvas.Weight.MEDIUM);
    }
    private static void field(RenderPngPreview c,Rect r,String text){c.round(r.x(),r.y(),r.width(),r.height(),6,VelaTheme.SURFACE_DEEP);c.text(text,r.x()+8,r.y()+7,8,VelaTheme.TEXT,UiCanvas.Weight.REGULAR);}
    private static void settings(RenderPngPreview c,Rect area,float progress){
        MarkerChrome.frame(c,area);if(area.width()<150)return;
        c.text("Настройки меток",area.x()+22,area.y()+16,9,VelaTheme.TEXT,UiCanvas.Weight.MEDIUM);c.round(area.x()+11,area.y()+14,3,14,1,VelaTheme.PRIMARY);c.line(area.x()+12,area.y()+39,area.width()-24,VelaTheme.BORDER);
        var gs=MarkerSettingsGeometry.place(new Rect(area.x()+4,area.y()+43,area.width()-8,area.height()-47),progress,progress,progress);Rect[] groups={gs.death(),gs.limit(),gs.quick()};String[] titles={"Метки смерти","Лимит меток","Быстрая метка"},details={"Сохранять место гибели","Количество сохранённых точек","Установить точку по взгляду"};
        for(int i=0;i<3;i++){Rect r=groups[i];MarkerChrome.group(c,r,titles[i],details[i]);ControlChrome.checkbox(c,new Rect(r.x()+r.width()-22,r.y()+7,13,13),progress);}
        if(progress>.95f){Rect d=groups[0],l=groups[1],q=groups[2];c.text("Только последняя",d.x()+7,d.y()+38,8,VelaTheme.TEXT_SECONDARY,UiCanvas.Weight.REGULAR);ControlChrome.checkbox(c,new Rect(d.x()+d.width()-24,d.y()+35,13,13),0);c.text("60",l.x()+l.width()-26,l.y()+33,8,VelaTheme.TEXT_SECONDARY,UiCanvas.Weight.REGULAR);ControlChrome.slider(c,new Rect(l.x()+8,l.y()+52,l.width()-16,5),50/240f);c.text("Клавиша",q.x()+8,q.y()+42,8,VelaTheme.TEXT_SECONDARY,UiCanvas.Weight.REGULAR);field(c,new Rect(q.x()+q.width()-64,q.y()+34,56,23),"M5");}
    }
}
