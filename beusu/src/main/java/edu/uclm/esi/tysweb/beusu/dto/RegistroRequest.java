package edu.uclm.esi.tysweb.beusu.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * RegistroRequest
 */
public record RegistroRequest(
	@NotBlank String nombre,
	@NotBlank String apellidos,
	@NotBlank @Email String email,
	@NotBlank String municipio,
	@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
	@NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud,
	@NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$") String password,
	@NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$") String confirmPassword) {

	@AssertTrue(message = "Las contraseñas deben coincidir")
	public boolean isPasswordMatch() {
		return password == null || confirmPassword == null || password.equals(confirmPassword);
	}

}
