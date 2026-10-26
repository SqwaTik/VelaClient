package dev.sqwat.vela.ui.render;

/** Shared segment geometry keeps the entire painted option clickable. */
public final class SegmentedControl {
    public static void divider(UiCanvas canvas,Rect bounds){canvas.fill(bounds.x()+bounds.width()/2f-.5f,bounds.y()+5,1,bounds.height()-10,0xFF465064);}
    private SegmentedControl() {}
    public static Rect segment(Rect bounds,int index,int count) {
        if(count<1||index<0||index>=count)throw new IllegalArgumentException("Invalid segment");
        int left=bounds.x()+index*bounds.width()/count;
        int right=bounds.x()+(index+1)*bounds.width()/count;
        return new Rect(left+1,bounds.y()+1,right-left-2,bounds.height()-2);
    }
    public static void draw(UiCanvas canvas,Rect rect,String label,float active,float hover) {
        canvas.round(rect.x(),rect.y(),rect.width(),rect.height(),4,
            VelaTheme.mix(VelaTheme.mix(0xFF191E28,0xFF252C39,hover),VelaTheme.readableAccentFill(),active));
        canvas.text(label,rect.x()+(rect.width()-canvas.textWidth(label,8,UiCanvas.Weight.MEDIUM))/2f,
            rect.y()+(rect.height()-8)/2f,8,VelaTheme.TEXT,UiCanvas.Weight.MEDIUM);
    }
}
