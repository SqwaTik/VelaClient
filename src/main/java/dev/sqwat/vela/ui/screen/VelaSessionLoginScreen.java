package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.account.IasIntegration;
import dev.sqwat.vela.ui.render.Rect;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import dev.sqwat.vela.ui.render.AccountChrome;
import dev.sqwat.vela.ui.render.GameUiCanvas;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import ru.vidtu.ias.account.MicrosoftAccount;
import ru.vidtu.ias.auth.LoginData;
import ru.vidtu.ias.auth.handlers.LoginHandler;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Vela-styled progress and password UI for switching an IAS-backed session. */
public final class VelaSessionLoginScreen extends VelaScreen implements LoginHandler {
    private final Screen parent;
    @Override protected float interfaceScale(){return parent instanceof VelaScreen screen?screen.interfaceScale():1;}
    private final UUID accountUuid;
    private Rect back = empty();
    private Rect retry = empty();
    private Rect confirmPassword = empty();
    private Rect passwordHit = empty();
    private volatile String stage = "Подготавливаем аккаунт…";
    private volatile String error = "";
    private volatile boolean cancelled;
    private volatile CompletableFuture<String> passwordFuture;
    private String passwordValue = "";
    private boolean started;
    private final dev.sqwat.vela.ui.animation.FrameClock transitionClock=new dev.sqwat.vela.ui.animation.FrameClock();
    private float reveal;
    private boolean closing;
    private boolean needsReauthentication;
    private final dev.sqwat.vela.ui.render.UiControls controls=new dev.sqwat.vela.ui.render.UiControls();
    private long completedAt;
    private float successVisible;
    private int focusIndex;
    private boolean keyboardFocus;

    public VelaSessionLoginScreen(Screen parent, UUID accountUuid) {
        super(Component.literal("Switch Account"));
        this.parent = parent;
        this.accountUuid = accountUuid;
    }

    @Override protected void init() { if (!started) begin(); }

    private void begin() {
        started = true;
        cancelled = false;
        error = "";
        passwordValue = "";
        passwordFuture = null;
        stage = "Подготавливаем аккаунт…";
        try { IasIntegration.startLogin(accountUuid, this); }
        catch (RuntimeException cause) { error(cause); }
    }

