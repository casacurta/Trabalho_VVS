package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifrs.vvs.dominio.Consulta;
import br.edu.ifrs.vvs.dominio.Medico;
import br.edu.ifrs.vvs.dominio.Paciente;
import br.edu.ifrs.vvs.dto.ConsultaRequisicao;
import br.edu.ifrs.vvs.dto.ConsultaResposta;
import br.edu.ifrs.vvs.excecao.DadosInvalidosException;
import br.edu.ifrs.vvs.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.vvs.excecao.RegraDeNegocioException;
import br.edu.ifrs.vvs.repositorio.ConsultaRepositorio;
import br.edu.ifrs.vvs.servico.ConsultaServico;
import br.edu.ifrs.vvs.servico.MedicoServico;
import br.edu.ifrs.vvs.servico.PacienteServico;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Testes unitários de {@link ConsultaServico}.
 *
 * <p><b>Objetivo:</b> exercitar as regras de agendamento com dependências isoladas
 * por mocks, verificando:
 * <ul>
 *   <li>conflito de agenda do médico (HTTP 409);</li>
 *   <li>conflito de agenda do paciente (HTTP 409);</li>
 *   <li>médico ou paciente inexistente (HTTP 404);</li>
 *   <li>dados inválidos (HTTP 400);</li>
 *   <li>reagendamento e filtros por paciente/médico.</li>
 * </ul>
 * O relógio é fixo em 01/06/2026 12:00 (America/Sao_Paulo) para evitar flakiness.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConsultaServico - regras de agendamento")
class ConsultaServicoTest {

    private static final Clock RELOGIO_FIXO = Clock.fixed(
            Instant.parse("2026-06-01T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private static final LocalDate DATA_FUTURA = LocalDate.of(2026, 7, 15);

    private static final LocalTime HORARIO = LocalTime.of(10, 0);

    @Mock
    private ConsultaRepositorio repositorio;

    @Mock
    private PacienteServico pacienteServico;

    @Mock
    private MedicoServico medicoServico;

    private ConsultaServico servico;

    private Medico medico;

    private Paciente paciente;

    @BeforeEach
    void preparar() {
        servico = new ConsultaServico(repositorio, pacienteServico, medicoServico, RELOGIO_FIXO);
        medico = new Medico("24016/RS", "Oncologista");
        medico.setIdMedico(1L);
        paciente = new Paciente();
        paciente.setIdPessoa(2L);
        paciente.setNome("Maria");
        when(medicoServico.buscarEntidade(1L)).thenReturn(medico);
        when(pacienteServico.buscarEntidade(2L)).thenReturn(paciente);
    }

    private static ConsultaRequisicao requisicao() {
        return new ConsultaRequisicao(DATA_FUTURA, HORARIO, 1L, 2L);
    }

    @Test
    @DisplayName("deve agendar consulta quando não há conflito")
    void deveAgendarConsulta() {
        when(repositorio.existeConsultaDoMedico(anyLong(), any(), any(), any())).thenReturn(false);
        when(repositorio.existeConsultaDoPaciente(anyLong(), any(), any(), any())).thenReturn(false);
        when(repositorio.salvar(any(Consulta.class))).thenAnswer(invocacao -> {
            Consulta c = invocacao.getArgument(0);
            c.setIdConsulta(100L);
            return c;
        });

        ConsultaResposta resposta = servico.agendar(requisicao());

        assertEquals(100L, resposta.id());
        assertEquals(DATA_FUTURA, resposta.data());
        assertEquals(1L, resposta.idMedico());
        assertEquals(2L, resposta.idPaciente());
        assertEquals("Maria", resposta.nomePaciente());
        verify(repositorio).salvar(any(Consulta.class));
    }

    @Test
    @DisplayName("deve recusar agendamento quando o médico já tem consulta no horário")
    void deveRecusarConflitoDoMedico() {
        when(repositorio.existeConsultaDoMedico(eq(1L), eq(DATA_FUTURA), eq(HORARIO), isNull()))
                .thenReturn(true);

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> servico.agendar(requisicao()));

        assertTrue(excecao.getMessage().contains("médico"));
        verify(repositorio, never()).salvar(any(Consulta.class));
    }

    @Test
    @DisplayName("deve recusar agendamento quando o paciente já tem consulta no horário")
    void deveRecusarConflitoDoPaciente() {
        when(repositorio.existeConsultaDoMedico(anyLong(), any(), any(), any())).thenReturn(false);
        when(repositorio.existeConsultaDoPaciente(eq(2L), eq(DATA_FUTURA), eq(HORARIO), isNull()))
                .thenReturn(true);

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> servico.agendar(requisicao()));

        assertTrue(excecao.getMessage().contains("paciente"));
        verify(repositorio, never()).salvar(any(Consulta.class));
    }

