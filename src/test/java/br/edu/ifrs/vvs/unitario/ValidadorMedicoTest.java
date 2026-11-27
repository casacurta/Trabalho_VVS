package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifrs.vvs.dto.MedicoRequisicao;
import br.edu.ifrs.vvs.excecao.DadosInvalidosException;
import br.edu.ifrs.vvs.servico.ValidadorMedico;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários de {@link ValidadorMedico}.
 *
 * <p><b>Objetivo:</b> verificar as regras de CRM e especialidade, incluindo campos
 * obrigatórios, formato do CRM, tamanho mínimo da especialidade e corpo nulo.</p>
 */
@DisplayName("ValidadorMedico - regras de cadastro de médico")
class ValidadorMedicoTest {

    @Test
    @DisplayName("deve aceitar médico válido")
    void deveAceitarMedicoValido() {
        assertDoesNotThrow(() -> ValidadorMedico.validar(new MedicoRequisicao("24016/RS", "Oncologista")));
    }

    @Test
    @DisplayName("deve recusar CRM ausente")
    void deveRecusarCrmAusente() {
        assertMensagem(new MedicoRequisicao(null, "Oncologista"), "O CRM do médico é obrigatório.");
    }

    @Test
    @DisplayName("deve recusar CRM em branco")
    void deveRecusarCrmEmBranco() {
        assertMensagem(new MedicoRequisicao("  ", "Oncologista"), "O CRM do médico é obrigatório.");
    }

    @Test
    @DisplayName("deve recusar CRM em formato inválido")
    void deveRecusarCrmInvalido() {
        assertMensagem(new MedicoRequisicao("24016", "Oncologista"),
                "CRM inválido. Use o formato 12345/UF (ex.: 24016/RS).");
    }

    @Test
    @DisplayName("deve recusar especialidade ausente")
    void deveRecusarEspecialidadeAusente() {
        assertMensagem(new MedicoRequisicao("24016/RS", null),
                "A especialidade do médico é obrigatória.");
    }

    @Test
    @DisplayName("deve recusar especialidade com menos de 3 caracteres (valor de borda)")
    void deveRecusarEspecialidadeCurta() {
        assertMensagem(new MedicoRequisicao("24016/RS", "UT"),
                "A especialidade deve ter ao menos 3 caracteres.");
    }

    @Test
    @DisplayName("deve aceitar especialidade com exatamente 3 caracteres (valor de borda)")
    void deveAceitarEspecialidadeNoLimite() {
        assertDoesNotThrow(() -> ValidadorMedico.validar(new MedicoRequisicao("24016/RS", "UTI")));
    }

    @Test
    @DisplayName("deve recusar corpo de requisição nulo")
    void deveRecusarRequisicaoNula() {
        assertEquals(List.of("O corpo da requisição é obrigatório."),
                ValidadorMedico.coletarErros(null));
    }

    @Test
    @DisplayName("deve acumular erros de CRM e especialidade simultaneamente")
    void deveAcumularErros() {
        DadosInvalidosException excecao = assertThrows(DadosInvalidosException.class,
                () -> ValidadorMedico.validar(new MedicoRequisicao("X", "Y")));
        assertEquals(2, excecao.getDetalhes().size());
    }

    private static void assertMensagem(MedicoRequisicao requisicao, String mensagemEsperada) {
        DadosInvalidosException excecao = assertThrows(DadosInvalidosException.class,
                () -> ValidadorMedico.validar(requisicao));
        assertTrue(excecao.getDetalhes().contains(mensagemEsperada),
                "Mensagens obtidas: " + excecao.getDetalhes());
    }
}