    @Override
    public void renderVela(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        float dt=transitionClock.step();
        if(completedAt!=0){successVisible+=dt;if(successVisible>1.2f)closing=true;}
        reveal=dev.sqwat.vela.ui.animation.Motion.approach(reveal,closing?0:1,16,dt);
        if(parent instanceof VelaAccountScreen account){account.loginPending(completedAt==0&&error.isBlank()?accountUuid:null);account.renderLoginBackdrop(g,width,height,delta);}
        else if(parent instanceof VelaScreen screen)screen.renderAsBackdrop(g,width,height,delta);
        if(closing&&reveal<.01f){if(parent instanceof VelaAccountScreen account)account.resumeFromLogin();minecraft.gui.setScreen(parent);return;}
        var layout = AccountChrome.layout(width, height);
        var d = AccountChrome.dialog(layout, 230);
        var c = new GameUiCanvas(g);
        g.nextStratum();
        if(error.isBlank()&&(passwordFuture==null||passwordFuture.isDone())){
            AccountChrome.drawLoginProgress(c,layout,completedAt!=0,successVisible,System.nanoTime());
            back=retry=confirmPassword=passwordHit=empty();return;
        }
        AccountChrome.drawDialog(c, layout, d, !error.isBlank() ? "Ошибка входа" : "Вход в аккаунт", !error.isBlank() ? "Сохранённый профиль остаётся в списке" : "Esc — отменить и вернуться к аккаунтам");
        back = d.cancel(); retry = confirmPassword = passwordHit = empty();
        if (!error.isBlank()) {
            AccountChrome.wrapped(c, error, new Rect(d.input().x(), d.input().y(), d.input().width(), d.primary().y() - d.input().y() - 10), 8, VelaTheme.DANGER);
            retry = d.primary(); button(g, retry, needsReauthentication ? "Microsoft" : "Повторить", true, mouseX, mouseY);
        } else if (passwordFuture != null && !passwordFuture.isDone()) {
            passwordHit = d.input();
            c.round(passwordHit.x(), passwordHit.y(), passwordHit.width(), passwordHit.height(), 6, VelaTheme.SURFACE_DEEP);
            c.border(passwordHit.x(), passwordHit.y(), passwordHit.width(), passwordHit.height(), 6, focusIndex == 0 ? VelaTheme.PRIMARY : VelaTheme.BORDER);
            String dots = "•".repeat(passwordValue.codePointCount(0, passwordValue.length()));
            VelaDraw.text(g, VelaDraw.ellipsize(dots, passwordHit.width() - 18, VelaDraw.Weight.REGULAR, 8), passwordHit.x() + 9, passwordHit.y() + 9, VelaTheme.TEXT, VelaDraw.Weight.REGULAR, 8);
            if (focusIndex == 0) VelaDraw.roundedRect(g, passwordHit.x() + 9 + Math.round(Math.min(passwordHit.width() - 20, VelaDraw.textWidth(dots, VelaDraw.Weight.REGULAR, 8))), passwordHit.y() + 8, 1, 10, 0, VelaDraw.caretColor());
            AccountChrome.wrapped(c, "Введите пароль шифрования сохранённого профиля", d.message(), 8, VelaTheme.TEXT_SECONDARY);
            confirmPassword = d.primary(); button(g, confirmPassword, "Продолжить", true, mouseX, mouseY);
        } else {
            dev.sqwat.vela.ui.render.ProgressSpinner.draw(c, d.input().x() + d.input().width() / 2f, d.input().y() + 13, 10, System.nanoTime());
            AccountChrome.wrapped(c, stage, d.message(), 8, VelaTheme.TEXT_SECONDARY);
        }
        button(g, back, "Отмена", false, mouseX, mouseY);
        if (keyboardFocus && focusIndex > 0) AccountChrome.focus(c, focusIndex == 1 ? back : !error.isBlank() ? retry : confirmPassword);
        dev.sqwat.vela.ui.render.UiCursor.hand(back.contains(mouseX, mouseY) || retry.contains(mouseX, mouseY) || confirmPassword.contains(mouseX, mouseY));
    }

    private void button(GuiGraphicsExtractor g, Rect rect, String label, boolean primary, int mx, int my) {
        AccountChrome.drawButton(new GameUiCanvas(g), rect, label, primary, controls.animate("session-" + label, rect.contains(mx, my) ? 1 : 0));
    }

