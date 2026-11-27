package br.edu.ifrs.vvs.excecao;

import br.edu.ifrs.vvs.dto.ErroResposta;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import java.util.List;

/**
 * Converte {@link RecursoNaoEncontradoException} em resposta HTTP 404 (Not Found).
 */
@Provider
public class MapeadorRecursoNaoEncontrado implements ExceptionMapper<RecursoNaoEncontradoException> {

    @Override
    public Response toResponse(RecursoNaoEncontradoException excecao) {
        ErroResposta corpo = new ErroResposta(
                Response.Status.NOT_FOUND.getStatusCode(),
                "Recurso não encontrado",
                excecao.getMessage(),
                List.of(),
                null,
                Instant.now());
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(corpo)
                .build();
    }
}
