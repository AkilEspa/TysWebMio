package edu.uclm.esi.tysweb.beusu.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password,
    @Pattern(regexp = "\\d{6}") String code
) {}