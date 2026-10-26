package dev.sqwat.vela.ui.render;

import java.util.ArrayList;
import java.util.List;
import static dev.sqwat.vela.ui.render.UiCanvas.Weight.*;

/** Account geometry and pixels shared by Minecraft and the offline Java2D preview. */
public final class AccountChrome {
    public enum RowAction { NONE, SELECT, LOGIN, DELETE }
    public record Row(Rect card, Rect avatar, Rect name, Rect login, Rect delete) {}
    public record Dialog(Rect panel, Rect content, Rect input, Rect message, Rect primary, Rect cancel) {}

    public record Layout(int x, int y, int width, int height, int side) {
        public boolean compact() { return height < 340 || width < 420; }
        public int padding() { return side>0 ? 10 : compact() ? 12 : 20; }
        public Rect panel() { return new Rect(x, y, width, height); }
        public int split() { return x + width - side; }
        public int listWidth() { return width-side; }
        public Rect details() { return new Rect(split()+30,y+43,Math.max(0,side-60),height-64); }
        public Rect profileAvatar() { Rect r=details();return new Rect(r.x()+14,r.y()+14,56,56); }
        public Rect profileLogin() { Rect r=details();return new Rect(r.x()+r.width()-116,r.y()+29,102,26); }
        public Rect back() { return new Rect(x + width - padding() - 24, y + 12, 24, 24); }
        public Rect search() { return new Rect(x + padding(), y + (side>0?90:compact() ? 48 : 64), listWidth() - padding() * 2, side>0?26:compact() ? 24 : 30); }
        public Rect searchText() { Rect r = search(); return new Rect(r.x() + 25, r.y() + (r.height() - 14) / 2, r.width() - 53, 14); }
        public Rect clearSearch() { Rect r = search(); return new Rect(r.x() + r.width() - 23, r.y() + (r.height() - 20) / 2, 20, 20); }
        public Rect list() { int top = y + (side>0?124:compact() ? 84 : 118); return new Rect(x + padding(), top, listWidth() - padding() * 2 - 9, Math.max(1, (side>0?microsoft().y()-14:status().y()-6) - top)); }
        public Rect status() { if(side>0)return new Rect(split()+30,y+height-30,side-60,18);Rect r = microsoft(); return new Rect(x + padding(), r.y() - (compact() ? 28 : 34), listWidth() - padding() * 2, compact() ? 22 : 28); }
        public Rect microsoft() { int h=side>0?26:compact()?28:34;return new Rect(x + padding(), y + height - padding() - h, (listWidth() - padding() * 2 - 8) / 2, h); }
        public Rect offline() { Rect r = microsoft(); return new Rect(r.x() + r.width() + 8, r.y(), listWidth() - padding() * 2 - r.width() - 8, r.height()); }
        public Rect avatar() { return row(0, 0).avatar(); }
        public int rowHeight() { return side>0?39:compact() ? 44 : 56; }
        public int stride() { return rowHeight() + (side>0?3:6); }
        public Row row(int index, int scroll) {
            Rect list = list();
            Rect card = new Rect(list.x(), list.y() + index * stride() - scroll, list.width(), rowHeight());
            int size = side>0?24:compact() ? 24 : 30;
            Rect avatar = new Rect(card.x() + 10, card.y() + (card.height() - size) / 2, size, size);
            Rect delete = new Rect(card.x() + card.width() - 32, card.y() + (card.height() - 24) / 2, 24, 24);
            Rect login = side>0?new Rect(delete.x()-12,card.y()+(card.height()-8)/2,8,8):new Rect(delete.x() - (compact() ? 58 : 70), card.y() + (card.height() - 24) / 2, compact() ? 52 : 64, 24);
            int nameX = avatar.x() + avatar.width() + 10;
            Rect name = new Rect(nameX, card.y() + (side>0?7:compact() ? 8 : 13), Math.max(1, login.x() - nameX - 8), 12);
            return new Row(card, avatar, name, login, delete);
        }
        public int maxScroll(int count) { return Math.max(0, Math.max(0, count) * stride() - (stride()-rowHeight()) - list().height()); }
        public int rowAt(double mx, double my, int count, int scroll) {
            if (!list().contains(mx, my)) return -1;
            int offset = (int)Math.floor(my - list().y()) + Math.clamp(scroll, 0, maxScroll(count));
            int index = offset / stride();
            return index >= 0 && index < count && offset % stride() < rowHeight() ? index : -1;
        }
        public int reveal(int index, int scroll, int count) {
            if (count <= 0 || index < 0) return 0;
            int top = Math.min(index, count - 1) * stride();
            return Math.clamp(Math.max(top + rowHeight() - list().height(), Math.min(scroll, top)), 0, maxScroll(count));
        }
        public Rect scrollTrack() { Rect r = list(); return new Rect(r.x() + r.width() + 2, r.y(), 7, r.height()); }
        public Rect scrollThumb(int count, int scroll) {
            Rect track = scrollTrack(); int max = maxScroll(count);
            int h = Math.min(track.height(), Math.max(18, track.height() * track.height() / Math.max(1, track.height() + max)));
            int top = max == 0 ? 0 : Math.round((track.height() - h) * (float)Math.clamp(scroll, 0, max) / max);
            return new Rect(track.x(), track.y() + top, track.width(), h);
        }
        public int scrollFromPointer(double y, double grab, int count) {
            Rect track = scrollTrack(), thumb = scrollThumb(count, 0);
            return (int)Math.round(Math.clamp((y - track.y() - grab) / Math.max(1, track.height() - thumb.height()), 0, 1) * maxScroll(count));
        }
    }

