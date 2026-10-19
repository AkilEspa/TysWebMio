package edu.uclm.esi.tysweb.beusu.model;

import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.uclm.esi.tysweb.beusu.dto.RegistroRequest;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class User {


    @Id @Column(length=36)
    private String id;
    @Column(nullable = false)
        private String nombre;
        @Column(nullable = false)
        private String apellidos;

        @Column(unique = true, nullable = false)
        private String email;
        @Column(nullable = false)
        private String municipio;
        @Column(nullable = false)
        private Double latitud;
        @Column(nullable = false)
        private Double longitud;
        @Column(nullable = false)
        private String password;
        
        @Column(length=36, nullable = false, unique = true)
        private String tokenCreacion;
        @Column (nullable = false)
        private LocalDateTime horaCreacion;
        private LocalDateTime horaConfirmacion;
        @Column
        private String twoFactorSecret;
        @Column(nullable = false, columnDefinition = "boolean default false")
        private boolean twoFactorEnabled;
        private LocalDateTime horaUltimaVerificacion2FA;
        @Column(length = 36, unique = true)
        private String sessionToken;
        private LocalDateTime horaSessionToken;
        public  User(){
            this.id = java.util.UUID.randomUUID().toString();  
            this.tokenCreacion = java.util.UUID.randomUUID().toString();
            this.horaCreacion = LocalDateTime.now(); 
        }

       /* public User(String nombre, String apellidos, String email, String municipio, String latitud, String longitud, String password) {
            this.nombre = nombre;
            this.apellidos = apellidos;
            this.email = email;
            this.municipio = municipio;
            this.latitud = latitud;
            this.longitud = longitud;
            this.password = password;
        } */
        public User(RegistroRequest body) {
            this();
            this.nombre = body.nombre();
            this.apellidos = body.apellidos();
            this.email = body.email();
            this.municipio = body.municipio();
            this.latitud = body.latitud();
            this.longitud = body.longitud();
            this.setPassword(body.password());
        }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }
    @JsonIgnore 
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = new BCryptPasswordEncoder().encode(password);
    }
    public String getTokenCreacion() {
        return tokenCreacion;
    }
    public void setTokenCreacion(String tokenCreacion) {
        this.tokenCreacion = tokenCreacion;
    }
    public LocalDateTime getHoraCreacion() {
        return horaCreacion;
    }
    public void setHoraCreacion(LocalDateTime horaCreacion) {
        this.horaCreacion = horaCreacion;
    }
    public LocalDateTime getHoraConfirmacion() {
        return horaConfirmacion;
    }
    public void setHoraConfirmacion(LocalDateTime horaConfirmacion) {
        this.horaConfirmacion = horaConfirmacion;
    }
    @JsonIgnore
    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }
    public void setTwoFactorSecret(String twoFactorSecret) {
        this.twoFactorSecret = twoFactorSecret;
    }
    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }
    public LocalDateTime getHoraUltimaVerificacion2FA() {
        return horaUltimaVerificacion2FA;
    }
    public void setHoraUltimaVerificacion2FA(LocalDateTime horaUltimaVerificacion2FA) {
        this.horaUltimaVerificacion2FA = horaUltimaVerificacion2FA;
    }
    @JsonIgnore
    public String getSessionToken() {
        return sessionToken;
    }
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
    public LocalDateTime getHoraSessionToken() {
        return horaSessionToken;
    }
    public void setHoraSessionToken(LocalDateTime horaSessionToken) {
        this.horaSessionToken = horaSessionToken;
    }
    public Boolean getActivo() {
        return this.horaConfirmacion != null;
    }
    public void actualizarDatos(RegistroRequest body) {
        this.nombre = body.nombre();
        this.apellidos = body.apellidos();
        this.municipio = body.municipio();
        this.latitud = body.latitud();
        this.longitud = body.longitud();
        this.setPassword(body.password());
        this.tokenCreacion = java.util.UUID.randomUUID().toString();
        this.horaCreacion = LocalDateTime.now();
    }
}
