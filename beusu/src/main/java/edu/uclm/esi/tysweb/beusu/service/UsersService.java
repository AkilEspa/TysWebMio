package edu.uclm.esi.tysweb.beusu.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
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
        String confirmationUrl = this.frontendPublicUrl + "/?confirmToken=" + user.getTokenCreacion();
        try {
            var message = this.mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(
                message,
                true,
                StandardCharsets.UTF_8.name()
            );
            helper.setFrom(this.mailFrom);
            helper.setTo(user.getEmail());
            helper.setSubject("Confirma tu cuenta del Portal municipal");
            helper.setText(
                this.buildConfirmationEmailText(user, confirmationUrl),
                this.buildConfirmationEmailHtml(user, confirmationUrl)
            );
            this.mailSender.send(message);
        } catch (MailException | MessagingException exception) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "La cuenta se ha guardado, pero no se pudo enviar el correo de confirmación. Comprueba el servicio SMTP y vuelve a intentarlo.",
                exception
            );
        }
    }

    private String buildConfirmationEmailText(User user, String confirmationUrl) {
        return "Hola " + user.getNombre() + ",\n\n"
            + "Gracias por crear una cuenta para " + user.getMunicipio() + ". "
            + "Confirma tu correo para activar el acceso al Portal municipal:\n\n"
            + confirmationUrl + "\n\n"
            + "Este enlace caduca en " + this.minutosCaducidad + " minutos. "
            + "Si no has solicitado este registro, puedes ignorar este mensaje.";
    }

    private String buildConfirmationEmailHtml(User user, String confirmationUrl) {
        String safeName = HtmlUtils.htmlEscape(user.getNombre());
        String safeMunicipio = HtmlUtils.htmlEscape(user.getMunicipio());
        String safeUrl = HtmlUtils.htmlEscape(confirmationUrl);

        return """
            <!doctype html>
            <html lang="es">
              <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Confirma tu cuenta</title>
              </head>
              <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:Arial,Helvetica,sans-serif;color:#172033;">
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#f1f5f9;padding:32px 12px;">
                  <tr>
                    <td align="center">
                      <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="width:100%;max-width:600px;background-color:#ffffff;border-radius:16px;overflow:hidden;">
                        <tr>
                          <td style="background-color:#123c4a;padding:28px 36px;color:#ffffff;">
                            <div style="font-size:13px;letter-spacing:1.5px;text-transform:uppercase;color:#b8e4dc;">Portal municipal</div>
                            <div style="font-size:25px;font-weight:bold;margin-top:8px;">Activa tu cuenta</div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:36px;">
                            <p style="font-size:17px;line-height:1.5;margin:0 0 16px;">Hola {{nombre}},</p>
                            <p style="font-size:15px;line-height:1.7;color:#475569;margin:0 0 24px;">
                              Gracias por registrar <strong>{{municipio}}</strong>. Confirma tu dirección de correo para activar tu cuenta y acceder a las herramientas de gestión municipal.
                            </p>
                            <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 auto 24px;">
                              <tr>
                                <td align="center" style="border-radius:8px;background-color:#087f72;">
                                  <a href="{{url}}" style="display:inline-block;padding:15px 26px;color:#ffffff;text-decoration:none;font-size:15px;font-weight:bold;border-radius:8px;">Confirmar mi correo</a>
                                </td>
                              </tr>
                            </table>
                            <p style="font-size:13px;line-height:1.6;color:#64748b;margin:0 0 8px;">
                              El enlace caduca en {{caducidad}} minutos. Si el botón no funciona, copia y pega este enlace en tu navegador:
                            </p>
                            <p style="font-size:12px;line-height:1.6;word-break:break-all;margin:0;">
                              <a href="{{url}}" style="color:#087f72;">{{url}}</a>
                            </p>
                            <p style="font-size:13px;line-height:1.6;color:#64748b;margin:28px 0 0;">
                              Si no has solicitado este registro, puedes ignorar este mensaje.
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:18px 36px;background-color:#f8fafc;color:#94a3b8;font-size:12px;text-align:center;">
                            Este es un mensaje automático del Portal municipal.
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """
            .replace("{{nombre}}", safeName)
            .replace("{{municipio}}", safeMunicipio)
            .replace("{{url}}", safeUrl)
            .replace("{{caducidad}}", Long.toString(this.minutosCaducidad));
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

        return new SessionResponse(user.getNombre(), user.getEmail(), user.getMunicipio());
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
