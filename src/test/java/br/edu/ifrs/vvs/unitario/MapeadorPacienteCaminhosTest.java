package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários complementares de {@link MapeadorPaciente}.
 *
 * <p><b>Objetivo:</b> exercitar os caminhos alternativos do mapeador que não são
 * cobertos pelo fluxo principal: prontuário sem data, listas nulas, elementos nulos
 * e paciente com coleções vazias. Esses casos garantem que a API responde de forma
 * previsível mesmo com entradas incompletas.</p>
 */
@DisplayName("MapeadorPaciente - caminhos alternativos")
class MapeadorPacienteCaminhosTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 6, 1);

    @Test
    @DisplayName("deve preencher a data do prontuário com hoje quando não informada")
    void devePreencherDataPadraoDoProntuario() {
        PacienteRequisicao requisicao = new PacienteRequisicao("Maria", null, null, null,
                "11144477735", LocalDate.of(1990, 1, 1), null, null,
                new ProntuarioDTO(null, "Sem data informada"));

        Paciente paciente = MapeadorPaciente.paraEntidade(requisicao);

        assertEquals(LocalDate.now(), paciente.getProntuario().getData());
        assertEquals("Sem data informada", paciente.getProntuario().getDescricao());
    }

    @Test
    @DisplayName("deve ignorar prontuário nulo sem criar registro")
    void deveIgnorarProntuarioNulo() {
        PacienteRequisicao requisicao = new PacienteRequisicao("Maria", null, null, null,
                "11144477735", LocalDate.of(1990, 1, 1), null, null, null);

        assertNull(MapeadorPaciente.paraEntidade(requisicao).getProntuario());
    }

    @Test
    @DisplayName("deve criar listas vazias quando telefones e e-mails são nulos")
    void deveCriarListasVazias() {
        PacienteRequisicao requisicao = new PacienteRequisicao("Maria", null, null, null,
                "11144477735", LocalDate.of(1990, 1, 1), null, null, null);

        Paciente paciente = MapeadorPaciente.paraEntidade(requisicao);

        assertNotNull(paciente.getTelefones());
        assertTrue(paciente.getTelefones().isEmpty());
        assertTrue(paciente.getEmails().isEmpty());
    }

    @Test
    @DisplayName("deve descartar telefones com DDD ou número nulo")
    void deveDescartarTelefonesIncompletos() {
        List<TelefoneDTO> telefones = new ArrayList<>();
        telefones.add(new TelefoneDTO(null, 123L));
        telefones.add(new TelefoneDTO(51, null));
        telefones.add(new TelefoneDTO(51, 999L));

        PacienteRequisicao requisicao = new PacienteRequisicao("Maria", null, telefones, null,
                "11144477735", LocalDate.of(1990, 1, 1), null, null, null);

        Paciente paciente = MapeadorPaciente.paraEntidade(requisicao);

        assertEquals(1, paciente.getTelefones().size());
        assertEquals(999L, paciente.getTelefones().get(0).getNumero());
    }

    @Test
    @DisplayName("deve descartar e-mails nulos ou em branco")
    void deveDescartarEmailsVazios() {
        PacienteRequisicao requisicao = new PacienteRequisicao("Maria", null, null, null,
                "11144477735", LocalDate.of(1990, 1, 1),
                Arrays.asList(null, " ", " ok@mail.com "), null, null);

        assertEquals(List.of("ok@mail.com"),
                MapeadorPaciente.paraEntidade(requisicao).getEmails());
    }

    @Test
    @DisplayName("resposta deve tratar paciente sem situação, sem prontuário e sem e-mails")
    void respostaDeveTratarCamposAusentes() {
        Paciente paciente = new Paciente();
        paciente.setTelefones(List.of(new Telefone(11, 1L)));
        paciente.getTelefones().add(null);

        PacienteResposta resposta = MapeadorPaciente.paraResposta(paciente, HOJE);

        assertNull(resposta.situacao());
        assertNull(resposta.situacaoDescricao());
        assertNull(resposta.prontuario());
        assertTrue(resposta.emails().isEmpty());
        assertEquals(1, resposta.telefones().size());
    }

    @Test
    @DisplayName("resposta deve contabilizar consultas e converter prontuário existente")
    void respostaDeveContabilizarConsultas() {
        Paciente paciente = new Paciente();
        paciente.setSituacao(Situacao.NORMAL);
        paciente.setProntuario(new Prontuario(LocalDate.of(2024, 1, 1), "Registro"));
        paciente.adicionarConsulta(new br.edu.ifrs.vvs.dominio.Consulta());
        paciente.adicionarConsulta(new br.edu.ifrs.vvs.dominio.Consulta());

        PacienteResposta resposta = MapeadorPaciente.paraResposta(paciente, HOJE);

        assertEquals(2, resposta.totalConsultas());
        assertEquals("Registro", resposta.prontuario().descricao());
        assertEquals(LocalDate.of(2024, 1, 1), resposta.prontuario().data());
    }
}
