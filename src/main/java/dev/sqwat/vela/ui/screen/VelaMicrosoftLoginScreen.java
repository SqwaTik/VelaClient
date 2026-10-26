package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.account.IasIntegration;
import dev.sqwat.vela.ui.render.Rect;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import dev.sqwat.vela.ui.render.AccountChrome;
import dev.sqwat.vela.ui.render.GameUiCanvas;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import ru.vidtu.ias.account.MicrosoftAccount;
import ru.vidtu.ias.auth.handlers.CreateHandler;
import ru.vidtu.ias.auth.microsoft.MSAuthClient;
import ru.vidtu.ias.crypt.HardwareCrypt;

import java.net.URI;

/** Vela-styled Microsoft device-code flow powered by IAS authentication. */
public final class VelaMicrosoftLoginScreen extends VelaScreen implements CreateHandler {
    private final Screen parent;
    @Override protected float interfaceScale(){return parent instanceof VelaScreen screen?screen.interfaceScale():1;}
    private final Runnable refresh;
    private Rect back = empty();
    private Rect copy = empty();
    private Rect open = empty();
    private Rect retry = empty();
    private volatile String stage = "Подключаемся к Microsoft…";
    private volatile String error = "";
    private volatile String userCode = "";
    private volatile URI verificationUri;
    private volatile boolean cancelled;
    private MSAuthClient auth;
    private boolean started;
    private volatile boolean suspended;
    private boolean codeFocused,draggingCode;
    private volatile int generation;
    private Rect codeBounds=empty();
    private final dev.sqwat.vela.ui.TextEdit codeSelection=new dev.sqwat.vela.ui.TextEdit();
    public boolean resumable(){return !cancelled;}
    public void resume(){closing=false;transitionClock.reset();}
    public void cancelPending(){cancelled=true;suspended=false;authClose();}
    private final dev.sqwat.vela.ui.animation.FrameClock transitionClock=new dev.sqwat.vela.ui.animation.FrameClock();
    private float reveal;
    private boolean closing,completed;
    private float successVisible;
    private int focusIndex;
    private boolean keyboardFocus;
    private final dev.sqwat.vela.ui.render.UiControls controls=new dev.sqwat.vela.ui.render.UiControls();

    public VelaMicrosoftLoginScreen(Screen parent, Runnable refresh) {
        super(Component.literal("Microsoft Login"));
        this.parent = parent;
        this.refresh = refresh;
    }

    @Override
    protected void init() {
        // Selecting an authentication tab is not consent to start a browser flow.
        // init runs after Gui assigns the new screen; preserve suspension until then.
        suspended=false;
    }

    private void begin() {
        started = true;
        cancelled = false;
        error = "";
        userCode = "";
        codeSelection.set("");codeFocused=draggingCode=false;
        verificationUri = null;
        stage = "Подключаемся к Microsoft…";
        int attempt=++generation;
        CreateHandler handler=new CreateHandler(){
            @Override public boolean cancelled(){return attempt!=generation||VelaMicrosoftLoginScreen.this.cancelled();}
            @Override public void stage(@NotNull String key,@Nullable Object @NotNull ... args){minecraft.execute(()->{if(!cancelled())VelaMicrosoftLoginScreen.this.stage(key,args);});}
            @Override public void success(@NotNull MicrosoftAccount account){minecraft.execute(()->{if(!cancelled())VelaMicrosoftLoginScreen.this.success(account);});}
            @Override public void error(@NotNull Throwable cause){minecraft.execute(()->{if(!cancelled())VelaMicrosoftLoginScreen.this.error(cause);});}
        };
        auth = new MSAuthClient(HardwareCrypt.INSTANCE_V2, handler);
        auth.start().thenAccept(device -> {
            if (device == null || handler.cancelled()) return;
            minecraft.execute(() -> {
                if (handler.cancelled()) return;
                userCode = device.user();
                codeSelection.set(userCode);
                verificationUri = device.uri();
                stage = "Введите код на странице Microsoft";
                minecraft.keyboardHandler.setClipboard(userCode);
                if(!suspended)Util.getPlatform().openUri(verificationUri);
            });
        }).exceptionally(cause -> {
            handler.error(cause);
            return null;
        });
    }

