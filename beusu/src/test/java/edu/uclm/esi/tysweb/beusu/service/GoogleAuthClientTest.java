package edu.uclm.esi.tysweb.beusu.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GoogleAuthClientTest {
    private final GoogleAuthClient client = new GoogleAuthClient();

    @Test
    void createsAuthenticatorProvisioningUrl() {
        String secret = this.client.createCredentials();

        assertTrue(secret.matches("[A-Z2-7]+"));
        String url = this.client.getOtpAuthURL("usuario+test@example.com", secret);
        assertTrue(url.startsWith("otpauth://totp/TySWeb:"));
        assertTrue(url.contains("usuario%2Btest%40example.com"));
        assertTrue(url.contains("?secret=" + secret + "&issuer=TySWeb"));
    }

    @Test
    void rejectsMalformedCodes() {
        assertFalse(this.client.authorize("not-a-secret", "12345"));
        assertFalse(this.client.authorize("not-a-secret", "abcdef"));
    }
}
