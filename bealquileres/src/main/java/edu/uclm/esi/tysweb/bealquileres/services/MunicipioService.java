package edu.uclm.esi.tysweb.bealquileres.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dao.FeatureDao;
import edu.uclm.esi.tysweb.bealquileres.dao.MunicipioDao;
import edu.uclm.esi.tysweb.bealquileres.dto.FeatureResponseDto;
import edu.uclm.esi.tysweb.bealquileres.dto.FeatureDto;
import edu.uclm.esi.tysweb.bealquileres.model.Feature;
import edu.uclm.esi.tysweb.bealquileres.model.Municipio;

@Service
public class MunicipioService {

    private final CartoCiudadClient cartoCiudadClient;
    private final MunicipioDao dao;
    private final FeatureDao featureDao;

    @Autowired 
    private SseService sseService;

    public MunicipioService(CartoCiudadClient cartoCiudadClient, MunicipioDao dao, FeatureDao featureDao) {
        this.cartoCiudadClient = cartoCiudadClient;
        this.dao = dao;
        this.featureDao = featureDao;
    }

    public void load(String name, Integer offset, Integer limit, Boolean progress) {
        List<FeatureDto> features = cartoCiudadClient.load(name, offset, limit);
        Municipio municipio = this.dao.findByName(name);
        if (municipio==null) {
            municipio = new Municipio();
            municipio.setName(name);
            this.dao.save(municipio);
        }

        final int BATCH_SIZE = 500;
        int processed = 0;
        List<Feature> batch = new ArrayList<>(BATCH_SIZE);

        for (FeatureDto dto : features) {
            Feature feature = new Feature();
            feature.setIdeeId(dto.id());
            feature.setLatitude(dto.geometry().coordinates().get(0));
            feature.setLongitude(dto.geometry().coordinates().get(1));
            feature.setMunicipio(municipio);
            feature.setName(dto.properties().componentThoroughfareName());
            feature.setNumber(dto.properties().locatorDesignatorAddressNumber());
            feature.setZipCode(dto.properties().componenPostalDescriptor());

            processed++;
            batch.add(feature);
            if (batch.size()==BATCH_SIZE) {
                this.featureDao.saveAllAndFlush(batch);
                if (progress)
                    this.sseService.send(name, "progress", processed);
                batch.clear();
            }
        }

        if (!batch.isEmpty())
            this.featureDao.saveAllAndFlush(batch);

        if (progress)
            this.sseService.send(name, "completed", processed);

        System.out.println("Terminado: " + processed);
    }

    public Integer getNumeroDeDirecciones(String municipio) {
        return this.cartoCiudadClient.getNumeroDeDirecciones(municipio);
    }
    public void setEstacionDtoRequest(String municipio, String nombre, Integer numero, Integer capacidad) {
        if (capacidad == null || capacidad < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La capacidad debe ser de al menos una bicicleta");
        }
        Municipio m = this.dao.findByName(municipio);
        if (m == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Municipio no encontrado: " + municipio);
        Feature feature = this.featureDao.findByMunicipioAndNameAndNumber(m, nombre, String.valueOf(numero));
        if (feature == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dirección no encontrada: " + nombre + " en " + municipio);

        feature.setCapacidad(capacidad);
        this.featureDao.save(feature);
    }

    public FeatureResponseDto setEstacionDesdeMapa(
            String municipio, Double latitud, Double longitud, Integer capacidad) {
        if (latitud == null || longitud == null || !Double.isFinite(latitud) || !Double.isFinite(longitud)
                || latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las coordenadas no son válidas");
        }
        if (capacidad == null || capacidad < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La capacidad debe ser de al menos una bicicleta");
        }

        Municipio municipioEntity = this.dao.findByName(municipio);
        if (municipioEntity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Municipio no encontrado: " + municipio);
        }

        Feature feature = new Feature();
        feature.setMunicipio(municipioEntity);
        feature.setName("Punto seleccionado en el mapa");
        feature.setLatitude(latitud);
        feature.setLongitude(longitud);
        feature.setCapacidad(capacidad);
        Feature savedFeature = this.featureDao.saveAndFlush(feature);
        return this.toFeatureResponse(savedFeature);
    }
    public List<Feature> getFeatures(String municipio) {
        Municipio m = this.dao.findByName(municipio);
        if (m == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Municipio no encontrado: " + municipio);
        }

        return this.featureDao.findAllByMunicipioOrderByName(m);
    }

    public List<FeatureResponseDto> getEstaciones(String municipio) {
        Municipio m = this.dao.findByName(municipio);
        if (m == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Municipio no encontrado: " + municipio);
        }

        return this.featureDao.findAllByMunicipioAndCapacidadIsNotNullOrderByNameAsc(m).stream()
                .map(this::toFeatureResponse)
                .toList();
    }

    private FeatureResponseDto toFeatureResponse(Feature feature) {
        return new FeatureResponseDto(
                feature.getId(),
                feature.getName(),
                feature.getNumber(),
                feature.getCapacidad(),
                feature.getLatitude(),
                feature.getLongitude());
    }
}
