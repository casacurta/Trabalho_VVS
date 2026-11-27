package br.edu.ifrs.vvs.servico;

import br.edu.ifrs.vvs.dominio.Paciente;
import br.edu.ifrs.vvs.dto.PacienteRequisicao;
import br.edu.ifrs.vvs.dto.PacienteResposta;
import br.edu.ifrs.vvs.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.vvs.excecao.RegraDeNegocioException;
import br.edu.ifrs.vvs.repositorio.PacienteRepositorio;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Regras de negócio de pacientes: cadastro, consulta, atualização e exclusão.
 *
 * <p>A camada de serviço é responsável por transações, validação de regras que
 * dependem do banco (unicidade de CPF) e conversão para DTOs.</p>
 */
@ApplicationScoped
public class PacienteServico {

    private final PacienteRepositorio repositorio;

    private final Clock relogio;

    public PacienteServico(PacienteRepositorio repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    /**
     * Cadastra um novo paciente após validar os dados e a unicidade do CPF.
     *
     * @param requisicao dados do paciente
     * @return paciente criado
     * @throws br.edu.ifrs.vvs.excecao.DadosInvalidosException quando os dados são inválidos
     * @throws RegraDeNegocioException                            quando o CPF já está cadastrado
     */
    @Transactional
    public PacienteResposta cadastrar(PacienteRequisicao requisicao) {
        ValidadorPaciente.validar(requisicao, hoje());
        String cpfNormalizado = ValidadorCpf.normalizar(requisicao.cpf());
        repositorio.buscarPorCpf(cpfNormalizado).ifPresent(existente -> {
            throw new RegraDeNegocioException(
                    "Já existe um paciente cadastrado com o CPF " + ValidadorCpf.formatar(cpfNormalizado) + ".");
        });
        Paciente paciente = MapeadorPaciente.paraEntidade(requisicao);
        return MapeadorPaciente.paraResposta(repositorio.salvar(paciente), hoje());
    }

    /**
     * Atualiza um paciente existente.
     *
     * @param id         identificador do paciente
     * @param requisicao novos dados do paciente
     * @return paciente atualizado
     * @throws RecursoNaoEncontradoException quando o paciente não existe
     */
    @Transactional
    public PacienteResposta atualizar(Long id, PacienteRequisicao requisicao) {
        ValidadorPaciente.validar(requisicao, hoje());
        Paciente paciente = buscarEntidade(id);
        String cpfNormalizado = ValidadorCpf.normalizar(requisicao.cpf());
        Optional<Paciente> porCpf = repositorio.buscarPorCpf(cpfNormalizado);
        if (porCpf.isPresent() && !porCpf.get().getIdPessoa().equals(id)) {
            throw new RegraDeNegocioException(
                    "O CPF " + ValidadorCpf.formatar(cpfNormalizado) + " pertence a outro paciente.");
        }
        MapeadorPaciente.aplicarDados(paciente, requisicao);
        return MapeadorPaciente.paraResposta(repositorio.atualizar(paciente), hoje());
    }

    /**
     * Remove um paciente e seus dados dependentes.
     *
     * @param id identificador do paciente
     * @throws RecursoNaoEncontradoException quando o paciente não existe
     */
    @Transactional
    public void excluir(Long id) {
        repositorio.excluir(buscarEntidade(id));
    }

    /**
     * Busca um paciente por identificador.
     *
     * @param id identificador do paciente
     * @return dados do paciente
     * @throws RecursoNaoEncontradoException quando o paciente não existe
     */
    public PacienteResposta buscarPorId(Long id) {
        return MapeadorPaciente.paraResposta(buscarEntidade(id), hoje());
    }

    /**
     * @return todos os pacientes cadastrados, ordenados por nome
     */
    public List<PacienteResposta> listarTodos() {
        LocalDate hoje = hoje();
        return repositorio.listarTodos().stream()
                .map(paciente -> MapeadorPaciente.paraResposta(paciente, hoje))
                .collect(Collectors.toList());
    }

    /**
     * Pesquisa pacientes pelo nome (busca parcial, sem diferenciar maiúsculas).
     *
     * @param nome trecho do nome
     * @return pacientes encontrados
     */
    public List<PacienteResposta> buscarPorNome(String nome) {
        LocalDate hoje = hoje();
        return repositorio.buscarPorNome(nome).stream()
                .map(paciente -> MapeadorPaciente.paraResposta(paciente, hoje))
                .collect(Collectors.toList());
    }

    /**
     * Obtém a entidade Paciente, lançando exceção caso não exista.
     *
     * @param id identificador do paciente
     * @return entidade gerenciada
     */
    public Paciente buscarEntidade(Long id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Paciente", id));
    }

    /**
     * @return quantidade total de pacientes
     */
    public long contar() {
        return repositorio.contar();
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }
}
