package dev.sqwat.vela;
import dev.sqwat.vela.render.TextSubmission;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TextSubmissionTest {
 @Test void onlyTrulyEmptyTextIsSkipped(){assertTrue(TextSubmission.empty(FormattedCharSequence.EMPTY));assertTrue(TextSubmission.empty(FormattedCharSequence.forward("",Style.EMPTY)));assertFalse(TextSubmission.empty(FormattedCharSequence.forward(" ",Style.EMPTY)));assertFalse(TextSubmission.empty(FormattedCharSequence.forward("Текст",Style.EMPTY)));}
}
