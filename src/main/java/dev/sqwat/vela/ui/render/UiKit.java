package dev.sqwat.vela.ui.render;

import dev.sqwat.vela.ui.input.TextEdit;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared Vela components. Every screen draws chips, cards, fields, badges and empty states through these, so sizes,
 * paddings and colours stay identical everywhere. Geometry is in GUI units; hover/active amounts are 0..1 and come
 * from the caller's animation state.
 */
public final class UiKit {
    private UiKit(){}
    public static final int PAD=6,CHIP_H=14,FIELD_H=20,RADIUS=6;
    public static final float LABEL=8f,SMALL=7f,TITLE=11f,HEADING=15f;

    /** Width a chip with this label needs. */
    public static int chipWidth(String label){return (int)Math.ceil(VelaDraw.textWidth(label,VelaDraw.Weight.MEDIUM,LABEL))+PAD*2;}
    /** Rounded label: neutral, accent when active. Equal padding on every side; underline grows in on hover. */
    public static Rect chip(GuiGraphicsExtractor g,int x,int y,String label,float active,float hover,boolean underline){
        int w=chipWidth(label);var r=new Rect(x,y,w,CHIP_H);
        int base=VelaTheme.mix(0x14FFFFFF,0x24FFFFFF,hover);
        VelaDraw.roundedRect(g,x,y,w,CHIP_H,5,VelaTheme.mix(base,(VelaTheme.PRIMARY&0xFFFFFF)|0x48000000,active));
        int text=VelaTheme.mix(0xFFDDE3EF,VelaTheme.TEXT,Math.max(active,hover));
        VelaDraw.text(g,label,x+PAD,y+3,text,VelaDraw.Weight.MEDIUM,LABEL);
        if(underline&&hover>.02f){int tw=w-PAD*2;g.fill(x+PAD,y+11,x+PAD+Math.round(tw*hover),y+12,text);}
        return r;
    }
    /** Small count pill, e.g. unread messages. Returns its width. */
    public static int badge(GuiGraphicsExtractor g,int x,int y,String value,int color){
        int w=(int)Math.ceil(VelaDraw.textWidth(value,VelaDraw.Weight.SEMIBOLD,SMALL))+8;
        VelaDraw.roundedRect(g,x,y,w,9,4,color);
        VelaDraw.text(g,value,x+4,y+1,VelaTheme.contrastText(color),VelaDraw.Weight.SEMIBOLD,SMALL);
        return w;
    }
    /** Panel surface with a hairline border that lights up on hover or selection. */
    public static void card(GuiGraphicsExtractor g,Rect r,float hover,float selected){
        VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),RADIUS,VelaTheme.mix(VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.SURFACE_HOVER,hover),VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.PRIMARY,.16f),selected));
        new GameUiCanvas(g).border(r.x(),r.y(),r.width(),r.height(),RADIUS,VelaTheme.mix(VelaTheme.mix(VelaTheme.BORDER,VelaTheme.BORDER_HOVER,hover),VelaTheme.PRIMARY,selected));
    }
    /** Search/text field with a magnifier and placeholder; returns the editable text area. */
    public static Rect searchField(GuiGraphicsExtractor g,Rect r,TextEdit edit,boolean focused,String placeholder){
        VelaDraw.roundedOutline(g,r.x(),r.y(),r.width(),r.height(),RADIUS,focused?VelaTheme.PRIMARY:VelaTheme.BORDER,VelaTheme.SURFACE_DEEP);
        VelaIcons.draw(g,"search",r.x()+7,r.y()+(r.height()-9)/2,9,focused?VelaTheme.PRIMARY:VelaTheme.TEXT_MUTED);
        var text=new Rect(r.x()+21,r.y()+(r.height()-14)/2,r.width()-28,14);
        TextFieldDraw.draw(g,edit,text,focused,placeholder);
        return text;
    }
    /** Section caption: small, muted, upper-case. */
    public static void caption(GuiGraphicsExtractor g,String text,float x,float y){
        VelaDraw.text(g,text.toUpperCase(java.util.Locale.ROOT),x,y,VelaTheme.TEXT_MUTED,VelaDraw.Weight.SEMIBOLD,SMALL);
    }
    public static void divider(GuiGraphicsExtractor g,int x,int y,int width){g.fill(x,y,x+width,y+1,VelaTheme.BORDER);}
    /** Centred icon, title and hint for empty lists. */
    public static void empty(GuiGraphicsExtractor g,Rect r,String icon,String title,String hint){
        float cx=r.x()+r.width()/2f,cy=r.y()+r.height()/2f;
        VelaIcons.draw(g,icon,Math.round(cx-9),Math.round(cy-30),18,VelaTheme.TEXT_MUTED);
        VelaDraw.centeredText(g,title,cx,cy-6,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.MEDIUM,9f);
        if(hint!=null&&!hint.isEmpty())VelaDraw.centeredText(g,hint,cx,cy+8,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,LABEL);
    }
    /** Window with title and optional subtitle; returns the content area below the header. */
    public static Rect window(GuiGraphicsExtractor g,Rect r,String title,String subtitle){
        VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),10,VelaTheme.WINDOW);
        new GameUiCanvas(g).border(r.x(),r.y(),r.width(),r.height(),10,VelaTheme.BORDER);
        VelaDraw.text(g,title,r.x()+14,r.y()+12,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,HEADING*.8f);
        if(subtitle!=null&&!subtitle.isEmpty())VelaDraw.text(g,subtitle,r.x()+14,r.y()+28,VelaTheme.TEXT_MUTED,VelaDraw.Weight.REGULAR,LABEL);
        return new Rect(r.x()+12,r.y()+44,r.width()-24,r.height()-56);
    }
    /** Relative age in Russian: "только что", "5 мин назад", "3 ч назад", "2 дн назад". */
    public static String ago(long millis){
        long s=Math.max(0,millis/1000);
        if(s<60)return "только что";
        if(s<3600)return (s/60)+dev.sqwat.vela.ui.Lang.tr(" мин назад");
        if(s<86400)return (s/3600)+dev.sqwat.vela.ui.Lang.tr(" ч назад");
        return (s/86400)+dev.sqwat.vela.ui.Lang.tr(" дн назад");
    }
}