    @Override
    public void renderVela(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        float dt=transitionClock.step();
        if(completed){successVisible+=dt;if(successVisible>1.2f)closing=true;}
        var settings=dev.sqwat.vela.VelaClient.services().configs().current();
        reveal=settings.reducedMotion||!settings.animations?(closing?0:1):dev.sqwat.vela.ui.animation.Motion.approach(reveal,closing?0:1,18,dt);
        if(parent instanceof VelaAccountScreen account)account.renderLoginBackdrop(g,width,height,delta);
        else if(parent instanceof VelaScreen screen)screen.renderAsBackdrop(g,width,height,delta);
        if(closing&&reveal<.01f){if(parent instanceof VelaAccountScreen account)account.resumeFromLogin();minecraft.gui.setScreen(parent);return;}
        var l = AccountChrome.layout(width, height);
        var d = AccountChrome.authDialog(l);
        var c = new GameUiCanvas(g);
        g.nextStratum();
        if(completed){AccountChrome.drawLoginProgress(c,l,true,successVisible,System.nanoTime());return;}
        g.pose().pushMatrix();
        AccountChrome.drawAuth(c,l,d,true,
            controls.animate("ms-tab",AccountChrome.authTab(d,true).contains(mouseX,mouseY)?1:0),
            controls.animate("offline-tab",AccountChrome.authTab(d,false).contains(mouseX,mouseY)?1:0),
            controls.animate("auth-close",AccountChrome.authClose(d).contains(mouseX,mouseY)?1:0));
        back = d.cancel(); copy = open = retry = codeBounds = empty();
        if (completed) {
            AccountChrome.wrapped(c, "Аккаунт добавлен", d.input(), 11, VelaTheme.POSITIVE);
        } else if (!started) {
            AccountChrome.wrapped(c, "Войдите через Microsoft, чтобы добавить лицензионный аккаунт.", d.input(), 9, VelaTheme.TEXT_SECONDARY);
            retry = d.primary(); button(g, retry, "Войти через Microsoft", true, mouseX, mouseY);
        } else if (!error.isBlank()) {
            AccountChrome.wrapped(c, error, new Rect(d.input().x(), d.input().y(), d.input().width(), d.primary().y() - d.input().y() - 10), 8, VelaTheme.DANGER);
            retry = d.primary(); button(g, retry, "Повторить", true, mouseX, mouseY);
        } else if (userCode.isBlank()) {
            dev.sqwat.vela.ui.render.ProgressSpinner.draw(c, d.input().x() + d.input().width() / 2f, d.input().y() + 13, 10, System.nanoTime());
            AccountChrome.wrapped(c, stage, d.message(), 8, VelaTheme.TEXT_SECONDARY);
        } else {
            c.round(d.input().x(), d.input().y(), d.input().width(), d.input().height(), 6, VelaTheme.SURFACE_DEEP);
            float codeWidth=VelaDraw.textWidth(userCode,VelaDraw.Weight.SEMIBOLD,13);
            codeBounds=new Rect(Math.round(d.input().x()+(d.input().width()-codeWidth)/2),d.input().y()+7,Math.round(codeWidth),16);
            if(codeFocused&&codeSelection.start()!=codeSelection.end()){
                float left=VelaDraw.textWidth(userCode.substring(0,codeSelection.start()),VelaDraw.Weight.SEMIBOLD,13);
                float right=VelaDraw.textWidth(userCode.substring(0,codeSelection.end()),VelaDraw.Weight.SEMIBOLD,13);
                c.round(codeBounds.x()+left,codeBounds.y()-1,right-left,16,2,0x66000000|(VelaTheme.PRIMARY&0xFFFFFF));
            }
            c.centered(userCode, d.input().x() + d.input().width() / 2f, d.input().y() + 7, 13, VelaTheme.TEXT, dev.sqwat.vela.ui.render.UiCanvas.Weight.SEMIBOLD);
            copy = new Rect(d.input().x(), d.input().y() + 34, d.input().width(), 24);
            open = d.primary();
            button(g, copy, "Скопировать код", false, mouseX, mouseY);
            button(g, open, "Открыть браузер", true, mouseX, mouseY);
            if (d.panel().height() >= 230) AccountChrome.drawStatus(c, new Rect(d.input().x(), copy.y() + 29, d.input().width(), 20), stage, false);
        }
        button(g, back, "Отмена · Esc", false, mouseX, mouseY);
        if (keyboardFocus) AccountChrome.focus(c, focusedControl());
        dev.sqwat.vela.ui.render.UiCursor.hand(back.contains(mouseX, mouseY) || copy.contains(mouseX, mouseY) || open.contains(mouseX, mouseY) || retry.contains(mouseX, mouseY));
        g.pose().popMatrix();
    }

