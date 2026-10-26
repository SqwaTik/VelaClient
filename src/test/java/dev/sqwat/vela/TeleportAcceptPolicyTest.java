package dev.sqwat.vela;
import dev.sqwat.vela.module.utility.TeleportAcceptPolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeleportAcceptPolicyTest {
    @Test void acceptsOnlyRecognizedRequests() {
        assertEquals("Friend", TeleportAcceptPolicy.requester("Friend has requested to teleport to you."));
        assertEquals("Friend", TeleportAcceptPolicy.requester("Friend просит телепортироваться к вам."));
        assertNull(TeleportAcceptPolicy.requester("<Enemy> Friend has requested to teleport to you."));
        assertNull(TeleportAcceptPolicy.requester("Friend has requested that you teleport to them."));
        assertNull(TeleportAcceptPolicy.requester("To accept type /tpaccept"));
    }
    @Test void combatAndExpiryNeverDeferTeleportation() {
        var p=new TeleportAcceptPolicy();
        p.request("Friend",true,1000);
        p.combat(1100,30_000);
        assertNull(p.take(1400,false));
        assertNull(p.take(32_000,false));
        p.request("Friend",true,33_000);
        assertEquals("Friend",p.take(33_300,false));
        assertNull(p.take(33_400,false));
        p.request("Friend",true,34_000);
        assertNull(p.take(34_300,false));
        p.request("Other",false,40_000);
        assertNull(p.take(40_300,false));
        p.request("Friend",true,45_000);
        assertNull(p.take(51_000,false));
    }
    @Test void serverCombatSignalBlocksSendAndResetDropsPending() {
        var p=new TeleportAcceptPolicy();p.request("Friend",true,1000);
        assertNull(p.take(1400,true));assertNull(p.take(1500,false));
        p.request("Friend",true,10_000);p.reset();assertNull(p.take(10_500,false));
    }
}
