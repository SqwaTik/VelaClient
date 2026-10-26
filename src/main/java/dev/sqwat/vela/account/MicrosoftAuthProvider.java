package dev.sqwat.vela.account;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Boundary for a Microsoft OAuth device-code implementation owned by the client developer.
 * A real implementation must use its own Entra application id and an OS-protected token vault.
 */
public interface MicrosoftAuthProvider {
    CompletableFuture<AccountProfile> beginDeviceCodeLogin(Consumer<DeviceCode> onCode);

    record DeviceCode(String userCode, String verificationUri, long expiresInSeconds) {}

    static MicrosoftAuthProvider unavailable() {
        return onCode -> CompletableFuture.failedFuture(new IllegalStateException("Microsoft OAuth client id is not configured"));
    }
}
