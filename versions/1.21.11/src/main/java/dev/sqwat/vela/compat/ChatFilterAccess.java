package dev.sqwat.vela.compat;
import net.minecraft.client.GuiMessage;
import java.util.function.Predicate;

/** 26.x ChatComponent.setVisibleMessageFilter, added to 1.21.11's ChatComponent by a compat mixin. */
public interface ChatFilterAccess {
    void vela$setVisibleMessageFilter(Predicate<GuiMessage> filter);
}
