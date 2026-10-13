package edu.uclm.esi.tysweb.beusu.http;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import edu.uclm.esi.tysweb.beusu.dao.UserDao;
import edu.uclm.esi.tysweb.beusu.dto.RegistroRequest;
import edu.uclm.esi.tysweb.beusu.model.User;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@TestInstance (TestInstance.Lifecycle.PER_CLASS)
public class TestRegister {
    private static final String VALID_REQUEST = """
        {"nombre":"Pepe","apellidos":"Lopez","email":"pepe.lopez@example.com","municipio":"Horcajo de los montes","latitud":"39.32","longitud":"-4.65","password":"Ppe123!A","confirmPassword":"Ppe123!A"}
        """;

    @Autowired
    private MockMvc server;
    @Autowired 
    private UserDao userDao;
    @BeforeAll 
    void borrarDatos() {
        this.userDao.deleteAll();
    }

    @Test
    void testRegistrarNormal() throws Exception {


        String email = "pepe.lopez@example.com";
        RegistroRequest body = new RegistroRequest("Pepe",
        "Lopez", email,
        "Horcajo de los montes", 39.32, -4.65,
        "Ppe123!A", "Ppe123!A");

        RequestBuilder request = MockMvcRequestBuilders.
            put("/users/registerMunicipio")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(body));
        this.server.perform(request)
            .andExpect(status().isOk());
        
        User user = this.userDao.findByEmail(email);
        assert user != null;

        assertNull(user.getHoraConfirmacion());
        assertNotEquals(body.password(), user.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches(body.password(), user.getPassword()));

        assertFalse(user.getActivo());
        assertNotNull(user.getTokenCreacion());
        assertNotNull(user.getHoraCreacion());
        //assertNotNull(user.getHoraConfirmacion());
        

    }
    @Test
    void testRegistrarMunicipioNoExiste() throws Exception {


        String email = "pepe.lopez@conejo.com";
        RegistroRequest body = new RegistroRequest("Pepe",
        "Lopez", email,
        "villaConejo", 39.32, -4.65,
        "Ppe123!A", "Ppe123!A");

        RequestBuilder request = MockMvcRequestBuilders.
            put("/users/registerMunicipio")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(body));
        this.server.perform(request)
            .andExpect(status().is4xxClientError());
        
        User user = this.userDao.findByEmail(email);
        assert user != null;

        assertNull(user.getHoraConfirmacion());
        assertNotEquals(body.password(), user.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches(body.password(), user.getPassword()));

        assertFalse(user.getActivo());
        assertNotNull(user.getTokenCreacion());
        assertNotNull(user.getHoraCreacion());
        //assertNotNull(user.getHoraConfirmacion());
        

    }

    @Test
    void testRechazaEmailInvalido() throws Exception {
        register(VALID_REQUEST.replace("pepe.lopez@example.com", "correo-invalido"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testRechazaCamposObligatoriosAusentes() throws Exception {
        register(VALID_REQUEST.replace("\"nombre\":\"Pepe\",", ""))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testRechazaContrasenasDistintas() throws Exception {
        register(VALID_REQUEST.replace("\"confirmPassword\":\"Ppe123!A\"", "\"confirmPassword\":\"Different123!\""))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testRechazaContrasenaInsegura() throws Exception {
        register(VALID_REQUEST.replace("Ppe123!A", "simple"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testRechazaCoordenadasFueraDeRango() throws Exception {
        register(VALID_REQUEST.replace("\"latitud\":\"39.32\"", "\"latitud\":\"91\""))
            .andExpect(status().isBadRequest());
        register(VALID_REQUEST.replace("\"longitud\":\"-4.65\"", "\"longitud\":\"181\""))
            .andExpect(status().isBadRequest());
    }

    private ResultActions register(String json) throws Exception {
        return this.server.perform(MockMvcRequestBuilders.put("/users/registerMunicipio")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
    }
}
