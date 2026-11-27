package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifrs.vvs.dominio.Paciente;
import br.edu.ifrs.vvs.dominio.Prontuario;
import br.edu.ifrs.vvs.dominio.Situacao;
import br.edu.ifrs.vvs.dominio.Telefone;
import br.edu.ifrs.vvs.dto.PacienteRequisicao;
import br.edu.ifrs.vvs.dto.PacienteResposta;
import br.edu.ifrs.vvs.dto.ProntuarioDTO;
import br.edu.ifrs.vvs.dto.TelefoneDTO;
import br.edu.ifrs.vvs.servico.MapeadorPaciente;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários de {@link MapeadorPaciente}.
 *
 * <p><b>Objetivo:</b> verificar a conversão entre entidade e DTO, o cálculo de idade
 * (incluindo bordas de aniversário) e o tratamento de listas nulas, garantindo que
 * a API nunca receba valores inesperados.</p>
 */
@DisplayName("MapeadorPaciente - conversão entidade/DTO")
class MapeadorPacienteTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 6, 1);

    @Test
    @DisplayName("deve converter requisição em entidade com CPF normalizado e dados aplicados")
    void deveConverterRequisicaoEmEntidade() {
        PacienteRequisicao requisicao = new PacienteRequisicao(
                "  Antonio Carlos  ", "Rua A, 10",
                List.of(new TelefoneDTO(51, 987654321L)), "IPE", "111.444.777-35",
                LocalDate.of(2002, 1, 8), List.of(" a@mail.com ", "  "),
                "HOSPITALIZADO", new ProntuarioDTO(HOJE, "Febre"));
        Paciente paciente = MapeadorPaciente.paraEntidade(requisicao);

        assertEquals("Antonio Carlos", paciente.getNome());
        assertEquals("Rua A, 10", paciente.getEndereco());
        assertEquals("IPE", paciente.getConvenio());
        assertEquals("11144477735", paciente.getCpf());
        assertEquals(Situacao.HOSPITALIZADO, paciente.getSituacao());
        assertEquals(1, paciente.getTelefones().size());
        assertEquals(51, paciente.getTelefones().get(0).getDdd());
        assertEquals(List.of("a@mail.com"), paciente.getEmails());
        assertEquals("Febre", paciente.getProntuario().getDescricao());
    }

    @Test
    @DisplayName("deve usar situação NORMAL quando a situação não é informada")
    void deveUsarSituacaoPadrao() {
        PacienteRequisicao requisicao = new PacienteRequisicao("Maria", null, null, null,
                "11144477735", LocalDate.of(1990, 1, 1), null, null, null);
        assertEquals(Situacao.NORMAL, MapeadorPaciente.paraEntidade(requisicao).getSituacao());
    }

    @Test
    @DisplayName("deve reutilizar o prontuário existente ao aplicar dados (atualização)")
    void deveReutilizarProntuarioNaAtualizacao() {
        Paciente paciente = new Paciente();
        Prontuario existente = new Prontuario(LocalDate.of(2025, 1, 1), "Antigo");
        paciente.setProntuario(existente);
        PacienteRequisicao requisicao = new PacienteRequisicao("Maria", null, null, null,
                "11144477735", LocalDate.of(1990, 1, 1), null, null,
                new ProntuarioDTO(HOJE, "Novo"));

        MapeadorPaciente.aplicarDados(paciente, requisicao);

        assertEquals(existente, paciente.getProntuario());
        assertEquals("Novo", paciente.getProntuario().getDescricao());
        assertEquals(HOJE, paciente.getProntuario().getData());
    }

    @Test
    @DisplayName("deve ignorar telefones e e-mails nulos ao converter")
    void deveIgnorarElementosNulos() {
        List<TelefoneDTO> telefones = new ArrayList<>();
        telefones.add(null);
        telefones.add(new TelefoneDTO(null, 123L));
        telefones.add(new TelefoneDTO(51, null));
        PacienteRequisicao requisicao = new PacienteRequisicao("Maria", null, telefones, null,
                "11144477735", LocalDate.of(1990, 1, 1),
                java.util.Arrays.asList(null, "b@mail.com"), null, null);

        Paciente paciente = MapeadorPaciente.paraEntidade(requisicao);

        assertTrue(paciente.getTelefones().isEmpty());
        assertEquals(List.of("b@mail.com"), paciente.getEmails());
    }

    @Nested
    @DisplayName("Cálculo de idade")
    class Idade {

        @Test
        @DisplayName("deve calcular idade em anos completos")
        void deveCalcularIdade() {
            assertEquals(24, MapeadorPaciente.calcularIdade(LocalDate.of(2002, 1, 8), HOJE));
        }

        @Test
        @DisplayName("não deve contar ano incompleto: um dia antes do aniversário")
        void naoDeveContarAnoIncompleto() {
            assertEquals(23, MapeadorPaciente.calcularIdade(LocalDate.of(2002, 6, 2), HOJE));
        }

        @Test
        @DisplayName("deve contar o ano no dia exato do aniversário (valor de borda)")
        void deveContarNoDiaDoAniversario() {
            assertEquals(24, MapeadorPaciente.calcularIdade(LocalDate.of(2002, 6, 1), HOJE));
        }

        @Test
        @DisplayName("deve retornar nulo quando a data de nascimento é nula")
        void deveRetornarNuloSemData() {
            assertNull(MapeadorPaciente.calcularIdade(null, HOJE));
        }
    }

    @Test
    @DisplayName("deve converter entidade em DTO de resposta com situação e total de consultas")
    void deveConverterEntidadeEmResposta() {
        Paciente paciente = new Paciente();
        paciente.setIdPessoa(7L);
        paciente.setNome("Maria");
        paciente.setCpf("11144477735");
        paciente.setDataNascimento(LocalDate.of(1990, 6, 1));
        paciente.setSituacao(Situacao.EM_OBSERVACAO);
        paciente.adicionarTelefone(new Telefone(51, 99999L));
        paciente.adicionarTelefone(null);
        paciente.setProntuario(new Prontuario(HOJE, "Estável"));

        PacienteResposta resposta = MapeadorPaciente.paraResposta(paciente, HOJE);

        assertEquals(7L, resposta.id());
        assertEquals(36, resposta.idade());
        assertEquals("EM_OBSERVACAO", resposta.situacao());
        assertEquals("Em observação", resposta.situacaoDescricao());
        assertEquals(1, resposta.telefones().size());
        assertEquals("Estável", resposta.prontuario().descricao());
        assertEquals(0, resposta.totalConsultas());
    }

    @Test
    @DisplayName("deve converter entidade vazia sem lançar exceção")
    void deveConverterEntidadeVazia() {
        PacienteResposta resposta = MapeadorPaciente.paraResposta(new Paciente(), HOJE);

        assertNull(resposta.situacao());
        assertNull(resposta.situacaoDescricao());
        assertNull(resposta.prontuario());
        assertNull(resposta.idade());
        assertTrue(resposta.telefones().isEmpty());
        assertTrue(resposta.emails().isEmpty());
    }
}
