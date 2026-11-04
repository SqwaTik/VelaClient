package dev.sqwat.vela.ui.render;

import static dev.sqwat.vela.ui.render.UiCanvas.Weight.*;

/** Layout translated from the supplied 820 × 500 design, in 1.5 physical-pixel points. */
public final class DashboardChrome {
    public static final String[] SECTIONS=java.util.Arrays.stream(dev.sqwat.vela.ui.screen.VelaSection.values()).map(dev.sqwat.vela.ui.screen.VelaSection::title).toArray(String[]::new);
    private static final int SECONDARY=dev.sqwat.vela.ui.screen.VelaSection.MARKERS.ordinal();
    public record Layout(int x,int y,int width,int height,int sidebar){
        public Rect navigation(int index){float step=Math.min(23.333333f,(height-103)/(float)(SECTIONS.length-1));return new Rect(x+5,y+46+Math.round(index*step)+(index>=SECONDARY?12:0),sidebar-10,Math.min(22,Math.max(14,(int)step-1)));}
        public Rect search(){return new Rect(x+width-146,y+9,107,21);}
        public Rect settings(){return new Rect(x+width-33,y+8,22,22);}
        public Rect content(){return new Rect(x+sidebar+11,y+48,width-sidebar-26,height-60);}
    }
    private DashboardChrome(){}
    public static Layout layout(int width,int height){int w=Math.min(547,width-32),h=Math.min(334,height-32);return new Layout((width-w)/2,(height-h)/2,w,h,109);}
    /** Hover amount of the version label, set by the dashboard before drawing. */
    public static float versionHover;
    public static void draw(UiCanvas c,Layout l,int section,String description,String query,boolean focused,float[] hovers,boolean gearHover,int accent){
        float x=l.x,y=l.y,w=l.width,h=l.height,side=l.sidebar;
        c.round(x+1,y+3,w,h,9,0x30000000);
        c.round(x,y,w,h,9,VelaTheme.WINDOW);
        c.round(x,y,side,h,9,VelaTheme.SIDEBAR);
        c.fill(x+side-9,y,9,h,VelaTheme.SIDEBAR);
        c.fill(x+side,y,1/c.density(),h,0xFF1A2030);
        c.border(x,y,w,h,9,VelaTheme.BORDER);
        c.image("logo",x+11,y+11,16,0xFFFFFFFF);
        c.text("Vela",x+34,y+10,10,VelaTheme.TEXT,SEMIBOLD);
        c.text("Client "+net.minecraft.SharedConstants.getCurrentVersion().name(),x+34,y+23,20f/3,VelaTheme.TEXT_MUTED,MEDIUM);
        c.line(x,y+40,side,0xFF1A2030);
        for(int i=0;i<SECTIONS.length;i++){
            if(i==SECONDARY)c.line(x+10,l.navigation(i).y()-7,side-20,0xFF1A2030);
            Rect r=l.navigation(i);boolean active=i==section;float hover=Math.max(0,Math.min(1,hovers[i]));
            if(active||hover>.001f)c.round(r.x(),r.y(),r.width(),r.height(),14f/3,VelaTheme.mix(VelaTheme.SIDEBAR,active?VelaTheme.mix(0xFF10151D,accent,.28f):0xFF10151D,active?1:hover));
            int iconColor=active?VelaTheme.TEXT:VelaTheme.mix(VelaTheme.TEXT_MUTED,accent,hover);
            float shift=hover*7;
            c.image(SECTIONS[i].toLowerCase(java.util.Locale.ROOT),r.x()+7+shift,r.y()+6,10,iconColor);
            c.text(SECTIONS[i],r.x()+24+shift,r.y()+6,26f/3,active?VelaTheme.TEXT:VelaTheme.mix(VelaTheme.TEXT_MUTED,VelaTheme.TEXT_SECONDARY,hover),active?MEDIUM:REGULAR);
        }
        c.line(x,y+h-23,side,0xFF1A2030);
        // The version opens the changelog (VelaDashboardScreen); it brightens on hover.
        float version=Math.max(0,Math.min(1,versionHover));
        if(version>.01f)c.round(x+side/2f-24,y+h-19,48,14,7,VelaTheme.mix(VelaTheme.SIDEBAR,0xFF1C2333,version));
        c.centered("v"+dev.sqwat.vela.ui.Changelog.current(),x+side/2,y+h-15,20f/3,VelaTheme.mix(VelaTheme.TEXT_MUTED,VelaTheme.TEXT,version),REGULAR);

        c.text(SECTIONS[section],x+side+11,y+14,10,VelaTheme.TEXT,MEDIUM);

        Rect search=l.search();
        c.round(search.x(),search.y(),search.width(),search.height(),16f/3,VelaTheme.SURFACE);
        c.image("search",search.x()+7,search.y()+6,9,VelaTheme.TEXT_MUTED);
        if(query!=null){String label=query.isEmpty()?(focused?"":"Поиск…"):fit(c,query,search.width()-26,8);
        c.text(label,search.x()+20,search.y()+6,8,query.isEmpty()?VelaTheme.TEXT_MUTED:VelaTheme.TEXT,REGULAR);}

        Rect gear=l.settings();
        if(gearHover)c.round(gear.x(),gear.y(),gear.width(),gear.height(),14f/3,VelaTheme.SURFACE);
        c.line(x+side,y+37,w-side,0xFF1A2030);
    }
    public static void empty(UiCanvas c,Rect content,String title,String detail){
        float center=content.x()+content.width()/2f,top=content.y()+content.height()/2f-12;
        c.centered(title,center,top,28f/3,VelaTheme.TEXT_SECONDARY,MEDIUM);
        c.centered(detail,center,top+17,8,VelaTheme.TEXT_MUTED,REGULAR);
    }
    private static String fit(UiCanvas c,String value,float max,float size){
        if(c.textWidth(value,size,REGULAR)<=max)return value;
        int end=value.length();while(end>0&&c.textWidth(value.substring(0,end)+"…",size,REGULAR)>max)end=value.offsetByCodePoints(end,-1);
        return value.substring(0,end)+"…";
    }
}
