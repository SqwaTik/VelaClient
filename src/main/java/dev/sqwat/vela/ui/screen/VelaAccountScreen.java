package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.account.IasIntegration;
import dev.sqwat.vela.account.SessionIdentity;
import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.ui.GameTextInput;
import dev.sqwat.vela.ui.TextEdit;
import dev.sqwat.vela.ui.animation.FrameClock;
import dev.sqwat.vela.ui.animation.Motion;
import dev.sqwat.vela.ui.render.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import ru.vidtu.ias.screen.VelaIasBridge;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Vela presentation only; IAS continues to own account storage and authentication. */
public final class VelaAccountScreen extends VelaScreen {
    private enum Focus { SEARCH, LIST, MICROSOFT, OFFLINE, CLOSE }
    private final Screen parent;
    private final VelaServices services;
    private final FrameClock motionClock = new FrameClock();
    private final UiControls controls = new UiControls();
    private final TextEdit searchEdit = new TextEdit();
    private final TextEdit offlineEdit = new TextEdit();
    private List<VelaIasBridge.AccountView> accounts = List.of();
    private UUID selected, pendingDelete, loginPending, menuAccount;
    private VelaMicrosoftLoginScreen microsoftLogin;
    private boolean openingMicrosoft;
    private Focus focus = Focus.SEARCH;
    private boolean keyboardFocus, disclaimerPending, offlineDialog, closing, draggingScroll, draggingText;
    private int dialogFocus;
    private float dropdown, smoothScroll, targetScroll;
    private double scrollGrab;
    private String status = "", dialogStatus = "";
    private boolean statusError;

    public VelaAccountScreen(Screen parent, VelaServices services) {
        super(Component.literal("Accounts"));
        this.parent = parent;
        this.services = services;
    }
    @Override protected float interfaceScale() { return services.configs().current().uiScale; }
    @Override protected void init() { motionClock.reset(); refresh(); }
    public void renderLoginBackdrop(GuiGraphicsExtractor g, int w, int h, float delta) { renderAsBackdrop(g, w, h, delta); }
    public void loginPending(UUID uuid) { loginPending = uuid; }
    public float revealProgress() { return dropdown; }
    public void resumeFromLogin() { loginPending = null; dropdown = 1; closing = false; motionClock.reset(); refresh(); }
    private AccountChrome.Layout layout() { return AccountChrome.layout(width, height); }
    private int scroll() { return Math.round(smoothScroll); }
    private boolean modal() { return disclaimerPending || offlineDialog || pendingDelete != null; }

    private void refresh() {
        try {
            accounts = IasIntegration.accounts();
            disclaimerPending = !IasIntegration.disclaimerAccepted();
            reconcileSelection();
        } catch (RuntimeException | LinkageError error) {
            accounts = List.of(); selected = null;
            showStatus(message(error), true);
        }
        clampScroll();
    }
    private void reconcileSelection() {
        List<VelaIasBridge.AccountView> filtered = filteredAccounts();
        if (selected != null && filtered.stream().anyMatch(a -> a.uuid().equals(selected))) return;
        selected = filtered.stream().filter(this::active).findFirst().or(() -> filtered.stream().findFirst()).map(VelaIasBridge.AccountView::uuid).orElse(null);
    }
    private void clampScroll() {
        int max = layout().maxScroll(filteredAccounts().size());
        targetScroll = Math.clamp(targetScroll, 0, max);
        smoothScroll = Math.clamp(smoothScroll, 0, max);
    }
    private boolean active(VelaIasBridge.AccountView account) {
        return SessionIdentity.matches(minecraft.getUser().getProfileId(), minecraft.getUser().getName(), account.uuid(), account.name(), account.microsoft());
    }

