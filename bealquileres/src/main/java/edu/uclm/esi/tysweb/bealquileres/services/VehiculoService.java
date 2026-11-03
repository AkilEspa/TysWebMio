package edu.uclm.esi.tysweb.bealquileres.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dao.MunicipioDao;
import edu.uclm.esi.tysweb.bealquileres.dao.VehiculoDao;
import edu.uclm.esi.tysweb.bealquileres.dto.VehiculoDto;
import edu.uclm.esi.tysweb.bealquileres.model.Municipio;
import edu.uclm.esi.tysweb.bealquileres.model.Vehiculo;

record VerificarPagoCompraRequest(String paymentIntentId, Integer quantity) {}
record VerificarPagoCompraResponse(
        boolean valid, String customerEmail, String municipality, long amountInCents) {}

@Service 
public class VehiculoService {
    private final VehiculoDao dao;
    private final MunicipioDao municipioDao;
    private final RestClient usersClient;

    public VehiculoService(
            VehiculoDao dao,
            MunicipioDao municipioDao,
            @Value("${app.users.url:http://localhost:8080}") String usersServiceUrl) {
        this.dao = dao;
        this.municipioDao = municipioDao;
        this.usersClient = RestClient.builder().baseUrl(usersServiceUrl).build();
    }

    public void createVehicles(Integer numberOfVehicles) {
        List<Vehiculo> vehiculos = new ArrayList<>();
        for (int i=0; i<numberOfVehicles; i++)
            vehiculos.add(new Vehiculo());
        this.dao.saveAll(vehiculos);
    }

    public List<VehiculoDto> getVehiculos() {
        return this.dao.findAll().stream()
            .map(v -> new VehiculoDto(v.getId(), v.getMatricula(),
                v.getLatitud(), v.getLongitud(), v.getDistanciaTotal(), v.getBateria(),
                v.getActivo(), v.getMunicipio()!=null ? v.getMunicipio().getName() : null
                )).toList();
    }
    @Transactional
    public synchronized void asignarVehiculos(Integer cantidad, String municipio) {
        Municipio m = this.municipioDao.findByName(municipio);
        if (m == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Municipio no encontrado: " + municipio);

        
        List<Vehiculo> vehiculosDisponibles = this.dao.getVehiculosDisponibles(cantidad);
        if(vehiculosDisponibles.size() < cantidad)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay suficientes vehículos disponibles. Se han asignado " + vehiculosDisponibles.size() + " vehículos.");
        for(Vehiculo v : vehiculosDisponibles) {
            v.setMunicipio(m);
        }
        this.dao.saveAll(vehiculosDisponibles);
    }

    @Transactional
    public synchronized List<VehiculoDto> comprarVehiculos(
            Integer quantity, String paymentIntentId, String sessionToken) {
        if (quantity == null || quantity < 1 || quantity > 100
                || paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La compra no es válida.");
        }

        VerificarPagoCompraResponse payment;
        try {
            payment = this.usersClient.post()
                    .uri("/payments/verify-purchase")
                    .header("X-Session-Token", sessionToken)
                    .body(new VerificarPagoCompraRequest(paymentIntentId, quantity))
                    .retrieve()
                    .body(VerificarPagoCompraResponse.class);
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión ha caducado.", exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo verificar el pago con el servicio de usuarios.",
                    exception);
        }

        if (payment == null || !payment.valid()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "El pago no está confirmado.");
        }

        List<Vehiculo> purchasedVehicles = this.dao.findByPaymentIntentId(paymentIntentId);
        if (!purchasedVehicles.isEmpty()) {
            boolean samePurchase = purchasedVehicles.size() == quantity
                    && purchasedVehicles.stream()
                            .allMatch(vehicle -> vehicle.getMunicipio() != null
                                    && payment.municipality().equals(vehicle.getMunicipio().getName()));
            if (samePurchase) {
                return purchasedVehicles.stream().map(this::toDto).toList();
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pago ya se ha utilizado.");
        }

        Municipio municipality = this.municipioDao.findByName(payment.municipality());
        if (municipality == null) {
            this.refundPurchase(paymentIntentId, quantity, sessionToken);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El municipio no está disponible para asignar las bicicletas; se ha solicitado el reembolso.");
        }

        List<Vehiculo> availableVehicles = this.dao.getVehiculosDisponibles(quantity);
        if (availableVehicles.size() < quantity) {
            this.refundPurchase(paymentIntentId, quantity, sessionToken);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No hay bicicletas suficientes; se ha solicitado el reembolso del pago.");
        }

        for (Vehiculo vehicle : availableVehicles) {
            vehicle.setMunicipio(municipality);
            vehicle.setPaymentIntentId(paymentIntentId);
        }

        try {
            this.dao.saveAllAndFlush(availableVehicles);
        } catch (RuntimeException exception) {
            this.refundPurchase(paymentIntentId, quantity, sessionToken);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo registrar la compra; se ha solicitado el reembolso.",
                    exception);
        }
        return availableVehicles.stream().map(this::toDto).toList();
    }

    private VehiculoDto toDto(Vehiculo vehicle) {
        return new VehiculoDto(
                vehicle.getId(),
                vehicle.getMatricula(),
                vehicle.getLatitud(),
                vehicle.getLongitud(),
                vehicle.getDistanciaTotal(),
                vehicle.getBateria(),
                vehicle.getActivo(),
                vehicle.getMunicipio() == null ? null : vehicle.getMunicipio().getName());
    }

    private void refundPurchase(String paymentIntentId, Integer quantity, String sessionToken) {
        try {
            this.usersClient.post()
                    .uri("/payments/refund-purchase")
                    .header("X-Session-Token", sessionToken)
                    .body(new VerificarPagoCompraRequest(paymentIntentId, quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "La compra no se pudo completar ni reembolsar automáticamente. Contacta con soporte.",
                    exception);
        }
    }
}