    private AccountChrome() {}
    public static void drawLoginProgress(UiCanvas c,Layout l,boolean complete,float successSeconds,long nanos){
        int w=Math.min(230,l.width()-24),h=70,x=l.x()+(l.width()-w)/2,y=l.y()+(l.height()-h)/2;
        c.round(l.x(),l.y(),l.width(),l.height(),12,0x9905070C);
        c.round(x,y,w,h,9,VelaTheme.WINDOW);c.border(x,y,w,h,9,VelaTheme.BORDER);
        float done=complete?Math.clamp(successSeconds/.22f,0,1):0;
        ProgressSpinner.draw(c,x+26,y+28,8,nanos,1-done);
        if(done>0)c.image("check",x+17,y+19,18,(Math.round(255*done)<<24)|(VelaTheme.POSITIVE&0xFFFFFF));
        c.text(complete?"Вход выполнен":"Выполняется вход",x+47,y+22,10,VelaTheme.TEXT,MEDIUM);
        c.text(complete?"Аккаунт готов к игре":"Esc — отменить",x+47,y+41,8,VelaTheme.TEXT_SECONDARY,REGULAR);
    }
    public static Layout layout(int width, int height) {
        int w = Math.max(1, Math.min(694, width - 24)), h = Math.max(1, Math.min(440, height - 24));
        return new Layout((width - w) / 2, (height - h) / 2, w, h, w>=650&&h>=420?w-227:0);
    }
    public static Layout layout(int width, int height, int accounts) { return layout(width, height); }
    public static Dialog dialog(Layout l, int preferredHeight) {
        int w = Math.min(408, l.width() - 16), h = Math.min(preferredHeight, l.height() - 16);
        int x = l.x() + (l.width() - w) / 2, y = l.y() + (l.height() - h) / 2, p = 16;
        Rect cancel = new Rect(x + p, y + h - p - 28, (w - p * 2 - 8) / 2, 28);
        Rect primary = new Rect(cancel.x() + cancel.width() + 8, cancel.y(), w - p * 2 - cancel.width() - 8, 28);
        return new Dialog(new Rect(x, y, w, h), new Rect(x + p, y + 16, w - p * 2, h - 76),
            new Rect(x + p, y + 62, w - p * 2, 28), new Rect(x + p, y + 98, w - p * 2, Math.max(16, h - 150)), primary, cancel);
    }
    public static Dialog authDialog(Layout l){
        if(l.height()<360)return dialog(l,252);
        int w=294,h=340,x=l.x()+(l.width()-w)/2,y=l.y()+(l.height()-h)/2;
        return new Dialog(new Rect(x,y,w,h),new Rect(x+18,y+16,w-36,248),
            new Rect(x+18,y+188,w-36,32),new Rect(x+18,y+232,w-36,33),
            new Rect(x+18,y+278,w-36,27),new Rect(x+18,y+311,w-36,20));
    }
    public static Rect authTab(Dialog d,boolean microsoft){return new Rect(d.panel().x()+15+(microsoft?0:(d.panel().width()-30)/2),d.panel().y()+52,(d.panel().width()-30)/2,29);}
    public static Rect authClose(Dialog d){return new Rect(d.panel().x()+d.panel().width()-34,d.panel().y()+16,20,20);}
    public static void drawAuth(UiCanvas c,Layout l,Dialog d,boolean microsoft){
        drawAuth(c,l,d,microsoft,0,0,0);
    }
    public static void drawAuth(UiCanvas c,Layout l,Dialog d,boolean microsoft,float msHover,float offlineHover,float closeHover){
        if(d.panel().height()<340){drawDialog(c,l,d,microsoft?"Вход через Microsoft":"Добавить Offline",microsoft?"Подтвердите вход на странице Microsoft":"Локальный профиль");return;}
        c.round(l.x(),l.y(),l.width(),l.height(),12,0xC5030508);
        Rect p=d.panel();c.texture("account_modal",p,882,1020,0xFFFFFFFF);
        c.border(p.x(),p.y(),p.width(),p.height(),12,VelaTheme.BORDER_HOVER);
        c.text("Добавить аккаунт",p.x()+15,p.y()+23,12,VelaTheme.TEXT,SEMIBOLD);
        Rect close=authClose(d);c.round(close.x(),close.y(),close.width(),close.height(),5,VelaTheme.mix(0x00121721,VelaTheme.SURFACE_HOVER,closeHover));c.image("x",close.x()+4,close.y()+4,12,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.TEXT,closeHover));
        Rect left=authTab(d,true),right=authTab(d,false);
        c.round(left.x(),left.y(),left.width()*2,left.height(),7,0xFF090D13);
        Rect selected=microsoft?left:right;c.round(selected.x()+3,selected.y()+3,selected.width()-6,selected.height()-6,5,0xFF171D29);
        Rect hovered=microsoft?right:left;float hover=microsoft?offlineHover:msHover;
        c.round(hovered.x()+3,hovered.y()+3,hovered.width()-6,hovered.height()-6,5,VelaTheme.mix(0x00090D13,0xFF141A25,hover));
        float msLeft=left.x()+(left.width()-17-c.textWidth("Microsoft",8,MEDIUM))/2;
        microsoft(c,msLeft,left.y()+9,11);c.text("Microsoft",msLeft+17,left.y()+10,8,microsoft?VelaTheme.TEXT:VelaTheme.TEXT_SECONDARY,MEDIUM);
        float offlineLeft=right.x()+(right.width()-17-c.textWidth("Offline",8,MEDIUM))/2;
        c.image("account",offlineLeft,right.y()+9,11,VelaTheme.TEXT_SECONDARY);c.text("Offline",offlineLeft+17,right.y()+10,8,microsoft?VelaTheme.TEXT_SECONDARY:VelaTheme.TEXT,MEDIUM);
        c.round(p.x()+p.width()/2f-19,p.y()+99,38,38,10,0xFF24233C);
        if(microsoft)microsoft(c,p.x()+p.width()/2f-9,p.y()+109,18);
        else c.image("account",p.x()+p.width()/2f-10,p.y()+108,20,0xFF918BFF);
        c.centered(microsoft?"Вход через Microsoft":"Локальный аккаунт",p.x()+p.width()/2f,p.y()+150,11,VelaTheme.TEXT,SEMIBOLD);
        c.centered(microsoft?"Подтвердите вход в браузере":"Введите имя игрока",p.x()+p.width()/2f,p.y()+168,7,VelaTheme.TEXT_SECONDARY,REGULAR);
    }
    private static void microsoft(UiCanvas c,float x,float y,float size){float cell=(size-1)/2;int[] colors={0xFFF35325,0xFF81BC06,0xFF05A6F0,0xFFFFBA08};for(int i=0;i<4;i++)c.fill(x+(i%2)*(cell+1),y+(i/2)*(cell+1),cell,cell,colors[i]);}
    public static RowAction action(Layout l, double mx, double my, int count, int scroll, boolean active) {
        int index = l.rowAt(mx, my, count, scroll);
        if (index < 0) return RowAction.NONE;
        Row r = l.row(index, scroll);
        if (!active && r.delete().contains(mx, my)) return RowAction.DELETE;
        if (l.side()==0 && !active && r.login().contains(mx, my)) return RowAction.LOGIN;
        return RowAction.SELECT;
    }
    public static int selection(boolean selected, float hover) { return selection(selected ? 1f : 0f, hover); }
    public static int selection(float selected, float hover) {
        int base = VelaTheme.mix(VelaTheme.SURFACE, VelaTheme.PRIMARY, .17f * selected);
        return VelaTheme.mix(base, VelaTheme.SURFACE_HOVER, hover * .7f) | 0xFF000000;
    }
    public static void draw(UiCanvas c, Layout l, String name, int count, String query, boolean focused, int mx, int my, int accent) {
        draw(c, l, name, count, query, focused, mx, my, accent, l.microsoft().contains(mx, my) ? 1 : 0, l.offline().contains(mx, my) ? 1 : 0);
    }
    public static void draw(UiCanvas c, Layout l, String name, int count, String query, boolean focused, int mx, int my, int accent, float microsoftHover, float offlineHover) {
        if(l.side()>0){drawManager(c,l,query,focused,accent,microsoftHover,offlineHover);return;}
        c.round(l.x(), l.y() + 4, l.width(), l.height(), 12, 0x40000000);
        c.round(l.x(), l.y(), l.width(), l.height(), 12, VelaTheme.WINDOW);
        c.border(l.x(), l.y(), l.width(), l.height(), 12, VelaTheme.BORDER);
        int left = l.x() + l.padding();
        c.text("Vela Manager", left, l.y() + 14, l.compact() ? 13 : 16, VelaTheme.TEXT, SEMIBOLD);
        float titleWidth = c.textWidth("Vela Manager", l.compact() ? 13 : 16, SEMIBOLD);
        String total = Integer.toString(count); float badgeWidth = c.textWidth(total, 8, MEDIUM) + 14;
        c.round(left + titleWidth + 10, l.y() + 14, badgeWidth, 17, 5, VelaTheme.SURFACE_HOVER);
        c.centered(total, left + titleWidth + 10 + badgeWidth / 2, l.y() + 18, 8, VelaTheme.TEXT_SECONDARY, MEDIUM);
        c.text(fit(c, dev.sqwat.vela.ui.Lang.tr("Сейчас в игре: ") + name, l.listWidth() - l.padding() * 2 - 30, 8, REGULAR), left, l.y() + (l.compact() ? 33 : 40), 8, VelaTheme.TEXT_SECONDARY, REGULAR);
        if(l.side()>0)c.fill(l.split(),l.y()+16,1/c.density(),l.height()-32,VelaTheme.BORDER);
        Rect search = l.search();
        c.round(search.x(), search.y(), search.width(), search.height(), 7, VelaTheme.SURFACE_DEEP);
        c.image("search", search.x() + 8, search.y() + (search.height() - 11) / 2f, 11, focused ? accent : VelaTheme.TEXT_MUTED);
        if (!query.isEmpty()) c.image("x", l.clearSearch().x() + 5, l.clearSearch().y() + 5, 10, VelaTheme.TEXT_SECONDARY);
        if (!l.compact()) c.text("СОХРАНЁННЫЕ ПРОФИЛИ", left, l.y() + 104, 7, VelaTheme.TEXT_MUTED, MEDIUM);
        c.line(left, l.status().y() - 3, l.listWidth() - l.padding() * 2, VelaTheme.BORDER);
        footer(c, l.microsoft(), "Microsoft", "microsoft", microsoftHover, accent);
        footer(c, l.offline(), "Offline", "account", offlineHover, accent);
    }
    public static void drawRow(UiCanvas c, Row r, String name, boolean microsoft, boolean active, float selected, float hover, float loginHover, float deleteHover) {
        Rect card = r.card();
        if(r.login().width()==8){
            c.round(card.x(),card.y(),card.width(),card.height(),7,VelaTheme.mix(0xFF0A0E14,VelaTheme.PRIMARY,.1f*selected+.055f*hover));
            c.text(fit(c,name,r.name().width(),9,MEDIUM),r.name().x(),r.name().y(),9,VelaTheme.TEXT,MEDIUM);
            c.text(microsoft?"Microsoft":"Offline",r.name().x(),r.name().y()+14,8,VelaTheme.TEXT_SECONDARY,REGULAR);
            if(active)c.round(r.delete().x()+10,r.login().y()+2,4,4,2,VelaTheme.POSITIVE);
            if(!active){
                c.round(r.delete().x()+3,r.delete().y()+3,18,18,5,VelaTheme.mix(0x000A0E14,0xFF202737,deleteHover));
                for(int i=0;i<3;i++)c.round(r.delete().x()+7+i*4,r.delete().y()+11,2,2,1,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.TEXT,deleteHover));
            }
            return;
        }
        c.round(card.x(), card.y(), card.width(), card.height(), 8, selection(active ? 1 : 0, hover));
        c.border(card.x(), card.y(), card.width(), card.height(), 8, VelaTheme.BORDER);
        c.round(r.avatar().x(), r.avatar().y(), r.avatar().width(), r.avatar().height(), 6, VelaTheme.SURFACE_DEEP);
        c.image("account", r.avatar().x() + 5, r.avatar().y() + 5, r.avatar().width() - 10, VelaTheme.TEXT_MUTED);
        c.text(fit(c, name, r.name().width(), 9, MEDIUM), r.name().x(), r.name().y(), 9, VelaTheme.TEXT, MEDIUM);
        String provider = microsoft ? "Microsoft" : "Offline";
        c.text(provider, r.name().x(), r.name().y() + 16, 7, VelaTheme.TEXT_SECONDARY, REGULAR);
        if (active) {
            c.round(r.login().x(), r.login().y(), r.login().width(), r.login().height(), 6, VelaTheme.mix(VelaTheme.SURFACE, VelaTheme.POSITIVE, .1f));
            c.centered("В игре", r.login().x() + r.login().width() / 2f, r.login().y() + 8, 8, VelaTheme.POSITIVE, MEDIUM);
        } else {
            drawButton(c, r.login(), "Войти", true, loginHover);
            c.round(r.delete().x(), r.delete().y(), r.delete().width(), r.delete().height(), 6, VelaTheme.mix(VelaTheme.SURFACE, VelaTheme.SURFACE_HOVER, deleteHover));
            c.image("trash", r.delete().x() + 6, r.delete().y() + 6, 12, VelaTheme.mix(VelaTheme.TEXT_MUTED, VelaTheme.DANGER, deleteHover));
        }
    }
    public static void drawProfile(UiCanvas c,Layout l,String name,String uuid,boolean microsoft,boolean active,float hover) {
        if(l.side()==0)return;
        Rect r=l.details(),avatar=l.profileAvatar();
        c.texture("account_hero",new Rect(r.x(),r.y(),r.width(),84),1224,252,0xFFFFFFFF);
        c.border(r.x(),r.y(),r.width(),84,10,VelaTheme.BORDER);
        int tx=avatar.x()+avatar.width()+14,available=l.profileLogin().x()-tx-8;
        c.round(tx,r.y()+14,65,15,4,VelaTheme.SURFACE_DEEP);
        if(microsoft)microsoft(c,tx+6,r.y()+17,9);else c.image("account",tx+6,r.y()+17,9,VelaTheme.PRIMARY);
        c.text(microsoft?"Microsoft":"Offline",tx+19,r.y()+18,8,VelaTheme.TEXT_SECONDARY,MEDIUM);
        c.text(fit(c,name,available,18,SEMIBOLD),tx,r.y()+35,18,VelaTheme.TEXT,SEMIBOLD);
        c.text(fit(c,microsoft?"Minecraft: Java Edition":"Локальный профиль",available,7,REGULAR),tx,r.y()+61,7,VelaTheme.TEXT_SECONDARY,REGULAR);
        Rect button=l.profileLogin();
        if(active){c.round(button.x(),button.y(),button.width(),button.height(),6,VelaTheme.mix(VelaTheme.SURFACE,VelaTheme.POSITIVE,.1f));c.centered("В игре",button.x()+button.width()/2f,button.y()+9,8,VelaTheme.POSITIVE,MEDIUM);}
        else drawButton(c,button,"Использовать аккаунт",true,hover);
        int top=r.y()+104;
        c.text("Информация профиля",r.x(),top,10,VelaTheme.TEXT,SEMIBOLD);
        c.round(r.x(),top+24,r.width(),112,8,0xFF0E121A);c.border(r.x(),top+24,r.width(),112,8,VelaTheme.BORDER);
        String[] labels={"Имя игрока","Тип аккаунта","Идентификатор","Хранение сессии"};
        String[] values={name,microsoft?"Microsoft":"Offline",uuid,microsoft?"Локальное хранилище IAS":"Не требуется"};
        for(int i=0;i<4;i++){
            int yy=top+24+i*28;float size=i==2?6.5f:7.5f;
            if(i>0)c.line(r.x(),yy,r.width(),VelaTheme.BORDER);
            c.image(new String[]{"account","key","configs","save"}[i],r.x()+9,yy+9,11,VelaTheme.PRIMARY);
            c.text(labels[i],r.x()+25,yy+10,8,VelaTheme.TEXT_SECONDARY,REGULAR);
            String value=fit(c,values[i],r.width()-130,size,REGULAR);
            float valueX=r.x()+r.width()-10-c.textWidth(value,size,REGULAR);
            if(i==1){if(microsoft)microsoft(c,valueX-15,yy+9,10);else c.image("account",valueX-15,yy+9,10,VelaTheme.PRIMARY);}
            c.text(value,valueX,yy+10,size,VelaTheme.TEXT_SECONDARY,REGULAR);
        }
    }
    private static void drawManager(UiCanvas c,Layout l,String query,boolean focused,int accent,float microsoftHover,float offlineHover){
        c.round(l.x(),l.y(),l.width(),l.height(),12,0xFF0D1118);
        c.round(l.x(),l.y(),l.listWidth(),l.height(),12,0xFF0A0E14);
        c.fill(l.split()-12,l.y()+1,12,l.height()-2,0xFF0A0E14);
        c.border(l.x(),l.y(),l.width(),l.height(),12,VelaTheme.BORDER);
        c.fill(l.split(),l.y(),1/c.density(),l.height(),VelaTheme.BORDER);
        float brandX=l.x()+(l.listWidth()-27-c.textWidth("Vela Manager",11,SEMIBOLD))/2;
        c.image("logo",brandX,l.y()+12,20,0xFFFFFFFF);c.text("Vela Manager",brandX+27,l.y()+16,11,VelaTheme.TEXT,SEMIBOLD);
        c.line(l.x(),l.y()+44,l.listWidth(),VelaTheme.BORDER);
        c.text("Аккаунты",l.x()+15,l.y()+61,14,VelaTheme.TEXT,SEMIBOLD);
        Rect search=l.search();c.round(search.x(),search.y(),search.width(),search.height(),6,0xFF0B0F16);
        c.image("search",search.x()+7,search.y()+8,10,VelaTheme.TEXT_SECONDARY);
        if(!query.isEmpty())c.image("x",l.clearSearch().x()+5,l.clearSearch().y()+5,10,VelaTheme.TEXT_SECONDARY);
        c.line(l.x(),l.microsoft().y()-9,l.listWidth(),VelaTheme.BORDER);
        addFooter(c,l.microsoft(),"Microsoft","microsoft",microsoftHover,accent);
        addFooter(c,l.offline(),"Offline","account",offlineHover,accent);
    }
    private static void addFooter(UiCanvas c,Rect r,String label,String icon,float hover,int accent){
        c.round(r.x(),r.y(),r.width(),r.height(),6,VelaTheme.mix(0xFF121721,VelaTheme.readableAccentFill(),hover));
        c.border(r.x(),r.y(),r.width(),r.height(),6,VelaTheme.mix(VelaTheme.BORDER,accent,hover*.45f));
        float left=r.x()+(r.width()-16-c.textWidth(label,8,MEDIUM))/2;
        if(icon.equals("microsoft"))microsoft(c,left,r.y()+8,10);else c.image(icon,left,r.y()+7,11,VelaTheme.TEXT_SECONDARY);
        c.text(label,left+16,r.y()+9,8,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.TEXT,hover),MEDIUM);
    }
    public static void drawStatus(UiCanvas c, Rect r, String text, boolean error) {
        c.text(fit(c, text, r.width(), 8, REGULAR), r.x(), r.y() + (r.height() - 8) / 2f, 8, error ? VelaTheme.DANGER : VelaTheme.TEXT_SECONDARY, REGULAR);
    }
    public static void drawEmpty(UiCanvas c, Layout l, boolean filtered) {
        Rect r = l.list(); int cy = r.y() + r.height() / 2;
        if (r.height() > 100) c.image(filtered ? "search" : "account", r.x() + r.width() / 2f - 12, cy - 45, 24, VelaTheme.TEXT_MUTED);
        c.centered(filtered ? "Ничего не найдено" : "Пока нет аккаунтов", r.x() + r.width() / 2f, cy - 9, 10, VelaTheme.TEXT, MEDIUM);
        String hint = filtered ? "Измените запрос или очистите поиск" : "Добавьте Microsoft или Offline ниже";
        c.centered(fit(c, hint, r.width() - 12, 8, REGULAR), r.x() + r.width() / 2f, cy + 9, 8, VelaTheme.TEXT_SECONDARY, REGULAR);
    }
    public static void drawDialog(UiCanvas c, Layout l, Dialog d, String title, String subtitle) {
        c.round(l.x(), l.y(), l.width(), l.height(), 12, 0xEC090C12);
        Rect p = d.panel();
        c.round(p.x(), p.y(), p.width(), p.height(), 10, VelaTheme.WINDOW);
        c.border(p.x(), p.y(), p.width(), p.height(), 10, VelaTheme.BORDER_HOVER);
        c.text(fit(c, title, d.content().width(), 12, SEMIBOLD), d.content().x(), d.content().y(), 12, VelaTheme.TEXT, SEMIBOLD);
        wrapped(c, subtitle, new Rect(d.content().x(), d.content().y() + 23, d.content().width(), 24), 8, VelaTheme.TEXT_SECONDARY);
    }
    public static void drawButton(UiCanvas c, Rect r, String label, boolean primary, float hover) {
        int base = primary ? VelaTheme.readableAccentFill() : VelaTheme.SURFACE;
        c.round(r.x(), r.y(), r.width(), r.height(), 6, VelaTheme.mix(base, VelaTheme.readableAccentFill(), hover));
        c.border(r.x(), r.y(), r.width(), r.height(), 6, primary ? VelaTheme.PRIMARY : VelaTheme.BORDER);
        float shift=label.equals("Добавить")?ActionGeometry.textShift(hover):0;
        c.centered(fit(c, label, r.width() - 8, 8, MEDIUM), r.x() + r.width() / 2f+shift, r.y() + (r.height() - 8) / 2f, 8, VelaTheme.TEXT, MEDIUM);
        if(shift>0)c.text(">",r.x()+(r.width()-c.textWidth(label,8,MEDIUM))/2+shift-10,r.y()+(r.height()-8)/2f,8,(Math.round(255*hover)<<24)|(VelaTheme.TEXT&0xFFFFFF),MEDIUM);
    }
    public static void focus(UiCanvas c, Rect r) { }
    public static String fit(UiCanvas c, String value, float width, float size, UiCanvas.Weight weight) {
        if (c.textWidth(value, size, weight) <= width) return value;
        int end = value.length();
        while (end > 0 && c.textWidth(value.substring(0, end) + "…", size, weight) > width) end = value.offsetByCodePoints(end, -1);
        return end == 0 && c.textWidth("…", size, weight) > width ? "" : value.substring(0, end) + "…";
    }
    public static void wrapped(UiCanvas c, String value, Rect r, float size, int color) {
        List<String> lines = new ArrayList<>(); String line = "";
        for (String word : value.replace('\n', ' ').split(" +")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && c.textWidth(next, size, REGULAR) > r.width()) { lines.add(line); line = word; }
            else line = next;
        }
        if (!line.isEmpty()) lines.add(line);
        int limit = Math.max(1, r.height() / ((int)size + 4));
        for (int i = 0; i < Math.min(limit, lines.size()); i++) {
            String text = lines.get(i); if (i == limit - 1 && lines.size() > limit) text += "…";
            c.text(fit(c, text, r.width(), size, REGULAR), r.x(), r.y() + i * (size + 4), size, color, REGULAR);
        }
    }
    private static void footer(UiCanvas c, Rect r, String label, String icon, float hover, int accent) {
        c.round(r.x(), r.y(), r.width(), r.height(), 7, selection(false, hover));
        c.border(r.x(), r.y(), r.width(), r.height(), 7, VelaTheme.mix(VelaTheme.BORDER, accent, hover));
        float left = r.x() + (r.width() - c.textWidth(label, 9, MEDIUM) - 20) / 2;
        if(icon.equals("microsoft"))microsoft(c,left,r.y()+(r.height()-13)/2f,13);
        else c.image(icon, left, r.y() + (r.height() - 13) / 2f, 13, accent);
        c.text(label, left + 20, r.y() + (r.height() - 9) / 2f, 9, VelaTheme.TEXT, MEDIUM);
    }
}
