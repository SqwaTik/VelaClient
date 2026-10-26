package dev.sqwat.vela.account;
import java.util.UUID;
public final class SessionIdentity {
    private SessionIdentity(){}
    public static boolean matches(UUID sessionId,String sessionName,UUID savedId,String savedName,boolean microsoft){
        return sessionId!=null?sessionId.equals(savedId):!microsoft&&sessionName!=null&&sessionName.equalsIgnoreCase(savedName);
    }
}
