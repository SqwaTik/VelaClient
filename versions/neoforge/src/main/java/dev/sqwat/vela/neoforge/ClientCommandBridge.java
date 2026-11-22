package dev.sqwat.vela.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.sqwat.vela.neoforge.shim.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import dev.sqwat.vela.neoforge.shim.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

/**
 * Fabric client commands on NeoForge: Vela's commands build a tree for {@link FabricClientCommandSource}, which NeoForge's
 * client dispatcher (CommandSourceStack) can't hold, so they live in a dispatcher of their own. Each of its top-level names
 * goes into NeoForge's dispatcher as a literal plus the rest of the line, which runs (and is completed) on Vela's dispatcher.
 */
final class ClientCommandBridge {
    private static CommandDispatcher<FabricClientCommandSource> own = new CommandDispatcher<>();

    private ClientCommandBridge() {}

    static void register(CommandDispatcher<CommandSourceStack> neo, CommandBuildContext context) {
        own = new CommandDispatcher<>();
        for (var l : ClientCommandRegistrationCallback.EVENT.listeners()) l.register(own, context);
        for (var root : own.getRoot().getChildren()) {
            String name = root.getName();
            neo.register(Commands.literal(name)
                .executes(ctx -> run(name))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> suggest(name, builder))
                    .executes(ctx -> run(name + " " + StringArgumentType.getString(ctx, "args")))));
        }
    }

    private static int run(String line) {
        var source = source();
        try {
            return own.execute(line, source);
        } catch (CommandSyntaxException error) {
            source.sendError(Component.literal(error.getMessage()));
            return 0;
        }
    }

    private static CompletableFuture<Suggestions> suggest(String name, SuggestionsBuilder builder) {
        String line = name + " " + builder.getRemaining();
        int offset = builder.getStart() - (name.length() + 1);
        return own.getCompletionSuggestions(own.parse(line, source()))
            .thenApply(found -> {
                var shifted = new SuggestionsBuilder(builder.getInput(), builder.getStart());
                for (var s : found.getList()) {
                    int start = s.getRange().getStart() + offset;
                    if (start == builder.getStart()) shifted.suggest(s.getText(), s.getTooltip());
                    else shifted.createOffset(start).suggest(s.getText(), s.getTooltip());
                }
                return shifted.build();
            });
    }

    /** The command source: chat output, the player and level; the suggestion questions go to the connection's provider. */
    private static FabricClientCommandSource source() {
        var client = Minecraft.getInstance();
        return (FabricClientCommandSource) Proxy.newProxyInstance(FabricClientCommandSource.class.getClassLoader(),
            new Class<?>[] {FabricClientCommandSource.class}, (proxy, method, args) -> switch (method.getName()) {
                case "sendFeedback" -> { client.gui.hud.getChat().addClientSystemMessage((Component) args[0]); yield null; }
                case "sendError" -> { client.gui.hud.getChat().addClientSystemMessage(((Component) args[0]).copy().withStyle(ChatFormatting.RED)); yield null; }
                case "getClient" -> client;
                case "getPlayer" -> client.player;
                case "getLevel" -> client.level;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "Vela client command source";
                default -> {
                    SharedSuggestionProvider provider = client.getConnection().getSuggestionsProvider();
                    try {
                        yield method.invoke(provider, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
            });
    }
}
