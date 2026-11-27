package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifrs.vvs.dto.ErroResposta;
import br.edu.ifrs.vvs.excecao.DadosInvalidosException;
import br.edu.ifrs.vvs.excecao.MapeadorDadosInvalidos;
import br.edu.ifrs.vvs.excecao.MapeadorErroGenerico;
import br.edu.ifrs.vvs.excecao.MapeadorRecursoNaoEncontrado;
import br.edu.ifrs.vvs.excecao.MapeadorRegraDeNegocio;
import br.edu.ifrs.vvs.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.vvs.excecao.RegraDeNegocioException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários dos mapeadores de exceção (tratamento de erros da API).
 *
 * <p><b>Objetivo:</b> garantir que cada tipo de exceção produza o código HTTP correto
 * (400, 404, 409, 500) e que o corpo de erro contenha mensagem e detalhes adequados,
 * incluindo a preservação de respostas já tratadas pelo Jakarta REST.</p>
 */
@DisplayName("Mapeadores de exceção - contrato de erros da API")
class MapeadoresExcecaoTest {

    @Test
    @DisplayName("DadosInvalidosException deve gerar HTTP 400 com a lista de detalhes")
    void deveMapearDadosInvalidosPara400() {
        DadosInvalidosException excecao = new DadosInvalidosException(List.of("erro A", "erro B"));

        Response resposta = new MapeadorDadosInvalidos().toResponse(excecao);

        assertEquals(400, resposta.getStatus());
        ErroResposta corpo = (ErroResposta) resposta.getEntity();
        assertEquals("Erro de validação", corpo.erro());
        assertEquals(2, corpo.detalhes().size());
        assertNotNull(corpo.momento());
    }

    @Test
    @DisplayName("RecursoNaoEncontradoException deve gerar HTTP 404 com a mensagem original")
    void deveMapearNaoEncontradoPara404() {
        RecursoNaoEncontradoException excecao = RecursoNaoEncontradoException.de("Paciente", 99L);

        Response resposta = new MapeadorRecursoNaoEncontrado().toResponse(excecao);

        assertEquals(404, resposta.getStatus());
        ErroResposta corpo = (ErroResposta) resposta.getEntity();
        assertEquals("Recurso não encontrado", corpo.erro());
        assertTrue(corpo.mensagem().contains("Paciente"));
        assertTrue(corpo.detalhes().isEmpty());
    }

    @Test
    @DisplayName("RegraDeNegocioException deve gerar HTTP 409")
    void deveMapearRegraDeNegocioPara409() {
        Response resposta = new MapeadorRegraDeNegocio()
                .toResponse(new RegraDeNegocioException("Conflito de agenda."));

        assertEquals(409, resposta.getStatus());
        assertEquals("Violação de regra de negócio", ((ErroResposta) resposta.getEntity()).erro());
    }

    @Test
    @DisplayName("erro genérico deve gerar HTTP 500 com mensagem neutra, sem vazar detalhes")
    void deveMapearErroGenericoPara500() {
        Response resposta = new MapeadorErroGenerico()
                .toResponse(new IllegalStateException("detalhe interno sensível"));

        assertEquals(500, resposta.getStatus());
        ErroResposta corpo = (ErroResposta) resposta.getEntity();
        assertEquals("Erro interno", corpo.erro());
        assertTrue(!corpo.mensagem().contains("sensível"),
                "A mensagem não deve expor detalhes internos.");
    }

    @Test
    @DisplayName("erro genérico deve preservar respostas HTTP já tratadas (ex.: 400 de JSON malformado)")
    void devePreservarWebApplicationException() {
        Response original = Response.status(Response.Status.BAD_REQUEST).build();

        Response resposta = new MapeadorErroGenerico().toResponse(new BadRequestException(original));

        assertSame(original, resposta);
    }

    @Test
    @DisplayName("exceção de construção de mensagem única deve conter o detalhe informado")
    void deveConstruirDadosInvalidosComDetalheUnico() {
        DadosInvalidosException excecao = new DadosInvalidosException("campo X inválido");

        assertEquals(List.of("campo X inválido"), excecao.getDetalhes());
        assertTrue(excecao.getMessage().contains("campo X inválido"));
    }
}
