package edu.uclm.esi.tysweb.beusu.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import edu.uclm.esi.tysweb.beusu.dto.LoginRequest;
import edu.uclm.esi.tysweb.beusu.dto.LoginResponse;
import edu.uclm.esi.tysweb.beusu.dto.RegistroRequest;
import edu.uclm.esi.tysweb.beusu.service.UsersService;

@RestController 
@RequestMapping ("/users")
@CrossOrigin ("*")
public class Usercontroller {
    @Autowired 
    private UsersService usersService;

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
    public LoginResponse login(@Valid @RequestBody LoginRequest credenciales) {
        return this.usersService.login(credenciales.email(), credenciales.password());
    }

    @PostMapping("/activate2FA")
    public String activate2FA(@Valid @RequestBody LoginRequest body) {
        return this.usersService.activar2FA(body.email(), body.password());
    }

    @PostMapping("/verify2FACode")
    public LoginResponse verify2FACode(@Valid @RequestBody LoginRequest body) {
        return this.usersService.confirm2faCode(body);
    }
}