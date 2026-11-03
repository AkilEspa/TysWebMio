package edu.uclm.esi.tysweb.bealquileres.http;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.esi.tysweb.bealquileres.services.AlquilerService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/alquileres")
public class AlquilerController {
    public record AlquilerDto(
            Long id,
            Long vehicleId,
            String matricula,
            Integer horas,
            Long importeCentimos,
            java.time.LocalDateTime horaInicio,
            java.time.LocalDateTime horaFin) {}

    public record VehiculoAlquilableDto(Long id, String matricula, Double bateria, String municipio) {}

    public record CrearAlquilerRequest(
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(1) Long vehicleId,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(1) Integer hours,
            @jakarta.validation.constraints.NotBlank String paymentIntentId) {}

    private final AlquilerService alquilerService;

    public AlquilerController(AlquilerService alquilerService) {
        this.alquilerService = alquilerService;
    }

    @GetMapping("/disponibles")
    public List<VehiculoAlquilableDto> getVehiculosAlquilables() {
        return this.alquilerService.getVehiculosAlquilables();
    }

    @GetMapping("/mios")
    public List<AlquilerDto> getAlquileresUsuario(
            @RequestAttribute("authenticatedUserEmail") String userEmail) {
        return this.alquilerService.getAlquileresUsuario(userEmail);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlquilerDto crearAlquiler(
            @Valid @RequestBody CrearAlquilerRequest request,
            @RequestAttribute("authenticatedSessionToken") String sessionToken) {
        return this.alquilerService.crearAlquiler(
                request.vehicleId(),
                request.hours(),
                request.paymentIntentId(),
                sessionToken);
    }
}
