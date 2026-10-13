package edu.uclm.esi.tysweb.bealquileres.dto;

/**
 * VehiculoSimulador
 */
public record VehiculoSimulador(String matricula, 
    Double latitudOrigen, 
    Double longitudOrigen, 
    Double latitudDestino, 
    Double longitudDestino,
    String nombreOrigen, 
    String nombreDestino,  
    Double longitudActual,
    Double latitudActual, 
    String direccion, 
    Double kmh, 
    Double distanciaRecorrida) {

}
