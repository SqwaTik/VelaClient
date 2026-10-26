package dev.sqwat.vela.render.entity;
import net.minecraft.util.FormattedCharSequence;
public final class TextSubmission {
 private TextSubmission(){}
 public static boolean empty(FormattedCharSequence text){return text==FormattedCharSequence.EMPTY||text.accept((index,style,codepoint)->false);}
}