    @Override public void renderVela(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        float dt = motionClock.step();
        var config = services.configs().current();
        boolean immediate = config.reducedMotion || !config.animations;
        VelaTheme.setPrimary(VelaTheme.parseHex(config.accentColor));
        clampScroll();
        smoothScroll = immediate || draggingScroll ? targetScroll : Motion.scroll(smoothScroll, targetScroll, dt);
        new GameUiCanvas(g).texture("account_background",new Rect(0,0,width,height),1024,640,0xFFFFFFFF);
        dropdown = immediate ? (closing ? 0 : 1) : Motion.approach(dropdown, closing ? 0 : 1, 28, dt);
        if (closing && dropdown < .015f) { minecraft.gui.setScreen(parent); return; }
        var l = layout(); var c = new GameUiCanvas(g);
        int mx = modal() ? -1 : mouseX, my = modal() ? -1 : mouseY;
        UiClip.enable(g, l.x() - 2, l.y() - 2, l.x() + l.width() + 2, l.y() + Math.round((l.height() + 5) * dropdown));
        try {
            AccountChrome.draw(c, l, minecraft.getUser().getName(), accounts.size(), searchEdit.text(), focus == Focus.SEARCH && !modal(), mx, my, VelaTheme.PRIMARY,
                controls.animate("accounts-microsoft-hover", l.microsoft().contains(mx, my) ? 1 : 0),
                controls.animate("accounts-offline-hover", l.offline().contains(mx, my) ? 1 : 0));
            controls.iconButton(g, "accounts-close", l.back(), "x", false, l.back().contains(mx, my));
            TextFieldDraw.draw(g, searchEdit, l.searchText(), focus == Focus.SEARCH && !modal(), "Поиск по имени");
            drawRows(g, c, l, mx, my);
            var profile=account(selected);
            if(profile!=null&&l.side()>0) {
                g.pose().pushMatrix();
                AccountChrome.drawProfile(c,l,profile.name(),profile.uuid().toString(),profile.microsoft(),active(profile),
                    controls.animate("account-profile-login",l.profileLogin().contains(mx,my)?1:0));
                var avatar=l.profileAvatar();
                dev.sqwat.vela.ui.PlayerAvatars.draw(g,profile.name(),avatar.x(),avatar.y(),avatar.width());
                if(profile.uuid().equals(loginPending))ProgressSpinner.draw(c,l.profileLogin().x()+l.profileLogin().width()-18,l.profileLogin().y()+16,6,System.nanoTime());
                g.pose().popMatrix();
            }
            String hint = status.isBlank() ? "↑ ↓ Выбор · Enter Вход · Del Удалить" : status;
            if(status.isBlank())c.text(hint,l.status().x(),l.status().y()+5,8,0xFF8A8D96,UiCanvas.Weight.REGULAR);
            else AccountChrome.drawStatus(c, l.status(), hint, statusError);
            Rect menu=accountMenu();
            if(menu!=null&&!modal()){
                g.pose().pushMatrix();
                c.round(menu.x(),menu.y(),menu.width(),menu.height(),6,0xFF121721);
                c.border(menu.x(),menu.y(),menu.width(),menu.height(),6,VelaTheme.BORDER_HOVER);
                float hover=controls.animate("account-menu-delete",menu.contains(mx,my)?1:0);
                c.round(menu.x()+3,menu.y()+3,menu.width()-6,menu.height()-6,4,VelaTheme.mix(0xFF121721,0xFF33202C,hover));
                c.image("trash",menu.x()+8,menu.y()+10,11,VelaTheme.DANGER);
                c.text("Удалить аккаунт",menu.x()+27,menu.y()+12,8,VelaTheme.DANGER,UiCanvas.Weight.MEDIUM);
                g.pose().popMatrix();
            }
            if (modal())drawDialog(g,c,l,mouseX,mouseY);
            int index = l.rowAt(mx, my, filteredAccounts().size(), scroll());
            UiCursor.hand(!modal() && (l.back().contains(mx, my) || l.microsoft().contains(mx, my) || l.offline().contains(mx, my) || index >= 0 || (profile != null && l.side() > 0 && !active(profile) && l.profileLogin().contains(mx, my)) || (!searchEdit.text().isEmpty() && l.clearSearch().contains(mx, my))));
        } finally { g.disableScissor(); }
    }
    private void drawRows(GuiGraphicsExtractor g, UiCanvas c, AccountChrome.Layout l, int mx, int my) {
        List<VelaIasBridge.AccountView> filtered = filteredAccounts();
        if (filtered.isEmpty()) { AccountChrome.drawEmpty(c, l, !searchEdit.text().isBlank()); return; }
        Rect bounds = l.list(); int scroll = scroll();
        UiClip.enable(g, bounds.x() - 1, bounds.y(), bounds.x() + bounds.width() + 1, bounds.y() + bounds.height());
        boolean previousSubpixel = VelaDraw.subpixelText(Math.abs(smoothScroll-targetScroll)>.01f || draggingScroll);
        g.pose().pushMatrix();
        g.pose().translate(0, scroll-smoothScroll);
        try {
            int first = Math.max(0, scroll / l.stride()), end = Math.min(filtered.size(), (scroll + bounds.height()) / l.stride() + 1);
            for (int index = first; index < end; index++) {
                var account = filtered.get(index); var row = l.row(index, scroll); String id = account.uuid().toString();
                boolean hovered = bounds.contains(mx, my) && row.card().contains(mx, my);
                AccountChrome.drawRow(c, row, account.name(), account.microsoft(), active(account),
                    controls.animate("account-selected-" + id, account.uuid().equals(selected) ? 1 : 0),
                    controls.animate("account-hover-" + id, hovered ? 1 : 0),
                    controls.animate("account-login-" + id, hovered && row.login().contains(mx, my) ? 1 : 0),
                    controls.animate("account-delete-" + id, hovered && row.delete().contains(mx, my) ? 1 : 0));
                dev.sqwat.vela.ui.PlayerAvatars.draw(g, account.name(), row.avatar().x(), row.avatar().y(), row.avatar().width());
                if(!searchEdit.text().isBlank())SearchText.highlight(g,AccountChrome.fit(c,account.name(),row.name().width(),9,UiCanvas.Weight.MEDIUM),searchEdit.text().strip(),row.name().x(),row.name().y(),VelaDraw.Weight.MEDIUM,9);
                if (account.uuid().equals(loginPending)) {
                    if(l.side()==0)c.round(row.login().x(), row.login().y(), row.login().width(), row.login().height(), 6, VelaTheme.SURFACE);
                    ProgressSpinner.draw(c, row.login().x() + row.login().width() / 2f, row.login().y() + row.login().height() / 2f, 6, System.nanoTime());
                }
            }
        } finally { g.pose().popMatrix(); VelaDraw.subpixelText(previousSubpixel); g.disableScissor(); }
        if (l.maxScroll(filtered.size()) > 0) {
            Rect track = l.scrollTrack(), thumb = l.scrollThumb(filtered.size(), scroll);
            c.round(track.x() + 2, track.y(), 3, track.height(), 2, VelaTheme.SURFACE_DEEP);
            c.round(thumb.x() + 2, thumb.y(), 3, thumb.height(), 2, draggingScroll ? VelaTheme.PRIMARY : VelaTheme.BORDER_HOVER);
        }
    }