    @Test
    @DisplayName("deve recusar agendamento com dados inválidos antes de acessar o banco")
    void deveRecusarDadosInvalidos() {
        ConsultaRequisicao invalida = new ConsultaRequisicao(DATA_FUTURA, LocalTime.of(22, 0), 1L, 2L);

        assertThrows(DadosInvalidosException.class, () -> servico.agendar(invalida));
        verify(repositorio, never()).salvar(any(Consulta.class));
    }

    @Test
    @DisplayName("deve propagar recurso não encontrado quando o médico não existe")
    void devePropagarMedicoInexistente() {
        when(medicoServico.buscarEntidade(1L))
                .thenThrow(new RecursoNaoEncontradoException("Médico não encontrado."));

        assertThrows(RecursoNaoEncontradoException.class, () -> servico.agendar(requisicao()));
    }

    @Test
    @DisplayName("deve propagar recurso não encontrado quando o paciente não existe")
    void devePropagarPacienteInexistente() {
        when(pacienteServico.buscarEntidade(2L))
                .thenThrow(new RecursoNaoEncontradoException("Paciente não encontrado."));

        assertThrows(RecursoNaoEncontradoException.class, () -> servico.agendar(requisicao()));
    }

    @Test
    @DisplayName("deve reagendar consulta ignorando o próprio registro na checagem de conflito")
    void deveReagendarConsulta() {
        Consulta existente = new Consulta(LocalDate.of(2026, 7, 1), LocalTime.of(9, 0), medico);
        existente.setIdConsulta(50L);
        existente.setPaciente(paciente);
        when(repositorio.buscarPorId(50L)).thenReturn(Optional.of(existente));
        when(repositorio.existeConsultaDoMedico(anyLong(), any(), any(), eq(50L))).thenReturn(false);
        when(repositorio.existeConsultaDoPaciente(anyLong(), any(), any(), eq(50L))).thenReturn(false);
        when(repositorio.atualizar(any(Consulta.class))).thenAnswer(i -> i.getArgument(0));

        ConsultaResposta resposta = servico.reagendar(50L, requisicao());

        assertEquals(50L, resposta.id());
        assertEquals(DATA_FUTURA, resposta.data());
        assertEquals(HORARIO, resposta.horario());
    }

    @Test
    @DisplayName("deve impedir reagendamento de consulta inexistente")
    void deveImpedirReagendamentoInexistente() {
        when(repositorio.buscarPorId(anyLong())).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> servico.reagendar(99L, requisicao()));
    }

    @Test
    @DisplayName("deve cancelar consulta existente")
    void deveCancelarConsulta() {
        Consulta existente = new Consulta(DATA_FUTURA, HORARIO, medico);
        when(repositorio.buscarPorId(7L)).thenReturn(Optional.of(existente));

        servico.cancelar(7L);

        verify(repositorio).excluir(existente);
    }

    @Test
    @DisplayName("deve listar as consultas do paciente validando sua existência")
    void deveListarPorPaciente() {
        Consulta consulta = new Consulta(DATA_FUTURA, HORARIO, medico);
        consulta.setPaciente(paciente);
        when(repositorio.listarPorPaciente(2L)).thenReturn(List.of(consulta));

        assertEquals(1, servico.listarPorPaciente(2L).size());
        verify(pacienteServico).buscarEntidade(2L);
    }

    @Test
    @DisplayName("deve listar as consultas do médico validando sua existência")
    void deveListarPorMedico() {
        Consulta consulta = new Consulta(DATA_FUTURA, HORARIO, medico);
        when(repositorio.listarPorMedico(1L)).thenReturn(List.of(consulta));

        assertEquals(1, servico.listarPorMedico(1L).size());
        verify(medicoServico).buscarEntidade(1L);
    }

    @Test
    @DisplayName("deve listar todas as consultas")
    void deveListarTodas() {
        when(repositorio.listarTodas()).thenReturn(List.of(new Consulta(DATA_FUTURA, HORARIO, medico)));

        assertEquals(1, servico.listarTodas().size());
    }

    @Test
    @DisplayName("deve buscar consulta por identificador")
    void deveBuscarConsultaPorId() {
        Consulta existente = new Consulta(DATA_FUTURA, HORARIO, medico);
        existente.setIdConsulta(30L);
        when(repositorio.buscarPorId(30L)).thenReturn(Optional.of(existente));

        assertEquals(30L, servico.buscarPorId(30L).id());
    }

    @Test
    @DisplayName("deve falhar ao buscar consulta inexistente")
    void deveFalharAoBuscarInexistente() {
        when(repositorio.buscarPorId(31L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> servico.buscarPorId(31L));
    }
}
