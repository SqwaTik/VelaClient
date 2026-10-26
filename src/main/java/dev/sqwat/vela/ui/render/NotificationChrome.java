package dev.sqwat.vela.ui.render;
public final class NotificationChrome {
    private NotificationChrome(){}
    public static int compactWidth(float titleWidth,float detailWidth){return Math.min(208,Math.max(60,(int)Math.ceil(Math.max(titleWidth+34,detailWidth+16))));}
    public static int height(boolean detail,boolean action){return 28+(detail?28:0)+(action?28:0);}
    public static Rect action(int x,int y,int width,boolean detail){return new Rect(x+8,y+height(detail,true)-28,width-16,20);}
    public static void draw(UiCanvas c,int x,int y,int width,String title,String detail,String icon,int color,boolean action){
        int h=height(!detail.isBlank(),action);c.round(x,y,width,h,7,VelaTheme.SURFACE);c.border(x,y,width,h,7,VelaTheme.BORDER);
        if(icon.equals("alert")){
            c.border(x+8,y+8,12,12,6,color);c.fill(x+13.5f,y+10.5f,1,4,color);c.fill(x+13.5f,y+16,1,1,color);
        }else c.image(icon,x+8,y+8,12,color);
        c.text(title,x+26,y+10,8,VelaTheme.TEXT,UiCanvas.Weight.MEDIUM);
        if(!detail.isBlank())c.line(x+10,y+28,width-20,VelaTheme.BORDER);
        if(!detail.isBlank())c.text(detail,x+8,y+38,7,VelaTheme.TEXT_SECONDARY,UiCanvas.Weight.REGULAR);
    }
}
