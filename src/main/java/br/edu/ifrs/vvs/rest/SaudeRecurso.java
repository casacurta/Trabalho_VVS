package br.edu.ifrs.vvs.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.Map;

/**
 * Endpoint de verificação de disponibilidade do serviço.
 * Utilizado pelos testes de sistema para confirmar que a aplicação está pronta
 * antes de exercitar os fluxos de negócio.
 */
@Path("/api/saude")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SaudeRecurso {

    /**
     * @return estado do serviço, nome da aplicação e instante da verificação
     */
    @GET
    public Map<String, Object> verificar() {
        return Map.of(
                "status", "OK",
                "aplicacao", "Trabalho_VVS",
                "momento", Instant.now().toString());
    }
}
