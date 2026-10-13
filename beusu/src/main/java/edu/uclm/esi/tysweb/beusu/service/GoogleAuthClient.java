package edu.uclm.esi.tysweb.beusu.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import com.warrenstrange.googleauth.GoogleAuthenticator;

@Service
public class GoogleAuthClient {
    private static final String ISSUER = "TySWeb";

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    public String createCredentials() {
        return this.gAuth.createCredentials().getKey();
    }

    public String getOtpAuthURL(String email, String key) {
        String label = ISSUER + ":" + URLEncoder.encode(email, StandardCharsets.UTF_8);
        String encodedIssuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label + "?secret=" + key + "&issuer=" + encodedIssuer;
    }

    public boolean authorize(String secret, String code) {
        if (secret == null || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        return this.gAuth.authorize(secret, Integer.parseInt(code));
    }
}
