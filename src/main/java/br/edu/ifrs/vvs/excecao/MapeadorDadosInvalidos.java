package br.edu.ifrs.vvs.excecao;

import br.edu.ifrs.vvs.dto.ErroResposta;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import java.util.List;

/**
 * Converte {@link DadosInvalidosException} em resposta HTTP 400 (Bad Request)
 * com o corpo padronizado {@link ErroResposta}.
 */
@Provider
public class MapeadorDadosInvalidos implements ExceptionMapper<DadosInvalidosException> {

    @Override
    public Response toResponse(DadosInvalidosException excecao) {
        ErroResposta corpo = new ErroResposta(
                Response.Status.BAD_REQUEST.getStatusCode(),
                "Erro de validação",
                "Um ou mais campos enviados são inválidos.",
                excecao.getDetalhes(),
                null,
                Instant.now());
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(corpo)
                .build();
    }
}
