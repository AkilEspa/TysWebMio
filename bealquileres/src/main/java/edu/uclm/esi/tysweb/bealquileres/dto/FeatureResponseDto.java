package edu.uclm.esi.tysweb.bealquileres.dto;

public record FeatureResponseDto(
        String id,
        String nombre,
        String numero,
        Integer capacidad,
        Double latitud,
        Double longitud) {
}
