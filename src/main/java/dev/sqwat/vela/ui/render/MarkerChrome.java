package dev.sqwat.vela.ui.render;

/** Production drawing shared with the headless renderer; no Minecraft state. */
public final class MarkerChrome {
    private MarkerChrome(){}
    public static void frame(UiCanvas c,Rect r){c.round(r.x(),r.y(),r.width(),r.height(),8,VelaTheme.SURFACE);c.border(r.x(),r.y(),r.width(),r.height(),8,VelaTheme.BORDER);}
    public static void editor(UiCanvas c,MarkerGeometry.Editor e){
        c.round(e.x()+4,e.y(),e.width()-8,MarkerGeometry.EDITOR_HEIGHT,7,VelaTheme.WINDOW);
        c.text("Название метки",e.x()+10,e.y()+6,8,VelaTheme.TEXT_SECONDARY,UiCanvas.Weight.REGULAR);
        c.text("Цвет метки",e.x()+10,e.y()+56,8,VelaTheme.TEXT_SECONDARY,UiCanvas.Weight.REGULAR);
        for(int i=0;i<3;i++){Rect r=e.coordinate(i);c.text(new String[]{"X","Y","Z"}[i],r.x()+2,e.y()+79,8,VelaTheme.TEXT_SECONDARY,UiCanvas.Weight.REGULAR);}
        c.text("Иконка",e.x()+10,e.y()+128,8,VelaTheme.TEXT_SECONDARY,UiCanvas.Weight.REGULAR);
        Rect strip=e.icons();c.round(strip.x(),strip.y(),strip.width(),strip.height(),6,VelaTheme.SURFACE_DEEP);
    }
    public static void group(UiCanvas c,Rect r,String title,String description){
        PanelChrome.group(c,r,title,description);
    }
}