    @Override
    public boolean clickVela(MouseButtonEvent event, boolean doubled) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.clickVela(event, doubled);
        if (closing) return true;
        keyboardFocus = false;
        if (back.contains(event.x(), event.y())) { onClose(); return true; }
        if (retry.contains(event.x(), event.y())) { retryLogin(); return true; }
        if (passwordHit.contains(event.x(), event.y())) { focusIndex = 0; return true; }
        if (confirmPassword.contains(event.x(), event.y())) { submitPassword(); return true; }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (closing) return true;
        keyboardFocus = true;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        boolean password = passwordFuture != null && !passwordFuture.isDone() && error.isBlank();
        if (event.key() == GLFW.GLFW_KEY_TAB) {
            int first = password ? 0 : 1, count = password ? 3 : error.isBlank() ? 1 : 2;
            focusIndex = first + Math.floorMod(focusIndex - first + ((event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1), count);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER || (event.key() == GLFW.GLFW_KEY_SPACE && focusIndex > 0)) {
            if (focusIndex == 1) onClose();
            else if (!error.isBlank()) retryLogin();
            else if (password) submitPassword();
            return true;
        }
        if (password && focusIndex == 0) {
            if(event.key()==GLFW.GLFW_KEY_BACKSPACE&&(event.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0){var edit=new dev.sqwat.vela.ui.TextEdit();edit.set(passwordValue);edit.eraseWord();passwordValue=edit.text();return true;}
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !passwordValue.isEmpty())
                passwordValue = passwordValue.substring(0, passwordValue.offsetByCodePoints(passwordValue.length(), -1));
            else if (event.key() == GLFW.GLFW_KEY_ENTER) submitPassword();
            else if ((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0 && event.key() == GLFW.GLFW_KEY_V) {
                String pasted = minecraft.keyboardHandler.getClipboard();
                passwordValue = pasted.substring(0, Math.min(32, pasted.length()));
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!closing && error.isBlank() && focusIndex == 0 && passwordFuture != null && !passwordFuture.isDone() && event.isAllowedChatCharacter() && passwordValue.length() < 32) {
            passwordValue += event.codepointAsString();
            return true;
        }
        return super.charTyped(event);
    }

    private void retryLogin() {
        if (needsReauthentication) { cancelled = true; IasIntegration.addMicrosoft(parent, () -> {}); }
        else { started = false; begin(); }
    }

    private void submitPassword() {
        CompletableFuture<String> future = passwordFuture;
        if (future == null || future.isDone() || passwordValue.isBlank()) return;
        future.complete(passwordValue);
        passwordValue = "";
        stage = "Расшифровываем аккаунт…";
    }

    @Override public boolean cancelled() { return cancelled || minecraft == null || minecraft.gui.screen() != this; }

    @Override
    public void stage(@NotNull String key, @Nullable Object @NotNull ... args) {
        stage = switch (key) {
            case MicrosoftAccount.INITIALIZING -> "Подготавливаем аккаунт";
            case MicrosoftAccount.DECRYPTING -> "Расшифровываем сохранённую сессию";
            case MicrosoftAccount.MSA_TO_XBL, MicrosoftAccount.XBL_TO_XSTS -> "Проверяем Xbox-профиль";
            case MicrosoftAccount.XSTS_TO_MCA, MicrosoftAccount.MCA_TO_MCP -> "Проверяем лицензию Minecraft";
            case MicrosoftAccount.ENCRYPTING -> "Обновляем защищённые данные";
            case MicrosoftAccount.FINALIZING -> "Применяем игровую сессию";
            default -> "Выполняется вход";
        };
    }

    @Override
    public @NotNull CompletableFuture<String> password() {
        CompletableFuture<String> future = new CompletableFuture<>();
        passwordFuture = future;
        stage = "Нужен пароль шифрования";
        return future;
    }

    @Override
    public void success(@NotNull LoginData data, boolean changed) {
        stage = "Применяем игровую сессию";
        IasIntegration.applySession(data, changed).thenRunAsync(() -> {
            if (!cancelled()){stage="Вход выполнен";completedAt=System.currentTimeMillis();}
        }, minecraft).exceptionally(cause -> {
            error(cause);
            return null;
        });
    }

    @Override
    public void error(@NotNull Throwable cause) {
        Throwable root = cause;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String detail = root.getMessage();
        needsReauthentication=root instanceof javax.crypto.AEADBadTagException||detail!=null&&detail.contains("Tag mismatch");
        error = needsReauthentication?"Сохранённый вход не расшифрован. Войдите заново.":detail == null || detail.isBlank() ? "Проверьте интернет и повторите попытку" : detail;
    }

    @Override
    public void onClose() {
        cancelled = true;
        CompletableFuture<String> future = passwordFuture;
        if (future != null && !future.isDone()) future.complete(null);
        closing=true;
    }

    @Override public boolean isPauseScreen() { return false; }
    private static Rect empty() { return new Rect(0, 0, 0, 0); }
    private static String ellipsis(String value, int max) { return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…"; }
}
