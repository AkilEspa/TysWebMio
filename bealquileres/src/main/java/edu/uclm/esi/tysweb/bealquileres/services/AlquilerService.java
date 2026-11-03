package edu.uclm.esi.tysweb.bealquileres.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dao.AlquilerDao;
import edu.uclm.esi.tysweb.bealquileres.dao.VehiculoDao;
import edu.uclm.esi.tysweb.bealquileres.http.AlquilerController.AlquilerDto;
import edu.uclm.esi.tysweb.bealquileres.http.AlquilerController.VehiculoAlquilableDto;
import edu.uclm.esi.tysweb.bealquileres.model.Alquiler;
import edu.uclm.esi.tysweb.bealquileres.model.Vehiculo;

record VerificarPagoAlquilerRequest(String paymentIntentId, Long vehicleId, Integer hours) {}
record VerificarPagoAlquilerResponse(boolean valid, String customerEmail, long amountInCents) {}

@Service
public class AlquilerService {
    private final AlquilerDao alquilerDao;
    private final VehiculoDao vehiculoDao;
    private final RestClient usersClient;

    public AlquilerService(
            AlquilerDao alquilerDao,
            VehiculoDao vehiculoDao,
            @Value("${app.users.url:http://localhost:8080}") String usersServiceUrl) {
        this.alquilerDao = alquilerDao;
        this.vehiculoDao = vehiculoDao;
        this.usersClient = RestClient.builder().baseUrl(usersServiceUrl).build();
    }

    @Transactional(readOnly = true)
    public List<VehiculoAlquilableDto> getVehiculosAlquilables() {
        return this.vehiculoDao.findVehiculosAlquilables(LocalDateTime.now()).stream()
                .map(vehicle -> new VehiculoAlquilableDto(
                        vehicle.getId(),
                        vehicle.getMatricula(),
                        vehicle.getBateria(),
                        vehicle.getMunicipio().getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlquilerDto> getAlquileresUsuario(String emailUsuario) {
        return this.alquilerDao.findByEmailUsuarioOrderByHoraInicioDesc(emailUsuario).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public synchronized AlquilerDto crearAlquiler(
            Long vehicleId, Integer hours, String paymentIntentId, String sessionToken) {
        if (this.alquilerDao.existsByPaymentIntentId(paymentIntentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pago ya está asociado a un alquiler.");
        }

        VerificarPagoAlquilerResponse payment;
        try {
            payment = this.usersClient.post()
                    .uri("/payments/verify-rental")
                    .header("X-Session-Token", sessionToken)
                    .body(new VerificarPagoAlquilerRequest(paymentIntentId, vehicleId, hours))
                    .retrieve()
                    .body(VerificarPagoAlquilerResponse.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo verificar el pago con el servicio de usuarios.",
                    exception);
        }
        if (payment == null || !payment.valid()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "El pago no está confirmado.");
        }

        LocalDateTime now = LocalDateTime.now();
        Vehiculo vehicle = this.vehiculoDao.findVehiculosAlquilables(now).stream()
                .filter(candidate -> candidate.getId().equals(vehicleId))
                .findFirst()
                .orElse(null);
        if (vehicle == null) {
            this.refundPayment(paymentIntentId, vehicleId, hours, sessionToken);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El vehículo ya no está disponible; se ha solicitado el reembolso del pago.");
        }

        Alquiler rental = this.alquilerDao.save(new Alquiler(
                vehicle,
                payment.customerEmail(),
                paymentIntentId,
                hours,
                payment.amountInCents(),
                now));
        return this.toDto(rental);
    }

    private AlquilerDto toDto(Alquiler rental) {
        return new AlquilerDto(
                rental.getId(),
                rental.getVehiculo().getId(),
                rental.getVehiculo().getMatricula(),
                rental.getHoras(),
                rental.getImporteCentimos(),
                rental.getHoraInicio(),
                rental.getHoraFin());
    }

    private void refundPayment(String paymentIntentId, Long vehicleId, Integer hours, String sessionToken) {
        try {
            this.usersClient.post()
                    .uri("/payments/refund-rental")
                    .header("X-Session-Token", sessionToken)
                    .body(new VerificarPagoAlquilerRequest(paymentIntentId, vehicleId, hours))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El vehículo ya no está disponible y no se pudo solicitar el reembolso automáticamente. Contacta con soporte.",
                    exception);
        }
    }
}
