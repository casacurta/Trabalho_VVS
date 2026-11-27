package br.edu.ifrs.vvs.servico;

import br.edu.ifrs.vvs.dominio.Consulta;
import br.edu.ifrs.vvs.dominio.Medico;
import br.edu.ifrs.vvs.dominio.Paciente;
import br.edu.ifrs.vvs.dto.ConsultaRequisicao;
import br.edu.ifrs.vvs.dto.ConsultaResposta;
import br.edu.ifrs.vvs.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.vvs.excecao.RegraDeNegocioException;
import br.edu.ifrs.vvs.repositorio.ConsultaRepositorio;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Regras de negócio de agendamento de consultas.
 *
 * <p>Além da validação de formato, garante duas regras de integridade da agenda:
 * o médico não pode ter duas consultas no mesmo dia/horário e o paciente também não.</p>
 */
@ApplicationScoped
public class ConsultaServico {

    private final ConsultaRepositorio repositorio;

    private final PacienteServico pacienteServico;

    private final MedicoServico medicoServico;

    private final Clock relogio;

    public ConsultaServico(ConsultaRepositorio repositorio, PacienteServico pacienteServico,
                           MedicoServico medicoServico, Clock relogio) {
        this.repositorio = repositorio;
        this.pacienteServico = pacienteServico;
        this.medicoServico = medicoServico;
        this.relogio = relogio;
    }

    /**
     * Agenda uma nova consulta.
     *
     * @param requisicao dados da consulta
     * @return consulta criada
     * @throws br.edu.ifrs.vvs.excecao.DadosInvalidosException dados inválidos
     * @throws RecursoNaoEncontradoException                    médico ou paciente inexistente
     * @throws RegraDeNegocioException                          conflito de agenda
     */
    @Transactional
    public ConsultaResposta agendar(ConsultaRequisicao requisicao) {
        ValidadorConsulta.validar(requisicao, LocalDateTime.now(relogio));
        Medico medico = medicoServico.buscarEntidade(requisicao.idMedico());
        Paciente paciente = pacienteServico.buscarEntidade(requisicao.idPaciente());
        verificarConflito(requisicao, null);
        Consulta consulta = MapeadorConsulta.paraEntidade(requisicao, medico, paciente);
        return MapeadorConsulta.paraResposta(repositorio.salvar(consulta));
    }

    /**
     * Reagenda uma consulta existente.
     *
     * @param id         identificador da consulta
     * @param requisicao novos dados
     * @return consulta atualizada
     */
    @Transactional
    public ConsultaResposta reagendar(Long id, ConsultaRequisicao requisicao) {
        ValidadorConsulta.validar(requisicao, LocalDateTime.now(relogio));
        Consulta consulta = buscarEntidade(id);
        Medico medico = medicoServico.buscarEntidade(requisicao.idMedico());
        Paciente paciente = pacienteServico.buscarEntidade(requisicao.idPaciente());
        verificarConflito(requisicao, id);
        consulta.setData(requisicao.data());
        consulta.setHorario(requisicao.horario());
        consulta.setMedico(medico);
        consulta.setPaciente(paciente);
        return MapeadorConsulta.paraResposta(repositorio.atualizar(consulta));
    }

    /**
     * Cancela (exclui) uma consulta.
     *
     * @param id identificador da consulta
     */
    @Transactional
    public void cancelar(Long id) {
        repositorio.excluir(buscarEntidade(id));
    }

    /**
     * @param id identificador da consulta
     * @return dados da consulta
     */
    public ConsultaResposta buscarPorId(Long id) {
        return MapeadorConsulta.paraResposta(buscarEntidade(id));
    }

    /**
     * @return todas as consultas
     */
    public List<ConsultaResposta> listarTodas() {
        return conversao(repositorio.listarTodas());
    }

    /**
     * @param pacienteId identificador do paciente
     * @return consultas do paciente
     */
    public List<ConsultaResposta> listarPorPaciente(Long pacienteId) {
        pacienteServico.buscarEntidade(pacienteId);
        return conversao(repositorio.listarPorPaciente(pacienteId));
    }

    /**
     * @param medicoId identificador do médico
     * @return consultas do médico
     */
    public List<ConsultaResposta> listarPorMedico(Long medicoId) {
        medicoServico.buscarEntidade(medicoId);
        return conversao(repositorio.listarPorMedico(medicoId));
    }

    /**
     * Obtém a entidade Consulta, lançando exceção caso não exista.
     *
     * @param id identificador da consulta
     * @return entidade gerenciada
     */
    public Consulta buscarEntidade(Long id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Consulta", id));
    }

    private void verificarConflito(ConsultaRequisicao requisicao, Long ignorarId) {
        boolean conflitoMedico = repositorio.existeConsultaDoMedico(
                requisicao.idMedico(), requisicao.data(), requisicao.horario(), ignorarId);
        if (conflitoMedico) {
            throw new RegraDeNegocioException("O médico já possui consulta agendada para "
                    + requisicao.data() + " às " + requisicao.horario() + ".");
        }
        boolean conflitoPaciente = repositorio.existeConsultaDoPaciente(
                requisicao.idPaciente(), requisicao.data(), requisicao.horario(), ignorarId);
        if (conflitoPaciente) {
            throw new RegraDeNegocioException("O paciente já possui consulta agendada para "
                    + requisicao.data() + " às " + requisicao.horario() + ".");
        }
    }

    private List<ConsultaResposta> conversao(List<Consulta> consultas) {
        return consultas.stream()
                .map(MapeadorConsulta::paraResposta)
                .collect(Collectors.toList());
    }
}
