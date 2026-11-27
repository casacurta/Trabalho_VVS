package br.edu.ifrs.vvs.excecao;

import br.edu.ifrs.vvs.dto.ErroResposta;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Tratador genérico de exceções não previstas.
 *
 * <p>Preserva exceções HTTP já tratadas pelo Jakarta REST (ex.: corpo JSON malformado)
 * e converte qualquer outra falha em HTTP 500, registrando o erro no log sem expor
 * detalhes internos ao cliente.</p>
 */
@Provider
public class MapeadorErroGenerico implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(MapeadorErroGenerico.class);

    @Override
    public Response toResponse(Throwable excecao) {
        if (excecao instanceof WebApplicationException webExcecao) {
            return webExcecao.getResponse();
        }

        LOG.error("Erro inesperado ao processar a requisição", excecao);
        ErroResposta corpo = new ErroResposta(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Erro interno",
                "Ocorreu um erro inesperado ao processar a requisição.",
                List.of(),
                null,
                Instant.now());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(corpo)
                .build();
    }
}