    private AccountChrome.Dialog dialog() { return offlineDialog?AccountChrome.authDialog(layout()):AccountChrome.dialog(layout(), disclaimerPending ? 236 : 196); }
    private Rect offlineTextBounds() { Rect r = dialog().input(); return new Rect(r.x() + 9, r.y() + 7, r.width() - 18, 14); }
    private void drawDialog(GuiGraphicsExtractor g, UiCanvas c, AccountChrome.Layout l, int mx, int my) {
        var d = dialog();
        if (disclaimerPending) {
            AccountChrome.drawDialog(c, l, d, "Безопасность аккаунтов", "Данные входа хранятся на этом ПК.");
            AccountChrome.wrapped(c, "Не передавайте другим папку с аккаунтами. При утечке смените пароль Microsoft и отзовите доступ приложения.", new Rect(d.input().x(), d.input().y(), d.input().width(), d.primary().y() - d.input().y() - 34), 8, VelaTheme.TEXT_SECONDARY);
            if (!dialogStatus.isBlank()) AccountChrome.drawStatus(c, new Rect(d.input().x(), d.primary().y() - 29, d.input().width(), 22), dialogStatus, true);
            dialogButton(c, d.cancel(), "Назад", false, "disclaimer-cancel", mx, my, 1);
            dialogButton(c, d.primary(), "Продолжить", true, "disclaimer-accept", mx, my, 2);
        } else if (offlineDialog) {
            AccountChrome.drawAuth(c,l,d,false,
                controls.animate("ms-tab",AccountChrome.authTab(d,true).contains(mx,my)?1:0),
                controls.animate("offline-tab",AccountChrome.authTab(d,false).contains(mx,my)?1:0),
                controls.animate("auth-close",AccountChrome.authClose(d).contains(mx,my)?1:0));
            Rect r = d.input();
            c.round(r.x(), r.y(), r.width(), r.height(), 6, VelaTheme.SURFACE_DEEP);
            c.border(r.x(), r.y(), r.width(), r.height(), 6, dialogFocus == 0 ? VelaTheme.PRIMARY : VelaTheme.BORDER);
            TextFieldDraw.draw(g, offlineEdit, offlineTextBounds(), dialogFocus == 0, "Ник игрока");
            AccountChrome.wrapped(c, dialogStatus.isBlank() ? "3–16 символов: A–Z, 0–9 и _" : dialogStatus, d.message(), 8, dialogStatus.isBlank() ? VelaTheme.TEXT_SECONDARY : VelaTheme.DANGER);
            dialogButton(c, d.cancel(), "Отмена", false, "offline-cancel", mx, my, 1);
            dialogButton(c, d.primary(), "Добавить", true, "offline-add", mx, my, 2);
        } else {
            var account = account(pendingDelete);
            AccountChrome.drawDialog(c, l, d, "Удалить аккаунт?", "Будет удалён только сохранённый профиль.");
            AccountChrome.wrapped(c, account == null ? "Аккаунт больше недоступен" : account.name(), d.input(), 11, VelaTheme.TEXT);
            if (!dialogStatus.isBlank()) AccountChrome.wrapped(c, dialogStatus, d.message(), 8, VelaTheme.DANGER);
            dialogButton(c, d.cancel(), "Отмена", false, "delete-cancel", mx, my, 1);
            dialogButton(c, d.primary(), "Удалить", false, "delete-confirm", mx, my, 2);
            c.border(d.primary().x(), d.primary().y(), d.primary().width(), d.primary().height(), 6, VelaTheme.DANGER);
        }
        UiCursor.hand(d.cancel().contains(mx, my) || d.primary().contains(mx, my));
    }
    private void dialogButton(UiCanvas c, Rect rect, String label, boolean primary, String id, int mx, int my, int index) {
        AccountChrome.drawButton(c, rect, label, primary, controls.animate("accounts-" + id, rect.contains(mx, my) ? 1 : 0));
    }

