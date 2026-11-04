package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.ui.animation.FrameClock;
import dev.sqwat.vela.ui.animation.Motion;
import dev.sqwat.vela.ui.render.Rect;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class VelaTitleScreen extends VelaScreen {
    private final VelaServices services;
    private final FrameClock clock = new FrameClock();
    private final List<MenuHit> hits = new ArrayList<>();
    private float entrance;
    private final float[] buttonHover=new float[6];
    private long clockSecond=-1;
    private String timeLabel="",dateLabel="";
    private static final java.time.format.DateTimeFormatter TIME=java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    private static final java.time.format.DateTimeFormatter DATE_EN=java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM",java.util.Locale.ENGLISH);
    private static final java.time.format.DateTimeFormatter DATE=java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM",java.util.Locale.forLanguageTag("ru"));

    public VelaTitleScreen(VelaServices services) {
        super(Component.literal("Vela"));
        this.services = services;
    }

    @Override
    public void renderVela(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        VelaTheme.setPrimary(VelaTheme.parseHex(services.configs().current().accentColor));
        float dt = clock.step();
        entrance = Motion.approach(entrance, 1, services.configs().current().reducedMotion ? 60 : 13, dt);
        drawBackground(g);

        int center=width/2;
        int y=height/2-75+Math.round((1-Motion.easeOutCubic(entrance))*5);
        VelaDraw.logo(g,center-14,y-43,28);
        long second=System.currentTimeMillis()/1000;
        if(second!=clockSecond){clockSecond=second;var now=java.time.LocalDateTime.now();timeLabel=now.format(TIME);dateLabel=now.format(dev.sqwat.vela.ui.Lang.english()?DATE_EN:DATE);}
        VelaDraw.centeredText(g,timeLabel,center,y,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,36);
        VelaDraw.centeredText(g,dateLabel,center,y+45,VelaTheme.TEXT_SECONDARY,VelaDraw.Weight.MEDIUM,9);
        hits.clear();
        String[] labels={"Singleplayer","Multiplayer","Accounts","Mods","Settings","Quit"};
        // Five pills in a row; on a narrow window they shrink to fit instead of running off the edges.
        PILL_W=Math.min(Math.max(96,Math.round(maxLabel(labels)+50)),(width-24-PILL_GAP*4)/5);ROW=PILL_W*5+PILL_GAP*4;
        String[] icons={"singleplayer","multiplayer","account","folder","settings","power"};
        Action[] actions={Action.SINGLEPLAYER,Action.MULTIPLAYER,Action.ACCOUNTS,Action.MODS,Action.SETTINGS,Action.QUIT};
        for(int i=0;i<labels.length;i++){
            int slot=i<2?i:i-1;
            Rect rect=i==2?new Rect(width-140,12,128,30):new Rect(center-ROW/2+slot*(PILL_W+PILL_GAP),y+86,PILL_W,PILL_H);
            boolean hovered=rect.contains(mouseX,mouseY)||i==2&&minecraft.gui.screen() instanceof VelaAccountScreen;
            buttonHover[i]=Motion.approach(buttonHover[i],hovered?1:0,services.configs().current().reducedMotion?60:13,dt);
            float h=buttonHover[i];
            if(i==2){
                VelaDraw.roundedRect(g,rect.x(),rect.y(),rect.width(),rect.height(),8,dev.sqwat.vela.ui.render.AccountChrome.selection(false,h));
                dev.sqwat.vela.ui.hud.PlayerAvatars.draw(g,minecraft.getUser().getName(),rect.x()+6,rect.y()+4,22);
                VelaDraw.text(g,VelaDraw.ellipsize(minecraft.getUser().getName(),73,VelaDraw.Weight.MEDIUM,9),rect.x()+33,rect.y()+10,VelaTheme.TEXT,VelaDraw.Weight.MEDIUM,9);
                float accountReveal=minecraft.gui.screen() instanceof VelaAccountScreen account?account.revealProgress():0;
                g.pose().pushMatrix();g.pose().translate(rect.x()+116,rect.y()+15);
                g.pose().rotate(dev.sqwat.vela.ui.render.ControlChrome.disclosureAngle(accountReveal));
                dev.sqwat.vela.ui.render.VelaIcons.draw(g,"chevron",-5,-5,10,VelaTheme.PRIMARY);
                g.pose().popMatrix();
                hits.add(new MenuHit(rect,actions[i]));continue;
            }
            // Labelled pill: the hover is a clear accent border, a tinted fill and a small lift; Quit turns red.
            int accent=actions[i]==Action.QUIT?VelaTheme.DANGER:VelaTheme.PRIMARY;
            float lift=h*2;
            Rect pill=new Rect(rect.x(),Math.round(rect.y()-lift),rect.width(),rect.height());
            if(h>.01f)VelaDraw.roundedRect(g,pill.x()-3,pill.y()-3,pill.width()+6,pill.height()+6,13,(Math.round(h*40)<<24)|(accent&0xFFFFFF));
            VelaDraw.roundedOutline(g,pill.x(),pill.y(),pill.width(),pill.height(),11,VelaTheme.mix(0xFF262E3F,accent,h),VelaTheme.mix(0xE0121722,VelaTheme.mix(0xFF121722,accent,.16f),h));
            dev.sqwat.vela.ui.render.VelaIcons.draw(g,icons[i],pill.x()+14,pill.y()+(pill.height()-14)/2,14,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,accent,h));
            VelaDraw.text(g,labels[i],pill.x()+35,pill.y()+(pill.height()-9)/2f,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.TEXT,h),VelaDraw.Weight.MEDIUM,9);
            hits.add(new MenuHit(rect,actions[i]));
        }
        dev.sqwat.vela.ui.render.UiCursor.hand(hits.stream().anyMatch(hit->hit.rect.contains(mouseX,mouseY))||changelogHit.contains(mouseX,mouseY)||launcherHit.contains(mouseX,mouseY)||updateHit.contains(mouseX,mouseY));
        drawChangelogPill(g,mouseX,mouseY,dt);
        drawLauncherLink(g,mouseX,mouseY,dt);
        drawUpdateCard(g,mouseX,mouseY,dt);
    }
    private static final String LAUNCHER_RELEASE="https://github.com/SqwaTik/VelaLauncher/releases/latest";
    private Rect launcherHit=new Rect(0,0,0,0);
    private float launcherHover;
    /** Tag of the newest launcher release ("v0.1.21"), asked from GitHub once per game; empty until it answers. */
    private static volatile String launcherTag="";
    private static boolean launcherAsked;
    private static void askLauncherTag(){
        if(launcherAsked)return;
        launcherAsked=true;
        Thread.ofVirtual().name("Vela launcher release").start(()->{
            try{
                var http=java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).followRedirects(java.net.http.HttpClient.Redirect.NORMAL).build();
                var response=http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("https://api.github.com/repos/SqwaTik/VelaLauncher/releases/latest"))
                    .timeout(java.time.Duration.ofSeconds(8)).header("Accept","application/vnd.github+json").header("User-Agent","VelaClient").GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());
                if(response.statusCode()!=200)return;
                var tag=com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject().get("tag_name");
                if(tag!=null&&!tag.isJsonNull())launcherTag=tag.getAsString().strip();
            }catch(Exception ignored){}
        });
    }
    /**
     * Bottom-left card for the newest Vela Launcher release on GitHub: the logo, the name with the release tag below,
     * and a download arrow that lights up on hover.
     */
    private void drawLauncherLink(GuiGraphicsExtractor g,int mouseX,int mouseY,float dt){
        askLauncherTag();
        String title="Vela Launcher";
        String tag=launcherTag;
        String sub=tag.isEmpty()?dev.sqwat.vela.ui.Lang.pick("Latest release","Последний релиз"):dev.sqwat.vela.ui.Lang.pick("Latest release · ","Последний релиз · ")+tag;
        float titleW=VelaDraw.textWidth(title,VelaDraw.Weight.SEMIBOLD,9.5f),subW=VelaDraw.textWidth(sub,VelaDraw.Weight.MEDIUM,7.5f);
        int w=(int)Math.ceil(Math.max(titleW,subW))+74,h=40,x=12,y=height-h-10;
        // Narrow windows: stack it above the changelog pill instead of overlapping it.
        if(x+w>changelogHit.x()-6){x=(width-w)/2;y=changelogHit.y()-h-6;}
        launcherHit=new Rect(x,y,w,h);
        float hover=launcherHover=Motion.approach(launcherHover,launcherHit.contains(mouseX,mouseY)?1:0,services.configs().current().reducedMotion?60:13,dt);
        int accent=VelaTheme.PRIMARY;
        int lift=Math.round(hover*2);
        y-=lift;
        if(hover>.01f)VelaDraw.roundedRect(g,x-3,y-3,w+6,h+6,15,(Math.round(hover*36)<<24)|(accent&0xFFFFFF));
        VelaDraw.roundedOutline(g,x,y,w,h,12,VelaTheme.mix(0xFF262E3F,accent,.35f+hover*.5f),VelaTheme.mix(0xE8121722,VelaTheme.mix(0xFF121722,accent,.14f),hover));
        VelaDraw.logo(g,x+9,y+8,24);
        float textX=x+41;
        VelaDraw.text(g,title,textX,y+9,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,9.5f);
        VelaDraw.text(g,sub,textX,y+23,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.TEXT,hover*.6f),VelaDraw.Weight.MEDIUM,7.5f);
        // Download arrow in a round well on the right.
        int ax=x+w-30,ay=y+(h-22)/2;
        VelaDraw.roundedRect(g,ax,ay,22,22,11,VelaTheme.mix(0xFF1A2130,accent,hover*.85f));
        dev.sqwat.vela.ui.render.VelaIcons.draw(g,"download",ax+5,ay+5,12,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,0xFFFFFFFF,hover));
    }
    private Rect updateHit=new Rect(0,0,0,0);
    private float updateHover;
    /** Top-left card when GitHub has a newer Vela release: "New version 0.2.2" and a download arrow (opens the release). */
    private void drawUpdateCard(GuiGraphicsExtractor g,int mouseX,int mouseY,float dt){
        var release=dev.sqwat.vela.core.UpdateCheck.newer();
        if(release==null){updateHit=new Rect(0,0,0,0);return;}
        String title=dev.sqwat.vela.ui.Lang.pick("New version ","Новая версия ")+release.version();
        String sub=dev.sqwat.vela.ui.Lang.pick("Download from GitHub","Скачать с GitHub");
        float titleW=VelaDraw.textWidth(title,VelaDraw.Weight.SEMIBOLD,9),subW=VelaDraw.textWidth(sub,VelaDraw.Weight.MEDIUM,7.5f);
        int w=(int)Math.ceil(Math.max(titleW,subW))+46,h=30,x=12,y=12;
        updateHit=new Rect(x,y,w,h);
        float hover=updateHover=Motion.approach(updateHover,updateHit.contains(mouseX,mouseY)?1:0,services.configs().current().reducedMotion?60:13,dt);
        int accent=VelaTheme.PRIMARY;
        float pulse=services.configs().current().reducedMotion?1:.6f+.4f*(float)Math.sin(System.currentTimeMillis()/1000.0*Math.PI*1.2);
        VelaDraw.roundedRect(g,x-3,y-3,w+6,h+6,11,(Math.round((18+hover*22)*pulse)<<24)|(accent&0xFFFFFF));
        VelaDraw.roundedOutline(g,x,y,w,h,8,VelaTheme.mix(0xFF262E3F,accent,.55f+hover*.45f),VelaTheme.mix(0xE8121722,VelaTheme.mix(0xFF121722,accent,.16f),hover));
        VelaDraw.text(g,title,x+10,y+6,VelaTheme.TEXT,VelaDraw.Weight.SEMIBOLD,9);
        VelaDraw.text(g,sub,x+10,y+18,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.TEXT,hover*.6f),VelaDraw.Weight.MEDIUM,7.5f);
        int ax=x+w-26,ay=y+(h-18)/2;
        VelaDraw.roundedRect(g,ax,ay,18,18,9,VelaTheme.mix(0xFF1A2130,accent,.35f+hover*.6f));
        dev.sqwat.vela.ui.render.VelaIcons.draw(g,"download",ax+4,ay+4,10,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,0xFFFFFFFF,.5f+hover*.5f));
    }
    private Rect changelogHit=new Rect(0,0,0,0);
    private float changelogHover;
    /** "Vela 0.2.0 · What's new" at the bottom; a pulsing dot until the newest release notes were opened. */
    private void drawChangelogPill(GuiGraphicsExtractor g,int mouseX,int mouseY,float dt){
        String label="Vela "+dev.sqwat.vela.ui.Changelog.current()+"  ·  "+dev.sqwat.vela.ui.Lang.pick("What's new","Что нового");
        boolean unseen=ChangelogScreen.unseen();
        int textWidth=(int)Math.ceil(VelaDraw.textWidth(label,VelaDraw.Weight.MEDIUM,8f));
        int w=textWidth+20+(unseen?10:0),h=18,x=(width-w)/2,y=height-26;
        changelogHit=new Rect(x,y,w,h);
        changelogHover=Motion.approach(changelogHover,changelogHit.contains(mouseX,mouseY)?1:0,services.configs().current().reducedMotion?60:13,dt);
        VelaDraw.roundedOutline(g,x,y,w,h,9,VelaTheme.mix(VelaTheme.BORDER,VelaTheme.PRIMARY,changelogHover*.7f),VelaTheme.mix(VelaTheme.SURFACE_DEEP,VelaTheme.SURFACE_HOVER,changelogHover));
        int tx=x+10;
        if(unseen){
            float pulse=.55f+.45f*(float)Math.sin(System.currentTimeMillis()/1000.0*Math.PI*1.4);
            VelaDraw.roundedRect(g,tx-1,y+6,6,6,3,(Math.round(80*pulse)<<24)|(VelaTheme.PRIMARY&0xFFFFFF));
            VelaDraw.roundedRect(g,tx,y+7,4,4,2,VelaTheme.PRIMARY);tx+=10;
        }
        VelaDraw.text(g,label,tx,y+5,VelaTheme.mix(VelaTheme.TEXT_SECONDARY,VelaTheme.TEXT,changelogHover),VelaDraw.Weight.MEDIUM,8f);
    }

    private static final int PILL_H=34,PILL_GAP=10;
    private static final net.minecraft.resources.Identifier GLOW=net.minecraft.resources.Identifier.fromNamespaceAndPath("velaclient","textures/gui/glow.png");
    private int PILL_W=104,ROW=PILL_W*4+PILL_GAP*3;
    private static float maxLabel(String[] labels){float m=0;for(int i=0;i<labels.length;i++)if(i!=2)m=Math.max(m,VelaDraw.textWidth(labels[i],VelaDraw.Weight.MEDIUM,9));return m;}
    /**
     * Deep base with three soft light blobs in the accent colour drifting slowly (one radial-glow quad each), and a
     * vignette that keeps the edges dark.
     */
    private void drawBackground(GuiGraphicsExtractor g) {
        g.fillGradient(0,0,width,height,0xFF0B0F17,0xFF05070B);
        double t=System.currentTimeMillis()/1000.0;
        boolean still=services.configs().current().reducedMotion;
        int accent=VelaTheme.PRIMARY&0xFFFFFF;
        float[][] blobs={{.28f,.30f,.42f,0},{.74f,.62f,.38f,2.1f},{.52f,.92f,.50f,4.2f}};
        for(int b=0;b<blobs.length;b++){
            float phase=blobs[b][3];
            float cx=width*(blobs[b][0]+(still?0:(float)Math.sin(t*.07+phase)*.05f)),cy=height*(blobs[b][1]+(still?0:(float)Math.cos(t*.06+phase)*.05f));
            float radius=Math.max(width,height)*blobs[b][2];
            int tint=b==1?VelaTheme.mix(accent|0xFF000000,0xFF43C7F4,.45f)&0xFFFFFF:accent;
            // One soft radial texture, tinted and scaled: a real round glow in a single quad.
            VelaDraw.blit(g,GLOW,cx-radius,cy-radius,radius*2,radius*2,0,0,256,256,256,256,(b==1?0x30000000:0x3C000000)|tint);
        }
        // Vignette
        g.fillGradient(0,0,width,height/3,0xB005070B,0x0005070B);
        g.fillGradient(0,height*2/3,width,height,0x0005070B,0xC005070B);
    }

    @Override
    public boolean clickVela(MouseButtonEvent event, boolean doubled) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.clickVela(event, doubled);
        if(changelogHit.contains(event.x(),event.y())){minecraft.gui.setScreen(new ChangelogScreen(this));return true;}
        if(launcherHit.contains(event.x(),event.y())){net.minecraft.util.Util.getPlatform().openUri(LAUNCHER_RELEASE);return true;}
        var release=dev.sqwat.vela.core.UpdateCheck.newer();
        if(release!=null&&updateHit.contains(event.x(),event.y())){net.minecraft.util.Util.getPlatform().openUri(release.url());return true;}
        for (MenuHit hit : hits) {
            if (!hit.rect().contains(event.x(), event.y())) continue;
            switch (hit.action()) {
                case SINGLEPLAYER -> minecraft.gui.setScreen(new SelectWorldScreen(this));
                case MULTIPLAYER -> minecraft.gui.setScreen(new JoinMultiplayerScreen(this));
                case ACCOUNTS -> minecraft.gui.setScreen(new VelaAccountScreen(this, services));
                case MODS -> minecraft.gui.setScreen(com.terraformersmc.modmenu.api.ModMenuApi.createModsScreen(this));
                case SETTINGS -> minecraft.gui.setScreen(new OptionsScreen(this, minecraft.options, false));
                case QUIT -> minecraft.gui.setScreen(new FarewellScreen());
            }
            return true;
        }
        return true;
    }

    @Override protected float interfaceScale(){return services.configs().current().uiScale;}
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    private enum Action { SINGLEPLAYER, MULTIPLAYER, ACCOUNTS, MODS, SETTINGS, QUIT }
    private record MenuHit(Rect rect, Action action) {}
}
