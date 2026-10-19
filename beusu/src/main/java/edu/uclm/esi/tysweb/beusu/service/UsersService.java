package edu.uclm.esi.tysweb.beusu.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.beusu.dao.UserDao;
import edu.uclm.esi.tysweb.beusu.dto.LoginRequest;
import edu.uclm.esi.tysweb.beusu.dto.LoginResponse;
import edu.uclm.esi.tysweb.beusu.dto.RegistroRequest;
import edu.uclm.esi.tysweb.beusu.dto.SessionResponse;
import edu.uclm.esi.tysweb.beusu.model.User;

/**
 * UsersService
 */
@Service 
public class UsersService {
    @Autowired 
    private UserDao userDao;

    @Autowired
    private IdeeClient idee;

    @Autowired
    private GoogleAuthClient googleAuth;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.frontend.public-url}")
    private String frontendPublicUrl;

    @Value("${app.token.caducidad.minutos:30}")
    private long minutosCaducidad;

    @Value("${app.2fa.intervalo-dias:15}")
    private long intervalo2FADias;

    @Value("${app.session.caducidad.minutos:1440}")
    private long minutosCaducidadSesion;

    public void registerMunicipio(RegistroRequest body) {
        Boolean existe = this.idee.existe(body.municipio());
        if (!existe) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El municipio no existe");
        }

        User existente = this.userDao.findByEmail(body.email());
        User user;

        if (existente != null) {
            if (Boolean.TRUE.equals(existente.getActivo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya existe y está activa");
            } else {
                existente.actualizarDatos(body);
                user = existente;
            }
        } else {
            user = new User(body);
        }

        this.userDao.save(user);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(this.mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Confirma tu registro");
        message.setText("Haz clic en el siguiente enlace para confirmar tu cuenta: "
            + this.frontendPublicUrl + "/?confirmToken=" + user.getTokenCreacion());
        try {
            this.mailSender.send(message);
        } catch (MailException exception) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "La cuenta se ha guardado, pero no se pudo enviar el correo de confirmación. Comprueba el servicio SMTP y vuelve a intentarlo.",
                exception
            );
        }
    }

    public void confirmarCuenta(String token) {
        User user = this.userDao.findByTokenCreacion(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Token no existe");
        }
        if (user.getActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cuenta ya confirmada");
        }
        
        if (user.getHoraCreacion().plusMinutes(minutosCaducidad).isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "El token ha caducado. Vuelva a registrarse.");
        }

        user.setHoraConfirmacion(LocalDateTime.now());
        this.userDao.save(user);
    }

    public LoginResponse login(String email, String password) {
        User user = this.validateCredentials(email, password);
        if (!user.isTwoFactorEnabled()) {
            return new LoginResponse("SETUP_REQUIRED", null, this.activar2FA(email));
        }

        LocalDateTime ultimaVerificacion = user.getHoraUltimaVerificacion2FA();
        if (ultimaVerificacion == null
                || !ultimaVerificacion.plusDays(this.intervalo2FADias).isAfter(LocalDateTime.now())) {
            return new LoginResponse("CODE_REQUIRED");
        }

        return this.createSession(user);
    }

    public String activar2FA(String email, String password) {
        this.validateCredentials(email, password);
        return this.activar2FA(email);
    }

    public String activar2FA(String email) {
        User user = this.userDao.findByEmail(email);
        if (user == null || !user.getActivo()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta inexistente o no activa");
        }
        if (user.isTwoFactorEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El 2FA ya está activado");
        }
        if (user.getTwoFactorSecret() == null) {
            user.setTwoFactorSecret(this.googleAuth.createCredentials());
            this.userDao.save(user);
        }
        return this.googleAuth.getOtpAuthURL(email, user.getTwoFactorSecret());
    }

    public LoginResponse confirm2faCode(LoginRequest data) {
        User user = this.validateCredentials(data.email(), data.password());
        String secret = user.getTwoFactorSecret();
        if (secret == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Primero debes configurar el 2FA");
        }
        if (!this.googleAuth.authorize(secret, data.code())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código 2FA incorrecto");
        }

        user.setHoraUltimaVerificacion2FA(LocalDateTime.now());
        if (!user.isTwoFactorEnabled()) {
            user.setTwoFactorEnabled(true);
        }

        this.userDao.save(user);
        return this.createSession(user);
    }

    public SessionResponse validateSession(String sessionToken) {
        User user = sessionToken == null ? null : this.userDao.findBySessionToken(sessionToken);
        if (user == null || user.getHoraSessionToken() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión no es válida");
        }

        if (!user.getHoraSessionToken().plusMinutes(this.minutosCaducidadSesion).isAfter(LocalDateTime.now())) {
            user.setSessionToken(null);
            user.setHoraSessionToken(null);
            this.userDao.save(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión ha caducado");
        }

        return new SessionResponse(user.getNombre());
    }

    public void logout(String sessionToken) {
        User user = sessionToken == null ? null : this.userDao.findBySessionToken(sessionToken);
        if (user != null) {
            user.setSessionToken(null);
            user.setHoraSessionToken(null);
            this.userDao.save(user);
        }
    }

    private LoginResponse createSession(User user) {
        String sessionToken = java.util.UUID.randomUUID().toString();
        user.setSessionToken(sessionToken);
        user.setHoraSessionToken(LocalDateTime.now());
        this.userDao.save(user);
        return new LoginResponse("AUTHENTICATED", sessionToken);
    }

    private User validateCredentials(String email, String password) {
        User user = this.userDao.findByEmail(email);
        if (user == null || !user.getActivo()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas o cuenta no activa");
        }
        
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        if (!encoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }
        return user;
    }
}