    @Override public boolean clickVela(MouseButtonEvent event, boolean doubled) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.clickVela(event, doubled);
        if (closing) return true;
        double mx = event.x(), my = event.y(); keyboardFocus = false;
        if (modal()) {
            var d = dialog();
            if(offlineDialog&&d.panel().height()>=340){
                if(AccountChrome.authClose(d).contains(mx,my)){cancelDialog();return true;}
                if(AccountChrome.authTab(d,true).contains(mx,my)){cancelDialog();addMicrosoft();return true;}
            }
            if (offlineDialog && d.input().contains(mx, my)) {
                dialogFocus = 0; draggingText = true;
                TextFieldDraw.click(offlineEdit, offlineTextBounds(), mx, (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0);
                if (doubled) offlineEdit.selectWord();
            } else if (d.cancel().contains(mx, my)) cancelDialog();
            else if (d.primary().contains(mx, my)) confirmDialog();
            return true;
        }
        var l = layout(); var filtered = filteredAccounts();
        Rect menu=accountMenu();UUID previousMenu=menuAccount;menuAccount=null;
        if(menu!=null&&menu.contains(mx,my)){selected=previousMenu;requestDelete();return true;}
        if(l.side()>0&&l.profileLogin().contains(mx,my)) {loginSelected();return true;}
        if (l.back().contains(mx, my) || !l.panel().contains(mx, my)) { onClose(); return true; }
        if (l.search().contains(mx, my)) {
            focus = Focus.SEARCH;
            if (!searchEdit.text().isEmpty() && l.clearSearch().contains(mx, my)) { searchEdit.set(""); searchChanged(); }
            else { draggingText = true; TextFieldDraw.click(searchEdit, l.searchText(), mx, (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0); if (doubled) searchEdit.selectWord(); }
            return true;
        }
        if (l.microsoft().contains(mx, my)) { focus = Focus.MICROSOFT; addMicrosoft(); return true; }
        if (l.offline().contains(mx, my)) { focus = Focus.OFFLINE; openOffline(); return true; }
        if (l.maxScroll(filtered.size()) > 0 && l.scrollTrack().contains(mx, my)) {
            draggingScroll = true; Rect thumb = l.scrollThumb(filtered.size(), scroll());
            scrollGrab = thumb.contains(mx, my) ? my - thumb.y() : thumb.height() / 2d; scrollTo(my); return true;
        }
        int index = l.rowAt(mx, my, filtered.size(), scroll());
        if (index >= 0) {
            var account = filtered.get(index);
            var action = AccountChrome.action(l, mx, my, filtered.size(), scroll(), active(account));
            if (action == AccountChrome.RowAction.DELETE && l.side() > 0) {
                menuAccount = account.uuid().equals(previousMenu) ? null : account.uuid();
                return true;
            }
            selected = account.uuid(); focus = Focus.LIST;
            switch (action) {
                case LOGIN -> loginSelected();
                case DELETE -> {if(l.side()>0)menuAccount=account.uuid().equals(previousMenu)?null:account.uuid();else requestDelete();}
                default -> { if (doubled) loginSelected(); }
            }
        }
        return true;
    }
    @Override public boolean scrollVela(double mouseX, double mouseY, double horizontal, double vertical) {
        if (modal() || closing) return true;
        menuAccount=null;
        if (layout().list().contains(mouseX, mouseY) || layout().scrollTrack().contains(mouseX, mouseY)) {
            targetScroll = (float)Math.clamp(targetScroll - vertical * layout().stride(), 0, layout().maxScroll(filteredAccounts().size()));
            return true;
        }
        return super.scrollVela(mouseX, mouseY, horizontal, vertical);
    }
    @Override public boolean dragVela(MouseButtonEvent event, double dx, double dy) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.dragVela(event, dx, dy);
        if (draggingScroll && !modal()) { scrollTo(event.y()); return true; }
        if (draggingText && offlineDialog) { TextFieldDraw.click(offlineEdit, offlineTextBounds(), event.x(), true); return true; }
        if (draggingText && focus == Focus.SEARCH && !modal()) { TextFieldDraw.click(searchEdit, layout().searchText(), event.x(), true); return true; }
        return super.dragVela(event, dx, dy);
    }
    private void scrollTo(double y) { targetScroll = smoothScroll = layout().scrollFromPointer(y, scrollGrab, filteredAccounts().size()); }
    @Override protected boolean releaseVela(MouseButtonEvent event) { draggingScroll = draggingText = false; return true; }

