package dev.sqwat.vela.mixin.screen;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.core.DotCommandSuggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
abstract class LocalCommandSuggestionsMixin {
    @Shadow @Final private EditBox input;
    @Shadow @Final private Screen screen;
    @Shadow @Final private List<FormattedCharSequence> commandUsage;
    @Shadow private boolean keepSuggestions,currentParseIsCommand,currentParseIsMessage;
    @Shadow private ParseResults<ClientSuggestionProvider> currentParse;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow public abstract void hide();
    @Shadow public abstract void showSuggestions(boolean narrate);

    @Inject(method="updateCommandInfo",at=@At("HEAD"),cancellable=true)
    private void vela$completeLocal(CallbackInfo ci){
        if(!(screen instanceof ChatScreen)||!input.getValue().startsWith("."))return;
        ci.cancel();
        if(keepSuggestions)return;
        hide();input.setSuggestion(null);commandUsage.clear();currentParse=null;
        currentParseIsCommand=currentParseIsMessage=false;
        pendingSuggestions=CompletableFuture.completedFuture(DotCommandSuggestions.suggest(input.getValue(),input.getCursorPosition(),VelaClient.services().configs().profiles()));
        showSuggestions(false);
    }
}
