package edu.uclm.esi.tysweb.beusu.dto;

public record LoginResponse(String status, String token, String activationUrl) {
    public LoginResponse(String status, String token) {
        this(status, token, null);
    }

    public LoginResponse(String status) {
        this(status, null, null);
    }
}