    @Override public boolean keyPressed(KeyEvent event) {
        if (closing) return true;
        keyboardFocus = true; int key = event.key();
        boolean shift = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if(menuAccount!=null)menuAccount=null;
            else if (modal()) cancelDialog();
            else if (focus == Focus.SEARCH && !searchEdit.text().isEmpty()) { searchEdit.set(""); searchChanged(); }
            else onClose();
            return true;
        }
        if (modal()) {
            if (key == GLFW.GLFW_KEY_TAB) {
                int first = offlineDialog ? 0 : 1;
                dialogFocus = first + Math.floorMod(dialogFocus - first + (shift ? -1 : 1), 3 - first);
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || (key == GLFW.GLFW_KEY_SPACE && dialogFocus != 0)) {
                if (dialogFocus == 1) cancelDialog(); else confirmDialog();
            } else if (offlineDialog && dialogFocus == 0) GameTextInput.key(offlineEdit, event, 16, VelaAccountScreen::validName);
            return true;
        }
        if (key == GLFW.GLFW_KEY_F && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) { focus = Focus.SEARCH; searchEdit.selectAll(); return true; }
        if (key == GLFW.GLFW_KEY_TAB) { focus = Focus.values()[Math.floorMod(focus.ordinal() + (shift ? -1 : 1), Focus.values().length)]; return true; }
        if (focus == Focus.SEARCH) {
            if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { focus = Focus.LIST; moveSelection(0); }
            else { String before = searchEdit.text(); GameTextInput.key(searchEdit, event, 64); if (!before.equals(searchEdit.text())) searchChanged(); }
            return true;
        }
        if (focus == Focus.LIST) {
            switch (key) {
                case GLFW.GLFW_KEY_UP -> moveSelection(-1);
                case GLFW.GLFW_KEY_DOWN -> moveSelection(1);
                case GLFW.GLFW_KEY_PAGE_UP -> moveSelection(-Math.max(1, layout().list().height() / layout().stride()));
                case GLFW.GLFW_KEY_PAGE_DOWN -> moveSelection(Math.max(1, layout().list().height() / layout().stride()));
                case GLFW.GLFW_KEY_HOME -> moveSelection(-accounts.size());
                case GLFW.GLFW_KEY_END -> moveSelection(accounts.size());
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> loginSelected();
                case GLFW.GLFW_KEY_DELETE -> requestDelete();
                default -> { }
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            switch (focus) { case MICROSOFT -> addMicrosoft(); case OFFLINE -> openOffline(); case CLOSE -> onClose(); default -> { } }
            return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean charTyped(CharacterEvent event) {
        if (closing || disclaimerPending || pendingDelete != null) return true;
        if (offlineDialog) {
            if (dialogFocus == 0 && event.isAllowedChatCharacter()) offlineEdit.insert(validName(event.codepointAsString()), 16);
            return true;
        }
        if (focus == Focus.SEARCH && event.isAllowedChatCharacter()) { searchEdit.insert(event.codepointAsString(), 64); searchChanged(); return true; }
        return super.charTyped(event);
    }
    private void moveSelection(int change) {
        menuAccount=null;
        List<VelaIasBridge.AccountView> filtered = filteredAccounts();
        if (filtered.isEmpty()) { selected = null; return; }
        int index = 0;
        for (int i = 0; i < filtered.size(); i++) if (filtered.get(i).uuid().equals(selected)) { index = i; break; }
        index = Math.clamp(index + change, 0, filtered.size() - 1); selected = filtered.get(index).uuid();
        targetScroll = layout().reveal(index, scroll(), filtered.size());
    }
    private void searchChanged() { menuAccount=null;targetScroll = smoothScroll = 0; draggingScroll = false; reconcileSelection(); }
    private Rect accountMenu(){
        if(menuAccount==null)return null;var list=filteredAccounts();var l=layout();
        for(int i=0;i<list.size();i++)if(list.get(i).uuid().equals(menuAccount)){
            var row=l.row(i,scroll());if(row.card().y()+row.card().height()<l.list().y()||row.card().y()>=l.list().y()+l.list().height())return null;
            return new Rect(row.delete().x()+row.delete().width()-118,Math.clamp(row.delete().y()+25,l.list().y(),l.list().y()+l.list().height()-34),118,34);
        }return null;
    }
    private void loginSelected() {
        var account = account(selected);
        if (account == null || active(account) || loginPending != null) return;
        try { IasIntegration.login(this, account.uuid()); }
        catch (RuntimeException | LinkageError error) { showStatus(message(error), true); }
    }
    private void requestDelete() {
        var account = account(selected);
        if (account == null || active(account)) return;
        pendingDelete = account.uuid(); dialogStatus = ""; dialogFocus = 1; draggingScroll = draggingText = false;
    }
    private void addMicrosoft() {
        try {
            if(microsoftLogin==null||!microsoftLogin.resumable())microsoftLogin=new VelaMicrosoftLoginScreen(this,this::refresh);
            microsoftLogin.resume();openingMicrosoft=true;
            try{IasIntegration.openMicrosoft(microsoftLogin);}finally{openingMicrosoft=false;}
        }
        catch (RuntimeException | LinkageError error) { showStatus(message(error), true); }
    }
    private void openOffline() { offlineDialog = true; offlineEdit.set(""); dialogStatus = ""; dialogFocus = 0; draggingScroll = draggingText = false; }
    public void openOfflineFromLogin(){resumeFromLogin();openOffline();}
    private void cancelDialog() {
        if (disclaimerPending) { onClose(); return; }
        offlineDialog = false; pendingDelete = null; offlineEdit.set(""); dialogStatus = ""; draggingText = false;
    }
    private void confirmDialog() {
        try {
            if (disclaimerPending) { IasIntegration.acceptDisclaimer(); disclaimerPending = false; }
            else if (offlineDialog) {
                if (!offlineEdit.text().matches("[A-Za-z0-9_]{3,16}")) { dialogStatus = "Ник должен содержать 3–16 символов A–Z, 0–9 или _."; dialogFocus = 0; return; }
                IasIntegration.addOffline(offlineEdit.text(), this::refresh);
                offlineDialog = false; offlineEdit.set(""); showStatus("Offline-аккаунт добавлен", false);
            } else if (pendingDelete != null) {
                var target = account(pendingDelete);
                if (target == null || active(target)) { pendingDelete = null; return; }
                IasIntegration.delete(pendingDelete, this::refresh); pendingDelete = null; showStatus("Аккаунт удалён", false);
            }
            dialogStatus = "";
        } catch (RuntimeException | LinkageError error) { dialogStatus = message(error); showStatus(dialogStatus, true); }
    }
    private List<VelaIasBridge.AccountView> filteredAccounts() {
        String query = searchEdit.text().strip().toLowerCase(Locale.ROOT);
        return query.isEmpty() ? accounts : accounts.stream().filter(a -> a.name().toLowerCase(Locale.ROOT).contains(query)).toList();
    }
    private VelaIasBridge.AccountView account(UUID uuid) { return uuid == null ? null : accounts.stream().filter(a -> a.uuid().equals(uuid)).findFirst().orElse(null); }
    private void showStatus(String value, boolean error) { status = value == null || value.isBlank() ? "Неизвестная ошибка" : value; statusError = error; }
    private static String message(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        return root.getMessage() == null || root.getMessage().isBlank() ? "Не удалось выполнить действие. Повторите попытку." : root.getMessage();
    }
    private static String validName(String value) {
        String cleaned = value == null ? "" : value.strip().replaceAll("[^A-Za-z0-9_]", "");
        return cleaned.substring(0, Math.min(16, cleaned.length()));
    }
    @Override public void onClose() { if(microsoftLogin!=null)microsoftLogin.cancelPending();draggingScroll = draggingText = false; closing = true; }
    @Override public void removed(){if(microsoftLogin!=null&&!openingMicrosoft)microsoftLogin.cancelPending();super.removed();}
    @Override public boolean isPauseScreen() { return false; }
}
