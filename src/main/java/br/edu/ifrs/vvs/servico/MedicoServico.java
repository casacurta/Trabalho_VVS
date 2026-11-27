package br.edu.ifrs.vvs.servico;

import br.edu.ifrs.vvs.dominio.Medico;
import br.edu.ifrs.vvs.dto.MedicoRequisicao;
import br.edu.ifrs.vvs.dto.MedicoResposta;
import br.edu.ifrs.vvs.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.vvs.excecao.RegraDeNegocioException;
import br.edu.ifrs.vvs.repositorio.ConsultaRepositorio;
import br.edu.ifrs.vvs.repositorio.MedicoRepositorio;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Regras de negócio de médicos.
 */
@ApplicationScoped
public class MedicoServico {

    private final MedicoRepositorio repositorio;

    private final ConsultaRepositorio consultaRepositorio;

    public MedicoServico(MedicoRepositorio repositorio, ConsultaRepositorio consultaRepositorio) {
        this.repositorio = repositorio;
        this.consultaRepositorio = consultaRepositorio;
    }

    /**
     * Cadastra um médico, garantindo que o CRM seja único.
     *
     * @param requisicao dados do médico
     * @return médico criado
     * @throws RegraDeNegocioException quando o CRM já está cadastrado
     */
    @Transactional
    public MedicoResposta cadastrar(MedicoRequisicao requisicao) {
        ValidadorMedico.validar(requisicao);
        String crm = ValidadorCrm.normalizar(requisicao.crm());
        repositorio.buscarPorCrm(crm).ifPresent(existente -> {
            throw new RegraDeNegocioException("Já existe um médico cadastrado com o CRM " + crm + ".");
        });
        Medico medico = repositorio.salvar(MapeadorConsulta.paraEntidade(requisicao));
        return MapeadorConsulta.paraResposta(medico, 0);
    }

    /**
     * Atualiza os dados de um médico.
     *
     * @param id         identificador do médico
     * @param requisicao novos dados
     * @return médico atualizado
     */
    @Transactional
    public MedicoResposta atualizar(Long id, MedicoRequisicao requisicao) {
        ValidadorMedico.validar(requisicao);
        Medico medico = buscarEntidade(id);
        String crm = ValidadorCrm.normalizar(requisicao.crm());
        Optional<Medico> porCrm = repositorio.buscarPorCrm(crm);
        if (porCrm.isPresent() && !porCrm.get().getIdMedico().equals(id)) {
            throw new RegraDeNegocioException("O CRM " + crm + " pertence a outro médico.");
        }
        medico.setCrm(crm);
        medico.setEspecialidade(requisicao.especialidade().trim());
        Medico atualizado = repositorio.atualizar(medico);
        return MapeadorConsulta.paraResposta(atualizado, consultaRepositorio.contarPorMedico(id));
    }

    /**
     * Remove um médico. Médicos com consultas agendadas não podem ser excluídos,
     * pois a integridade da agenda precisa ser preservada.
     *
     * @param id identificador do médico
     * @throws RegraDeNegocioException quando há consultas vinculadas
     */
    @Transactional
    public void excluir(Long id) {
        Medico medico = buscarEntidade(id);
        long consultas = consultaRepositorio.contarPorMedico(id);
        if (consultas > 0) {
            throw new RegraDeNegocioException("O médico possui " + consultas
                    + " consulta(s) agendada(s) e não pode ser excluído.");
        }
        repositorio.excluir(medico);
    }

    /**
     * @param id identificador do médico
     * @return dados do médico
     */
    public MedicoResposta buscarPorId(Long id) {
        Medico medico = buscarEntidade(id);
        return MapeadorConsulta.paraResposta(medico, consultaRepositorio.contarPorMedico(id));
    }

    /**
     * @return todos os médicos cadastrados
     */
    public List<MedicoResposta> listarTodos() {
        return repositorio.listarTodos().stream()
                .map(medico -> MapeadorConsulta.paraResposta(medico,
                        consultaRepositorio.contarPorMedico(medico.getIdMedico())))
                .collect(Collectors.toList());
    }

    /**
     * @param especialidade especialidade pesquisada
     * @return médicos da especialidade
     */
    public List<MedicoResposta> buscarPorEspecialidade(String especialidade) {
        return repositorio.buscarPorEspecialidade(especialidade).stream()
                .map(medico -> MapeadorConsulta.paraResposta(medico,
                        consultaRepositorio.contarPorMedico(medico.getIdMedico())))
                .collect(Collectors.toList());
    }

    /**
     * Obtém a entidade Médico, lançando exceção caso não exista.
     *
     * @param id identificador do médico
     * @return entidade gerenciada
     */
    public Medico buscarEntidade(Long id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Médico", id));
    }
}
