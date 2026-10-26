import dev.sqwat.vela.ui.render.*;
import java.nio.file.*;
public class RenderSharedNotices {
    public static void main(String[] args)throws Exception{
        Path assets=Path.of(args[0]),out=Path.of(args[1]);Files.createDirectories(out);
        var c=new RenderPngPreview(assets,780,390,1.5f);
        NotificationChrome.draw(c,16,16,160,"Создана метка","Метка 1","markers",0xFFFF8F33,true);Rect action=NotificationChrome.action(16,16,160,true);
        c.round(action.x(),action.y(),action.width(),action.height(),6,VelaTheme.SURFACE_DEEP);c.centered("Удалить",action.x()+action.width()/2f,action.y()+6,8,VelaTheme.TEXT,UiCanvas.Weight.MEDIUM);
        NotificationChrome.draw(c,16,112,160,"Друг зашёл","SqwaT09","friends",VelaTheme.PRIMARY,false);
        NotificationChrome.draw(c,16,178,160,"Аккаунт добавлен","","account",VelaTheme.PRIMARY,false);
        int configWidth=NotificationChrome.compactWidth(c.textWidth("Конфиг создан",8,UiCanvas.Weight.MEDIUM),c.textWidth("Новый профиль",7,UiCanvas.Weight.REGULAR));
        NotificationChrome.draw(c,198,16,configWidth,"Конфиг создан","Новый профиль","configs",VelaTheme.PRIMARY,false);
        c.text("Near",200,92,8,VelaTheme.TEXT,UiCanvas.Weight.MEDIUM);c.image("near_arrow",233,88,10,VelaTheme.PRIMARY);
        c.text("Скорость II",200,119,8,VelaTheme.TEXT,UiCanvas.Weight.REGULAR);c.text("Огнестойкость",200,140,8,VelaTheme.TEXT,UiCanvas.Weight.REGULAR);
        for(int i=0;i<2;i++)CrosshairChrome.draw(c,215+i*55,200,4,i%2==0?1:2,2,i%2==1,VelaTheme.PRIMARY);
        c.save(out.resolve("shared-notices.png"));
    }
}
