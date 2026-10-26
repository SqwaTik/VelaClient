import dev.sqwat.vela.ui.render.*;
import java.nio.file.*;
import java.awt.Shape;
import static dev.sqwat.vela.ui.render.UiCanvas.Weight.*;

/** Synthetic identities only. All geometry, row, empty and dialog chrome is production code. */
public class RenderAccountFixPreview {
    public static void main(String[] args) throws Exception {
        Path assets = Path.of(args[0]), out = Path.of(args[1]);
        Files.createDirectories(out);
        render(assets, out, "accounts-wide", 1000, 680, 1.5f, 18, "", false, false, null);
        render(assets, out, "accounts-compact", 320, 240, 3f, 18, "", false, false, null);
        render(assets, out, "accounts-medium", 610, 424, 2f, 18, "", false, false, null);
        render(assets, out, "accounts-empty", 854, 480, 1.5f, 0, "", false, false, null);
        render(assets, out, "accounts-no-results", 610, 424, 2f, 0, "no-match", true, false, null);
        render(assets, out, "accounts-error", 854, 480, 1.5f, 18, "", false, true, null);
        render(assets, out, "accounts-scrolled", 610, 424, 2f, 120, "", false, false, "scroll");
        for (String dialog : new String[] {"offline", "delete", "disclaimer", "microsoft", "session-error", "session-password", "session-progress", "session-success"}) {
            render(assets, out, "dialog-" + dialog, 854, 480, 1.5f, 3, "", false, false, dialog);
            render(assets, out, "compact-" + dialog, 320, 240, 3f, 3, "", false, false, dialog);
        }
    }
    private static void render(Path assets, Path out, String file, int width, int height, float density, int count, String query, boolean filtered, boolean error, String mode) throws Exception {
        var c = new RenderPngPreview(assets, Math.round(width * density), Math.round(height * density), density);
        c.texture("account_background",new Rect(0,0,width,height),1024,640,0xFFFFFFFF);
        var l = AccountChrome.layout(width, height);
        AccountChrome.draw(c, l, "DemoPlayer", filtered ? 18 : count, query, filtered, -1, -1, VelaTheme.PRIMARY);
        c.image("x", l.back().x() + 6, l.back().y() + 6, 12, VelaTheme.TEXT_SECONDARY);
        c.text(query.isBlank() ? "Поиск по имени" : query, l.searchText().x(), l.searchText().y() + 2, 8, query.isBlank() ? VelaTheme.TEXT_MUTED : VelaTheme.TEXT, REGULAR);
        int scroll = "scroll".equals(mode) ? l.maxScroll(count) : 0;
        Shape clip = c.graphics.getClip();
        var bounds = l.list();
        c.graphics.clipRect(Math.round(bounds.x() * density), Math.round(bounds.y() * density), Math.round(bounds.width() * density), Math.round(bounds.height() * density));
        if (count == 0) AccountChrome.drawEmpty(c, l, filtered);
        for (int i = scroll / l.stride(); i < count; i++) {
            var r = l.row(i, scroll);
            if (r.card().y() >= bounds.y() + bounds.height()) break;
            String name = i == 0 ? "DemoPlayer" : i == 1 ? "VeryLongDemoProfileNameForClipping" : "DemoProfile_" + (i + 1);
            AccountChrome.drawRow(c, r, name, i % 3 != 0, i == 0, i == (scroll == 0 ? 1 : count - 1) ? 1 : 0, 0, 0, 0);
        }
        c.graphics.setClip(clip);
        if(count>0)AccountChrome.drawProfile(c,l,"DemoPlayer","00000000-0000-0000-0000-000000000001",false,true,0);
        if (l.maxScroll(count) > 0) {
            Rect track = l.scrollTrack(), thumb = l.scrollThumb(count, scroll);
            c.round(track.x() + 2, track.y(), 3, track.height(), 2, VelaTheme.SURFACE_DEEP);
            c.round(thumb.x() + 2, thumb.y(), 3, thumb.height(), 2, VelaTheme.BORDER_HOVER);
        }
        AccountChrome.drawStatus(c, l.status(), error ? "Не удалось войти. Повторите попытку." : "↑ ↓ Выбор · Enter Вход · Del Удалить", error);
        if (mode != null && !mode.equals("scroll")) dialog(c, l, mode);
        c.save(out.resolve(file + ".png"));
    }
    private static void dialog(UiCanvas c, AccountChrome.Layout l, String kind) {
        if(kind.equals("session-progress")||kind.equals("session-success")){AccountChrome.drawLoginProgress(c,l,kind.equals("session-success"),.5f,600_000_000L);return;}
        var d = kind.equals("offline")||kind.equals("microsoft")?AccountChrome.authDialog(l):AccountChrome.dialog(l, switch (kind) { case "delete" -> 196; case "disclaimer" -> 236; default -> 230; });
        String title = switch (kind) { case "offline" -> "Добавить Offline"; case "delete" -> "Удалить аккаунт?"; case "disclaimer" -> "Безопасность аккаунтов"; case "microsoft" -> "Вход через Microsoft"; case "session-error" -> "Ошибка входа"; default -> "Вход в аккаунт"; };
        String subtitle = switch (kind) { case "offline" -> "Локальный профиль без Microsoft-входа"; case "delete" -> "Будет удалён только сохранённый профиль."; case "disclaimer" -> "Данные входа хранятся на этом ПК."; case "microsoft" -> "Подтвердите вход на странице Microsoft"; case "session-error" -> "Сохранённый профиль остаётся в списке"; default -> "Esc — отменить и вернуться к аккаунтам"; };
        if(kind.equals("offline")||kind.equals("microsoft"))AccountChrome.drawAuth(c,l,d,kind.equals("microsoft"));else AccountChrome.drawDialog(c, l, d, title, subtitle);
        String primary = "Продолжить";
        switch (kind) {
            case "offline", "session-password" -> {
                Rect r = d.input();
                c.round(r.x(), r.y(), r.width(), r.height(), 6, VelaTheme.SURFACE_DEEP);
                c.border(r.x(), r.y(), r.width(), r.height(), 6, VelaTheme.PRIMARY);
                c.text(kind.equals("offline") ? "DemoOffline" : "••••••••", r.x() + 9, r.y() + 9, 8, VelaTheme.TEXT, REGULAR);
                AccountChrome.wrapped(c, kind.equals("offline") ? "Ник должен содержать 3–16 символов A–Z, 0–9 или _." : "Введите пароль шифрования сохранённого профиля", d.message(), 8, kind.equals("offline") ? VelaTheme.DANGER : VelaTheme.TEXT_SECONDARY);
                primary = kind.equals("offline") ? "Добавить" : "Продолжить";
            }
            case "delete" -> { AccountChrome.wrapped(c, "VeryLongDemoProfileNameForClipping", d.input(), 11, VelaTheme.TEXT); primary = "Удалить"; }
            case "disclaimer" -> AccountChrome.wrapped(c, "Не передавайте другим папку с аккаунтами. При утечке смените пароль Microsoft и отзовите доступ приложения.", new Rect(d.input().x(), d.input().y(), d.input().width(), d.primary().y() - d.input().y() - 34), 8, VelaTheme.TEXT_SECONDARY);
            case "microsoft" -> {
                Rect r = d.input();
                c.round(r.x(), r.y(), r.width(), r.height(), 6, VelaTheme.SURFACE_DEEP);
                c.centered("DEMO-CODE", r.x() + r.width() / 2f, r.y() + 7, 13, VelaTheme.TEXT, SEMIBOLD);
                AccountChrome.drawButton(c, new Rect(r.x(), r.y() + 34, r.width(), 24), "Скопировать код", false, 0);
                if (d.panel().height() >= 230) AccountChrome.drawStatus(c, new Rect(r.x(), r.y() + 63, r.width(), 20), "Введите код на странице Microsoft", false);
                primary = "Открыть браузер";
            }
            case "session-error" -> {
                AccountChrome.wrapped(c, "Сохранённый вход не расшифрован. Войдите заново через Microsoft. Профиль не был удалён.", new Rect(d.input().x(), d.input().y(), d.input().width(), d.primary().y() - d.input().y() - 10), 8, VelaTheme.DANGER);
                primary = "Microsoft";
            }
        }
        AccountChrome.drawButton(c, d.cancel(), kind.equals("disclaimer") ? "Назад" : "Отмена", false, 0);
        AccountChrome.drawButton(c, d.primary(), primary, true, 0);
        if (kind.equals("delete")) c.border(d.primary().x(), d.primary().y(), d.primary().width(), d.primary().height(), 6, VelaTheme.DANGER);
    }
}