    private void button(GuiGraphicsExtractor g, Rect rect, String label, boolean primary, int mx, int my) {
        AccountChrome.drawButton(new GameUiCanvas(g), rect, label, primary, controls.animate("microsoft-" + label, rect.contains(mx, my) ? 1 : 0));
    }

    @Override
    public boolean clickVela(MouseButtonEvent event, boolean doubled) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.clickVela(event, doubled);
        if (closing || completed) return true;
        var dialog=AccountChrome.authDialog(AccountChrome.layout(width,height));
        if(dialog.panel().height()>=340){
            if(AccountChrome.authClose(dialog).contains(event.x(),event.y())){onClose();return true;}
            if(AccountChrome.authTab(dialog,false).contains(event.x(),event.y())&&parent instanceof VelaAccountScreen account){suspended=true;draggingCode=false;account.openOfflineFromLogin();minecraft.gui.setScreen(parent);return true;}
        }
        keyboardFocus = false;
        codeFocused=codeBounds.contains(event.x(),event.y());
        if(codeFocused){selectCode(event.x(),false);if(doubled)codeSelection.selectAll();draggingCode=true;return true;}
        if (back.contains(event.x(), event.y())) { onClose(); return true; }
        if (copy.contains(event.x(), event.y()) && !userCode.isBlank()) {
            minecraft.keyboardHandler.setClipboard(userCode);
            stage = "Код скопирован";
            return true;
        }
        if (open.contains(event.x(), event.y()) && verificationUri != null) {
            Util.getPlatform().openUri(verificationUri);
            return true;
        }
        if (retry.contains(event.x(), event.y())) {
            authClose();
            begin();
            return true;
        }
        return true;
    }

    private java.util.List<Rect> focusControls() {
        var result = new java.util.ArrayList<Rect>();
        result.add(back);
        if (copy.width() > 0) result.add(copy);
        if (open.width() > 0) result.add(open);
        if (retry.width() > 0) result.add(retry);
        return result;
    }
    private Rect focusedControl() { var list = focusControls(); focusIndex = Math.floorMod(focusIndex, list.size()); return list.get(focusIndex); }
    @Override public boolean keyPressed(KeyEvent event) {
        if (closing || completed) return true;
        keyboardFocus = true;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if(codeFocused){
            boolean ctrl=(event.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0,shift=(event.modifiers()&GLFW.GLFW_MOD_SHIFT)!=0;
            if(ctrl&&event.key()==GLFW.GLFW_KEY_A){codeSelection.selectAll();return true;}
            if(ctrl&&event.key()==GLFW.GLFW_KEY_C){minecraft.keyboardHandler.setClipboard(codeSelection.selected());return true;}
            if(event.key()==GLFW.GLFW_KEY_LEFT||event.key()==GLFW.GLFW_KEY_RIGHT){codeSelection.arrow(event.key()==GLFW.GLFW_KEY_LEFT?-1:1,shift);return true;}
            if(event.key()==GLFW.GLFW_KEY_HOME||event.key()==GLFW.GLFW_KEY_END){codeSelection.move(event.key()==GLFW.GLFW_KEY_HOME?0:userCode.length(),shift);return true;}
            if(event.key()!=GLFW.GLFW_KEY_TAB)return true;
            codeFocused=false;
        }
        if (event.key() == GLFW.GLFW_KEY_TAB) { focusIndex = Math.floorMod(focusIndex + ((event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1), focusControls().size()); return true; }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER || event.key() == GLFW.GLFW_KEY_SPACE) {
            Rect r = focusedControl();
            if (r.equals(back)) onClose();
            else if (r.equals(copy) && !userCode.isBlank()) { minecraft.keyboardHandler.setClipboard(userCode); stage = "Код скопирован"; }
            else if (r.equals(open) && verificationUri != null) Util.getPlatform().openUri(verificationUri);
            else if (r.equals(retry)) { authClose(); begin(); }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public boolean cancelled() { return cancelled || minecraft == null || minecraft.gui.screen() != this&&!(suspended&&minecraft.gui.screen()==parent); }

    private void selectCode(double mouseX,boolean extend){
        int nearest=0;float best=Float.MAX_VALUE;
        for(int i=0;i<=userCode.length();i++){
            float distance=Math.abs((float)mouseX-codeBounds.x()-VelaDraw.textWidth(userCode.substring(0,i),VelaDraw.Weight.SEMIBOLD,13));
            if(distance<best){nearest=i;best=distance;}
        }
        codeSelection.move(nearest,extend);
    }
    @Override protected boolean dragVela(MouseButtonEvent e,double dx,double dy){if(draggingCode){selectCode(e.x(),true);return true;}return super.dragVela(e,dx,dy);}
    @Override protected boolean releaseVela(MouseButtonEvent e){draggingCode=false;return super.releaseVela(e);}

    @Override
    public void stage(@NotNull String key, @Nullable Object @NotNull ... args) {
        stage = switch (key) {
            case MicrosoftAccount.PROCESSING -> "Microsoft подтвердил вход";
            case MicrosoftAccount.MSA_TO_XBL, MicrosoftAccount.XBL_TO_XSTS -> "Проверяем Xbox-профиль";
            case MicrosoftAccount.XSTS_TO_MCA, MicrosoftAccount.MCA_TO_MCP -> "Проверяем лицензию Minecraft";
            case MicrosoftAccount.ENCRYPTING -> "Шифруем данные аккаунта";
            case MicrosoftAccount.FINALIZING -> "Сохраняем аккаунт";
            default -> "Выполняется вход…";
        };
    }

    @Override
    public void success(@NotNull MicrosoftAccount account) {
        minecraft.execute(() -> {
            if (cancelled()) return;
            try {
                IasIntegration.storeMicrosoft(account, refresh);
                completed=true;
            } catch (RuntimeException cause) {
                error(cause);
            }
        });
    }

    @Override
    public void error(@NotNull Throwable cause) {
        Throwable root = cause;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String detail = root.getMessage();
        error = detail == null || detail.isBlank() ? "Проверьте интернет и повторите попытку" : detail;
    }

    private void authClose() {
        generation++;
        if (auth != null) {
            auth.close();
            auth = null;
        }
    }

    @Override
    public void onClose() {
        cancelled = true;
        authClose();
        closing=true;
    }

    @Override
    public void removed() {
        if (!suspended) {
            cancelled = true;
            authClose();
        }
        super.removed();
    }

    @Override public boolean isPauseScreen() { return false; }
    private static Rect empty() { return new Rect(0, 0, 0, 0); }
    private static String ellipsis(String value, int max) { return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…"; }
}
