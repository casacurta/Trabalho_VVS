package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifrs.vvs.dto.ConsultaRequisicao;
import br.edu.ifrs.vvs.excecao.DadosInvalidosException;
import br.edu.ifrs.vvs.servico.ValidadorConsulta;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários de {@link ValidadorConsulta}.
 *
 * <p><b>Objetivo:</b> validar as regras de agendamento — obrigatoriedade dos campos,
 * proibição de datas no passado e restrição ao horário de funcionamento (08:00 às 18:00),
 * cobrindo também os limites exatos do expediente.</p>
 */
@DisplayName("ValidadorConsulta - regras de agendamento")
class ValidadorConsultaTest {

    /** Instante fixo de referência: 01/06/2026 às 10:00. */
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 6, 1, 10, 0);

    private static ConsultaRequisicao requisicao(LocalDate data, LocalTime horario) {
        return new ConsultaRequisicao(data, horario, 1L, 1L);
    }

    @Test
    @DisplayName("deve aceitar agendamento válido no futuro dentro do expediente")
    void deveAceitarAgendamentoValido() {
        assertDoesNotThrow(() -> ValidadorConsulta.validar(
                requisicao(AGORA.toLocalDate().plusDays(1), LocalTime.of(9, 0)), AGORA));
    }

    @Test
    @DisplayName("deve recusar médico ausente")
    void deveRecusarMedicoAusente() {
        assertMensagem(new ConsultaRequisicao(AGORA.toLocalDate().plusDays(1),
                LocalTime.of(9, 0), null, 1L), "O médico é obrigatório.");
    }

    @Test
    @DisplayName("deve recusar paciente ausente")
    void deveRecusarPacienteAusente() {
        assertMensagem(new ConsultaRequisicao(AGORA.toLocalDate().plusDays(1),
                LocalTime.of(9, 0), 1L, null), "O paciente é obrigatório.");
    }

    @Test
    @DisplayName("deve recusar data ausente")
    void deveRecusarDataAusente() {
        assertMensagem(new ConsultaRequisicao(null, LocalTime.of(9, 0), 1L, 1L),
                "A data da consulta é obrigatória.");
    }

    @Test
    @DisplayName("deve recusar horário ausente")
    void deveRecusarHorarioAusente() {
        assertMensagem(new ConsultaRequisicao(AGORA.toLocalDate().plusDays(1), null, 1L, 1L),
                "O horário da consulta é obrigatório.");
    }

    @Nested
    @DisplayName("Datas e horários")
    class DatasEHorarios {

        @Test
        @DisplayName("deve recusar agendamento no passado")
        void deveRecusarDataPassada() {
            assertMensagem(requisicao(AGORA.toLocalDate().minusDays(1), LocalTime.of(9, 0)),
                    "Não é permitido agendar consulta para data/horário no passado.");
        }

        @Test
        @DisplayName("deve recusar agendamento no mesmo dia em horário já passado")
        void deveRecusarHorarioPassadoNoMesmoDia() {
            assertMensagem(requisicao(AGORA.toLocalDate(), LocalTime.of(9, 0)),
                    "Não é permitido agendar consulta para data/horário no passado.");
        }

        @Test
        @DisplayName("deve aceitar agendamento exatamente no instante atual (valor de borda)")
        void deveAceitarInstanteAtual() {
            assertDoesNotThrow(() -> ValidadorConsulta.validar(
                    requisicao(AGORA.toLocalDate(), AGORA.toLocalTime()), AGORA));
        }

        @Test
        @DisplayName("deve recusar horário antes da abertura (07:59)")
        void deveRecusarHorarioAntesDaAbertura() {
            assertMensagem(requisicao(AGORA.toLocalDate().plusDays(1), LocalTime.of(7, 59)),
                    "O horário deve estar entre 08:00 e 18:00.");
        }

        @Test
        @DisplayName("deve aceitar horário exatamente na abertura (08:00)")
        void deveAceitarHorarioDeAbertura() {
            assertDoesNotThrow(() -> ValidadorConsulta.validar(
                    requisicao(AGORA.toLocalDate().plusDays(1), ValidadorConsulta.ABERTURA), AGORA));
        }

        @Test
        @DisplayName("deve recusar horário exatamente no fechamento (18:00)")
        void deveRecusarHorarioDeFechamento() {
            assertMensagem(requisicao(AGORA.toLocalDate().plusDays(1), ValidadorConsulta.FECHAMENTO),
                    "O horário deve estar entre 08:00 e 18:00.");
        }

        @Test
        @DisplayName("deve aceitar horário imediatamente antes do fechamento (17:59)")
        void deveAceitarUltimoHorarioValido() {
            assertDoesNotThrow(() -> ValidadorConsulta.validar(
                    requisicao(AGORA.toLocalDate().plusDays(1), LocalTime.of(17, 59)), AGORA));
        }
    }

    @Test
    @DisplayName("deve recusar corpo de requisição nulo")
    void deveRecusarRequisicaoNula() {
        assertEquals(1, ValidadorConsulta.coletarErros(null, AGORA).size());
    }

    @Test
    @DisplayName("deve acumular erros de campos obrigatórios ausentes")
    void deveAcumularErrosDeCamposObrigatorios() {
        DadosInvalidosException excecao = assertThrows(DadosInvalidosException.class,
                () -> ValidadorConsulta.validar(new ConsultaRequisicao(null, null, null, null), AGORA));
        assertEquals(4, excecao.getDetalhes().size());
    }

    private static void assertMensagem(ConsultaRequisicao requisicao, String mensagemEsperada) {
        DadosInvalidosException excecao = assertThrows(DadosInvalidosException.class,
                () -> ValidadorConsulta.validar(requisicao, AGORA));
        assertTrue(excecao.getDetalhes().contains(mensagemEsperada),
                "Mensagens obtidas: " + excecao.getDetalhes());
    }
}
