package edu.uclm.esi.tysweb.beusu.http;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.validation.Valid;
import edu.uclm.esi.tysweb.beusu.dto.LoginRequest;
import edu.uclm.esi.tysweb.beusu.dto.LoginResponse;
import edu.uclm.esi.tysweb.beusu.dto.RegistroRequest;
import edu.uclm.esi.tysweb.beusu.dto.SessionResponse;
import edu.uclm.esi.tysweb.beusu.service.UsersService;

@RestController 
@RequestMapping ("/users")
@CrossOrigin(origins = "${app.frontend.url:http://localhost:4200}", allowCredentials = "true")
public class Usercontroller {
    @Autowired 
    private UsersService usersService;

    @Value("${app.session.caducidad.minutos:1440}")
    private long minutosCaducidadSesion;

    @Value("${app.session.cookie.secure:false}")
    private boolean secureSessionCookie;

    @PutMapping ("/registerMunicipio")
    public void registerMunicipio(@Valid @RequestBody RegistroRequest body) {
        this.usersService.registerMunicipio(body);
    }

    @GetMapping("/confirmar")
    public String confirmarCuenta(@RequestParam String token) {
        this.usersService.confirmarCuenta(token);
        return "Cuenta confirmada";
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest credenciales, HttpServletResponse response) {
        return this.setSessionCookie(this.usersService.login(credenciales.email(), credenciales.password()), response);
    }

    @PostMapping("/activate2FA")
    public String activate2FA(@Valid @RequestBody LoginRequest body) {
        return this.usersService.activar2FA(body.email(), body.password());
    }

    @PostMapping("/verify2FACode")
    public LoginResponse verify2FACode(@Valid @RequestBody LoginRequest body, HttpServletResponse response) {
        return this.setSessionCookie(this.usersService.confirm2faCode(body), response);
    }

    @GetMapping("/validateSession")
    public SessionResponse validateSession(
            @CookieValue(name = "sessionToken", required = false) String cookieToken,
            @RequestHeader(name = "X-Session-Token", required = false) String headerToken,
            HttpServletResponse response) {
        String token = headerToken != null ? headerToken : cookieToken;
        try {
            return this.usersService.validateSession(token);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                response.addHeader(HttpHeaders.SET_COOKIE, this.expiredSessionCookie().toString());
            }
            throw exception;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "sessionToken", required = false) String cookieToken,
            @RequestHeader(name = "X-Session-Token", required = false) String headerToken) {
        this.usersService.logout(headerToken != null ? headerToken : cookieToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, this.expiredSessionCookie().toString())
                .build();
    }

    private LoginResponse setSessionCookie(LoginResponse login, HttpServletResponse response) {
        if ("AUTHENTICATED".equals(login.status()) && login.token() != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, this.sessionCookie(login.token()).toString());
            return new LoginResponse(login.status(), null, login.activationUrl());
        }
        return login;
    }

    private ResponseCookie sessionCookie(String token) {
        return ResponseCookie.from("sessionToken", token)
                .httpOnly(true)
                .secure(this.secureSessionCookie)
                .path("/")
                .maxAge(Duration.ofMinutes(this.minutosCaducidadSesion))
                .sameSite("Lax")
                .build();
    }

    private ResponseCookie expiredSessionCookie() {
        return ResponseCookie.from("sessionToken", "")
                .httpOnly(true)
                .secure(this.secureSessionCookie)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();
    }
}