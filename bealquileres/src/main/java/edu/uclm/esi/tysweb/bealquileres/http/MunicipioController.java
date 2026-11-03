package edu.uclm.esi.tysweb.bealquileres.http;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import edu.uclm.esi.tysweb.bealquileres.dto.MunicipioRequest;
import edu.uclm.esi.tysweb.bealquileres.dto.FeatureResponseDto;
import edu.uclm.esi.tysweb.bealquileres.model.Feature;
import edu.uclm.esi.tysweb.bealquileres.services.MunicipioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/municipios")
@Tag(
    name = "Municipios",
    description = "Operaciones relacionadas con la gestión de municipios por parte de los ayuntamientos"
)
public class MunicipioController {

    public record SetEstacionMapaRequest(Double latitud, Double longitud, Integer capacidad) {
    }

    private final MunicipioService municipioService;

    public MunicipioController(MunicipioService municipioService) {
        this.municipioService = municipioService;
    }

    @GetMapping("/getNumeroDeDirecciones") @Operation(summary = "Devuelve el número de direcciones del municipio pasado como parámetro")
    public Integer getNumeroDeCalles(@RequestParam @Parameter(description = "Nombre de la ciudad") String municipio) {
        return this.municipioService.getNumeroDeDirecciones(municipio);
    }

    @PostMapping("/load")
    @Operation(summary = "Carga en la base de datos las direcciones del municipio")
    public void load(@RequestParam @Parameter(description = "Índice de la dirección inicial (0, por ejemplo)") Integer offset, 
        @RequestParam  @Parameter(description = "Índice de la dirección final") Integer limit, 
        @RequestParam(required = false) @Parameter(description = "Mostrar o no progreso") Boolean progress, 
        @RequestBody MunicipioRequest request) {
            
        municipioService.load(request.municipio(), offset, limit, progress);
    }
    @PostMapping("/setEstacion")
    public void setEstacion(@RequestBody SetEstacionDtoRequest request){
        this.municipioService.setEstacionDtoRequest(request.municipio(), request.nombre(), request.numero(), request.capacidad());
    }

    @PostMapping("/setEstacionMapa")
    @ResponseStatus(HttpStatus.CREATED)
    public FeatureResponseDto setEstacionMapa(
            @RequestAttribute("authenticatedMunicipality") String municipio,
            @RequestBody SetEstacionMapaRequest request) {
        return this.municipioService.setEstacionDesdeMapa(
                municipio, request.latitud(), request.longitud(), request.capacidad());
    }
    @GetMapping("/getFeatures/{municipio}")
    public List<Feature> getFeatures(@PathVariable String municipio) {
        return this.municipioService.getFeatures(municipio);
    }

    @GetMapping("/getEstaciones/{municipio}")
    public List<FeatureResponseDto> getEstaciones(@PathVariable String municipio) {
        return this.municipioService.getEstaciones(municipio);
    }
}