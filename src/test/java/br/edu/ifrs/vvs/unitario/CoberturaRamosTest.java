package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifrs.vvs.dominio.Consulta;
import br.edu.ifrs.vvs.dominio.Medico;
import br.edu.ifrs.vvs.dominio.Paciente;
import br.edu.ifrs.vvs.dominio.Pessoa;
import br.edu.ifrs.vvs.dominio.Prontuario;
import br.edu.ifrs.vvs.dominio.Situacao;
import br.edu.ifrs.vvs.dominio.Telefone;
import br.edu.ifrs.vvs.dto.ConsultaRequisicao;
import br.edu.ifrs.vvs.dto.MedicoRequisicao;
import br.edu.ifrs.vvs.dto.PacienteRequisicao;
import br.edu.ifrs.vvs.servico.ValidadorConsulta;
import br.edu.ifrs.vvs.servico.ValidadorCpf;
import br.edu.ifrs.vvs.servico.ValidadorMedico;
import br.edu.ifrs.vvs.servico.ValidadorPaciente;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de cobertura de ramificações (branch coverage).
 *
 * <p><b>Objetivo:</b> exercitar os caminhos {@code null}/vazio/limite que não são
 * percorridos pelos testes de regra de negócio, elevando a cobertura de ramos
 * medida pelo JaCoCo. Cada caso corresponde a uma decisão condicional real do
 * código de produção, e não a uma redundância de teste.</p>
 */
