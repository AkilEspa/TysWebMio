package edu.uclm.esi.tysweb.bealquileres.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.esi.tysweb.bealquileres.dto.MunicipioRequest;
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
}