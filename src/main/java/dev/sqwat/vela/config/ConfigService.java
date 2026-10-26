package dev.sqwat.vela.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleRegistry;
import dev.sqwat.vela.module.setting.Setting;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class ConfigService implements AutoCloseable {
    private static final Gson GSON = new Gson();
    private final Path profilesDirectory;
    private final ModuleRegistry modules;
    private final dev.sqwat.vela.module.ZakoOptimization optimization;
    private final JsonFileStore<String> activeProfileStore;
    private final ScheduledExecutorService writer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "Vela config writer");
        thread.setDaemon(true);
        return thread;
    });
    private ClientConfig config;
    private JsonFileStore<ClientConfig> store;
    private ScheduledFuture<?> pending;
    private String saveError;
    private long saveRevision;
    private List<String> profileCache=List.of();
    private long profileRefreshAt;

    public ConfigService(Path root, ModuleRegistry modules) {
        this(root,modules,null);
    }
    public ConfigService(Path root, ModuleRegistry modules,dev.sqwat.vela.module.ZakoOptimization optimization) {
        this.optimization=optimization;
        this.profilesDirectory = root.resolve("profiles");
        this.modules = Objects.requireNonNull(modules);
        migrateProfileExtensions();
        activeProfileStore = new JsonFileStore<>(root.resolve("active-profile.json"), String.class, () -> "default");
        selectProfile(activeProfileStore.load());
    }

    public synchronized void selectProfile(String rawName) {
        profileRefreshAt=0;
        String name = sanitize(rawName);
        if (pending != null) pending.cancel(false);
        if (store != null && config != null) flushQuietly();
        store = new JsonFileStore<>(profilePath(name), ClientConfig.class, ClientConfig::new);
        config = store.load();
        if(config.favorites==null)config.favorites=new java.util.LinkedHashSet<>();
        if(config.expandedModules==null)config.expandedModules=new java.util.LinkedHashSet<>();
        if(config.expandedModule!=null){config.expandedModules.add(config.expandedModule);config.expandedModule=null;}
        if (config.modules == null) config.modules = new java.util.LinkedHashMap<>();
        if(optimization!=null){
            if(config.optimization==null){
                var legacy=config.modules.get("zako_optimization");
                config.optimization=new java.util.LinkedHashMap<>();
                if(legacy!=null&&legacy.settings!=null)config.optimization.putAll(legacy.settings);
            }
            config.favorites.remove("zako_optimization");
            config.expandedModules.remove("zako_optimization");
            applyOptimization();
        }
        config.uiScale = Float.isFinite(config.uiScale) ? Math.max(0.75f, Math.min(1.25f, config.uiScale)) : 1f;
        config.uiSoundVolume = Float.isFinite(config.uiSoundVolume) ? Math.max(0, Math.min(1, config.uiSoundVolume)) : 0.55f;
        config.activeProfile = name;
        dev.sqwat.vela.ui.render.VelaTheme.setPrimary(dev.sqwat.vela.ui.render.VelaTheme.parseHex(config.accentColor));
        applyToModules();
        try { activeProfileStore.save(name); } catch (IOException error) { saveError = error.getMessage(); }
    }

    public synchronized ClientConfig current() { return config; }
    public Path profilesDirectory(){return profilesDirectory;}
    public Path profilePath(String name){return profilesDirectory.resolve(sanitize(name)+".vela");}
    private void migrateProfileExtensions(){
        if(!java.nio.file.Files.isDirectory(profilesDirectory))return;
        try(var paths=java.nio.file.Files.list(profilesDirectory)){
            for(Path source:paths.filter(java.nio.file.Files::isRegularFile).toList()){
                String name=source.getFileName().toString();
                if(!name.matches("[a-z0-9_-]{1,32}\\.json"))continue;
                Path target=profilePath(name.substring(0,name.length()-5));
                if(!java.nio.file.Files.exists(target))java.nio.file.Files.move(source,target);
                else {
                    Path backup=profilesDirectory.resolve("legacy-backup");
                    java.nio.file.Files.createDirectories(backup);
                    Path destination=backup.resolve(name);
                    for(int index=2;java.nio.file.Files.exists(destination);index++)destination=backup.resolve(name+"."+index);
                    java.nio.file.Files.move(source,destination);
                }
            }
        }catch(IOException error){throw new IllegalStateException("Не удалось обновить формат файлов конфигурации",error);}
    }
    public synchronized void saveToProfile(String target)throws IOException {
        captureModules();ClientConfig snapshot=GSON.fromJson(GSON.toJson(config),ClientConfig.class);
        snapshot.activeProfile=sanitize(target);
        new JsonFileStore<>(profilePath(snapshot.activeProfile),ClientConfig.class,ClientConfig::new).save(snapshot);
        profileRefreshAt=0;
    }

    public synchronized List<String> profiles() {
        long now=System.nanoTime();if(now<profileRefreshAt&&!profileCache.isEmpty())return profileCache;
        Set<String> names = new LinkedHashSet<>();
        try {
            if (java.nio.file.Files.isDirectory(profilesDirectory)) {
                try (var stream = java.nio.file.Files.list(profilesDirectory)) {
                    stream.filter(path -> path.getFileName().toString().endsWith(".vela"))
                        .map(path -> path.getFileName().toString().replaceFirst("\\.vela$", ""))
                        .sorted()
                        .forEach(names::add);
                }
            }
        } catch (IOException error) { saveError=error.getMessage();return profileCache.isEmpty()?List.of(config.activeProfile):profileCache; }
        names.add(config.activeProfile);
        profileCache=List.copyOf(names);profileRefreshAt=now+1_000_000_000L;return profileCache;
    }

    public synchronized String createProfile(String rawName) {
        String name = sanitize(rawName);
        try {saveToProfile(name);}catch(IOException error){throw new IllegalStateException("Не удалось создать профиль",error);}
        selectProfile(name);
        requestSave();
        return name;
    }

    public synchronized String shareProfile(String source)throws IOException {
        flush();
        var saved=new JsonFileStore<>(profilePath(source),ClientConfig.class,ClientConfig::new).load();
        return ConfigShare.encode(saved);
    }
    public synchronized String importProfile(String key)throws IOException {
        ClientConfig imported=ConfigShare.decode(key.strip());
        String name="imported";int index=2;
        while(java.nio.file.Files.exists(profilePath(name)))name="imported-"+index++;
        imported.activeProfile=name;
        new JsonFileStore<>(profilePath(name),ClientConfig.class,ClientConfig::new).save(imported);
        selectProfile(name);return name;
    }
    public synchronized String duplicateProfile(String source)throws IOException {
        flush();String base=sanitize(source),prefix=base.substring(0,Math.min(base.length(),20)),target=prefix+"-copy";int index=2;
        while(java.nio.file.Files.exists(profilePath(target)))target=prefix+"-copy-"+index++;
        java.nio.file.Files.copy(profilePath(base),profilePath(target));profileRefreshAt=0;return target;
    }
    public synchronized String renameProfile(String source,String rawTarget)throws IOException {
        if(rawTarget==null||!rawTarget.matches("[A-Za-z0-9_-]{1,32}"))throw new IOException("Имя: латиница, цифры, _ и -");
        String old=sanitize(source),target=sanitize(rawTarget);
        if(old.equals(target))return target;
        Path from=profilePath(old),to=profilePath(target);
        if(java.nio.file.Files.exists(to))throw new IOException("Такой конфиг уже есть");
        boolean active=old.equals(config.activeProfile);
        if(active)flush();
        java.nio.file.Files.move(from,to);
        if(active){
            try{activeProfileStore.save(target);}
            catch(IOException failure){try{java.nio.file.Files.move(to,from);}catch(IOException rollback){failure.addSuppressed(rollback);}throw failure;}
            config.activeProfile=target;
            store=new JsonFileStore<>(to,ClientConfig.class,ClientConfig::new);
            requestSave();
        }
        profileRefreshAt=0;
        return target;
    }
    public synchronized void archiveProfile(String source)throws IOException {
        String name=sanitize(source);
        if(name.equals(config.activeProfile)){
            flush();String replacement=profiles().stream().filter(p->!p.equals(name)).findFirst().orElse(null);
            if(replacement==null){replacement=name.equals("default")?"new-profile":"default";}
            selectProfile(replacement);flush();
        }
        Path archived=profilesDirectory.resolve("archive");java.nio.file.Files.createDirectories(archived);
        java.nio.file.Files.move(profilePath(name),archived.resolve(name+"-"+System.currentTimeMillis()+".vela"));
        profileRefreshAt=0;
    }
    public synchronized void requestSave() {
        captureModules();
        if (pending != null) pending.cancel(false);
        // The client thread owns live module state. The writer only serializes the captured snapshot.
        ClientConfig snapshot = GSON.fromJson(GSON.toJson(config), ClientConfig.class);
        JsonFileStore<ClientConfig> destination = store;
        long revision = ++saveRevision;
        pending = writer.schedule(() -> {
            synchronized (this) {
                if (revision != saveRevision) return;
                try { destination.save(snapshot); saveError = null; }
                catch (IOException error) { saveError = error.getMessage(); }
            }
        }, 250, TimeUnit.MILLISECONDS);
    }

    public synchronized void flush() throws IOException {
        ++saveRevision;
        if (pending != null) pending.cancel(false);
        if (store == null || config == null) return;
        captureModules();
        store.save(config);
        saveError = null;
    }

    private synchronized void flushQuietly() {
        try { flush(); } catch (IOException error) { saveError = error.getMessage(); }
    }

    public synchronized String saveError() { return saveError; }

    private void captureModules() {
        if(optimization!=null){
            for(var setting:optimization.settings())config.optimization.put(setting.id(),GSON.toJsonTree(setting.value()));
            // Keep a rollback-compatible copy; it is no longer registered or bindable.
            var legacy=config.modules.get("zako_optimization");
            if(legacy!=null)legacy.settings=new java.util.LinkedHashMap<>(config.optimization);
        }
        for (ClientModule module : modules.all()) {
            ClientConfig.ModuleState state = config.modules.computeIfAbsent(module.id(), ignored -> new ClientConfig.ModuleState());
            state.enabled = module.bindMode() != ClientModule.BindMode.HOLD && module.enabled();
            state.keybind = module.keybind();
            state.boundKey = module.boundKey();
            state.bindMode = module.bindMode().name();
            if (state.settings == null) state.settings = new java.util.LinkedHashMap<>();
            for (Setting<?> setting : module.settings()) state.settings.put(setting.id(), GSON.toJsonTree(setting.value()));
        }
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void applyOptimization(){
        for(Setting setting:optimization.settings()){
            var value=config.optimization.get(setting.id());
            setting.reset();
            if(value!=null)try{setting.set(GSON.fromJson(value,setting.defaultValue().getClass()));}
            catch(RuntimeException ignored){}
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void applyToModules() {
        for (ClientModule module : modules.all()) {
            ClientConfig.ModuleState state = config.modules.get(module.id());
            ClientModule.BindMode mode;
            try { mode = ClientModule.BindMode.valueOf(state == null ? null : state.bindMode); }
            catch (RuntimeException invalid) { mode = ClientModule.BindMode.TOGGLE; }
            module.bind(state == null ? -1 : state.boundKey, state == null || state.keybind == null ? "None" : state.keybind, mode);
            // Identical profiles must not tear down terrain, render caches or active effects.
            if (state != null && module.enabled() == state.enabled && settingsMatch(module, state)) continue;
            module.setEnabled(false);
            for (Setting<?> setting : module.settings()) setting.reset();
            if (state == null) continue;
            for (Setting setting : module.settings()) {
                JsonElement value = state.settings == null ? null : state.settings.get(setting.id());
                if (value == null) continue;
                try {
                    Object parsed = GSON.fromJson(value, setting.defaultValue().getClass());
                    setting.set(parsed);
                } catch (RuntimeException ignored) { }
            }
            if(module instanceof dev.sqwat.vela.module.ViewModel viewModel
                &&(state.settings==null||!state.settings.containsKey("off_offset_x")))viewModel.migrateLegacy();
            module.normalizeSettings();
            module.setEnabled(state.enabled);
        }
    }

    private static boolean settingsMatch(ClientModule module, ClientConfig.ModuleState state) {
        // Legacy ViewModel values still require migration even if their stored subset matches.
        if (module instanceof dev.sqwat.vela.module.ViewModel
            && (state.settings == null || !state.settings.containsKey("off_offset_x"))) return false;
        for (Setting<?> setting : module.settings()) {
            JsonElement target = state.settings == null ? null : state.settings.get(setting.id());
            if (target == null) target = GSON.toJsonTree(setting.defaultValue());
            if (!GSON.toJsonTree(setting.value()).equals(target)) return false;
        }
        return true;
    }

    private static String sanitize(String name) {
        String clean = name == null ? "default" : name.strip().toLowerCase().replaceAll("[^a-z0-9_-]", "-");
        return clean.isBlank() ? "default" : clean.substring(0, Math.min(clean.length(), 32));
    }

    @Override public synchronized void close() {
        if (pending != null) pending.cancel(false);
        flushQuietly();
        writer.shutdown();
    }
}
