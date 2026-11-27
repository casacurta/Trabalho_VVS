package br.edu.ifrs.vvs.excecao;

import br.edu.ifrs.vvs.dto.ErroResposta;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import java.util.List;

/**
 * Converte {@link RegraDeNegocioException} em resposta HTTP 409 (Conflict).
 */
@Provider
public class MapeadorRegraDeNegocio implements ExceptionMapper<RegraDeNegocioException> {

    @Override
    public Response toResponse(RegraDeNegocioException excecao) {
        ErroResposta corpo = new ErroResposta(
                Response.Status.CONFLICT.getStatusCode(),
                "Violação de regra de negócio",
                excecao.getMessage(),
                List.of(),
                null,
                Instant.now());
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(corpo)
                .build();
    }
}