@DisplayName("Cobertura de ramos - decisões condicionais")
class CoberturaRamosTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 6, 1);

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 6, 1, 10, 0);

    @Test
    @DisplayName("toString de Paciente com e-mails nulos ou vazios e consultas com elemento nulo")
    void pacienteComColecoesParciais() {
        Paciente paciente = new Paciente();
        paciente.setEmails(List.of());
        assertTrue(paciente.toString().contains("Sem e-mail"));

        paciente.getConsultas().add(null);
        paciente.adicionarConsulta(new Consulta(LocalDate.of(2030, 1, 1),
                LocalTime.of(9, 0), new Medico("1234/DF", "Clínica")));
        assertTrue(paciente.toString().contains("Data"));

        Paciente semSituacao = new Paciente();
        assertTrue(semSituacao.toString().contains("Situação = "));

        Paciente semProntuario = new Paciente();
        semProntuario.setSituacao(Situacao.NORMAL);
        assertFalse(semProntuario.toString().contains("Prontuario ("));
    }

    @Test
    @DisplayName("Pessoa: lista de telefones vazia deve indicar ausência e nomes nulos não quebram")
    void pessoaComListaVazia() {
        Pessoa pessoa = new Pessoa(null, null, null);
        pessoa.setTelefones(List.of());

        String texto = pessoa.toString();

        assertTrue(texto.contains("Sem telefone"));
        assertTrue(texto.contains("nome = null"));
    }

    @Test
    @DisplayName("Consulta: toString com data e médico nulos e com apenas um dos campos")
    void consultaComCamposParciais() {
        Consulta semTudo = new Consulta();
        assertEquals("[Data:  - ]", semTudo.toString());

        Consulta soData = new Consulta(LocalDate.of(2030, 1, 1), null, null);
        assertEquals("[Data: 01/01/2030 - ]", soData.toString());

        Consulta soHora = new Consulta(null, LocalTime.of(10, 0), null);
        assertEquals("[Data:  - 10:00]", soHora.toString());
    }

    @Test
    @DisplayName("Prontuario: toString com data e descrição nulas")
    void prontuarioComCamposNulos() {
        assertEquals("\n\t - Prontuario () = null", new Prontuario().toString());
        assertEquals("\n\t - Prontuario (01/01/2030) = null",
                new Prontuario(LocalDate.of(2030, 1, 1), null).toString());
    }

    @Test
    @DisplayName("Situacao.de: reconhece todas as constantes e ignora caixa/espacos")
    void situacaoReconheceConstantes() {
        assertEquals(Situacao.NORMAL, Situacao.de(" NORMAL "));
        assertEquals(Situacao.EM_OBSERVACAO, Situacao.de("em_observacao"));
        assertEquals(Situacao.HOSPITALIZADO, Situacao.de("Hospitalizado"));
        assertNull(Situacao.de("HOSPITALIZADO2"));
    }

    @Test
    @DisplayName("ValidadorCpf: CPF válido com dígito verificador zero e formatação de entrada inválida")
    void validadorCpfBordas() {
        // 11144477735 já coberto; agora um CPF cujo primeiro dígito calculado é zero
        assertTrue(ValidadorCpf.ehValido("01234567890"));
        assertFalse(ValidadorCpf.ehValido("00000000000"));
        assertEquals("123", ValidadorCpf.formatar("123"));
        assertEquals("111.444.777-35", ValidadorCpf.formatar("111.444.777-35"));
    }

    @Test
    @DisplayName("ValidadorPaciente: telefone válido, lista vazia e nome no limite inferior")
    void validadorPacienteBordas() {
        PacienteRequisicao valido = new PacienteRequisicao("Ana", null,
                List.of(), "SUS", "11144477735", HOJE, List.of(), "", null);
        assertEquals(List.of(), ValidadorPaciente.coletarErros(valido, HOJE));

        PacienteRequisicao semCpf = new PacienteRequisicao("Ana", null, null, null,
                "   ", HOJE, null, null, null);
        assertTrue(ValidadorPaciente.coletarErros(semCpf, HOJE).contains("O CPF do paciente é obrigatório."));

        PacienteRequisicao telefoneNegativo = new PacienteRequisicao("Ana", null,
                List.of(new br.edu.ifrs.vvs.dto.TelefoneDTO(51, -1L)), null,
                "11144477735", HOJE, null, null, null);
        assertTrue(ValidadorPaciente.coletarErros(telefoneNegativo, HOJE).stream()
                .anyMatch(m -> m.startsWith("Número de telefone inválido")));
    }

    @Test
    @DisplayName("ValidadorMedico: CRM em branco e especialidade nula geram mensagens distintas")
    void validadorMedicoBordas() {
        assertTrue(ValidadorMedico.coletarErros(new MedicoRequisicao("  ", "Oncologista"))
                .contains("O CRM do médico é obrigatório."));
        assertTrue(ValidadorMedico.coletarErros(new MedicoRequisicao("24016/RS", null))
                .contains("A especialidade do médico é obrigatória."));
        assertEquals(2, ValidadorMedico.coletarErros(new MedicoRequisicao(null, "  ")).size());
    }

    @Test
    @DisplayName("ValidadorConsulta: apenas data ou apenas horário informados não disparam validação de agenda")
    void validadorConsultaCamposParciais() {
        assertEquals(List.of("O horário da consulta é obrigatório."),
                ValidadorConsulta.coletarErros(new ConsultaRequisicao(HOJE.plusDays(1), null, 1L, 1L), AGORA));
        assertEquals(List.of("A data da consulta é obrigatória."),
                ValidadorConsulta.coletarErros(new ConsultaRequisicao(null, LocalTime.NOON, 1L, 1L), AGORA));
    }

    @Test
    @DisplayName("MapeadorPaciente: atualização de paciente sem prontuário mantém o existente")
    void mapeadorPacienteAtualizacaoSemProntuario() {
        Paciente paciente = new Paciente();
        Prontuario prontuario = new Prontuario(LocalDate.of(2025, 1, 1), "Antigo");
        paciente.setProntuario(prontuario);

        br.edu.ifrs.vvs.servico.MapeadorPaciente.aplicarDados(paciente,
                new PacienteRequisicao("Maria", null, null, null, "11144477735",
                        LocalDate.of(1990, 1, 1), null, null, null));

        assertEquals(prontuario, paciente.getProntuario());
        assertEquals("Antigo", paciente.getProntuario().getDescricao());
    }

    @Test
    @DisplayName("Telefone: criação padrão mantém valores zerados e identificador nulo")
    void telefonePadrao() {
        Telefone telefone = new Telefone();

        assertNull(telefone.getIdTelefone());
        assertEquals(0, telefone.getDdd());
        assertEquals(0L, telefone.getNumero());
    }
}
