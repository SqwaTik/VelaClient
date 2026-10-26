package dev.sqwat.vela;
import dev.sqwat.vela.account.SessionIdentity;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class SessionIdentityTest {
    @Test void sameNameDoesNotSelectDifferentOfflineIdentity(){assertFalse(SessionIdentity.matches(UUID.randomUUID(),"Sqwat09",UUID.randomUUID(),"sqwat09",false));}
    @Test void microsoftIdentityDoesNotMatchOnlyByName(){assertFalse(SessionIdentity.matches(UUID.randomUUID(),"player",UUID.randomUUID(),"player",true));}
    @Test void matchingUuidIsActive(){UUID id=UUID.randomUUID();assertTrue(SessionIdentity.matches(id,"player",id,"player",true));}
}
