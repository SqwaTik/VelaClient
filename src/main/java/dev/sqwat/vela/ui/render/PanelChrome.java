package dev.sqwat.vela.ui.render;

/** Interior group used by every dashboard drawer. */
public final class PanelChrome {
    private PanelChrome(){}
    public static void group(UiCanvas c,Rect r,String title,String description){
        c.round(r.x(),r.y(),r.width(),r.height(),7,VelaTheme.WINDOW);
        c.text(title,r.x()+7,r.y()+7,9,VelaTheme.TEXT,UiCanvas.Weight.MEDIUM);
        c.text(description,r.x()+7,r.y()+23,6,VelaTheme.TEXT_MUTED,UiCanvas.Weight.REGULAR);
    }
}
