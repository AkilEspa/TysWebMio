package edu.uclm.esi.tysweb.bealquileres.ws;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import edu.uclm.esi.tysweb.bealquileres.dto.VehiculoSimulador;
import edu.uclm.esi.tysweb.bealquileres.services.SimuladorClient;

@Component
public class WS extends TextWebSocketHandler {

    @Autowired 
    private SimuladorClient simuladorClient;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Se ha conectado "+session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {
        String texto=message.getPayload();
        JSONObject json=new JSONObject(texto);
        if(json.getString("tipo").equals("consultar_vehiculos")) {
            String municipio=json.getString("municipio");
            List<VehiculoSimulador> vehiculos=this.simuladorClient.getVehiculos(municipio);
            JSONArray array=new JSONArray(vehiculos);
            WebSocketMessage<?> respuesta=new TextMessage(array.toString());
            session.sendMessage(respuesta);

        }

        WebSocketMessage<?> respuesta=new TextMessage("Te doy la bienvenida");
        session.sendMessage(respuesta);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
            throws Exception {
    }

    @Override 
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status)
            throws Exception {
        System.out.println("Se ha desconectado "+session.getId());
    }
}