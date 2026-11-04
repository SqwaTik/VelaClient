package dev.sqwat.vela.rules;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Sends the functions switched off in config.yml to every player with Vela. Vela registers the velaclient:disable
 * channel right after joining, and only then can the server write to it, so the list goes out on that registration.
 */
public final class VelaRules extends JavaPlugin implements Listener {
    private List<String> disabled = List.of();
    private String bypass = "vela.rules.bypass";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        load();
        getServer().getMessenger().registerOutgoingPluginChannel(this, DisablePacket.CHANNEL);
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void load() {
        reloadConfig();
        var config = getConfig();
        List<String> ids = new ArrayList<>();
        if (config.getBoolean("all-competitive")) ids.add("*");
        var functions = config.getConfigurationSection("functions");
        if (functions != null) for (String id : functions.getKeys(false)) if (functions.getBoolean(id)) ids.add(id);
        disabled = List.copyOf(ids);
        bypass = config.getString("bypass-permission", "vela.rules.bypass");
        getLogger().info(disabled.isEmpty() ? "No Vela functions are switched off" : "Vela functions switched off: " + String.join(", ", disabled));
    }

    @EventHandler
    public void onRegister(PlayerRegisterChannelEvent event) {
        if (event.getChannel().equals(DisablePacket.CHANNEL)) send(event.getPlayer());
    }

    private void send(Player player) {
        if (disabled.isEmpty() || player.hasPermission(bypass)) return;
        player.sendPluginMessage(this, DisablePacket.CHANNEL, DisablePacket.encode(disabled));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) return false;
        load();
        // Vela only adds to what a server switched off: a function set back to false returns once the player rejoins.
        for (Player player : getServer().getOnlinePlayers()) if (player.getListeningPluginChannels().contains(DisablePacket.CHANNEL)) send(player);
        sender.sendMessage("Vela Rules: " + (disabled.isEmpty() ? "nothing is switched off" : String.join(", ", disabled)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("reload") : List.of();
    }
}
