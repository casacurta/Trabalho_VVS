package br.edu.ifrs.vvs.repositorio;

import br.edu.ifrs.vvs.dominio.Paciente;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Repositório de acesso a dados de {@link Paciente}.
 *
 * <p>O {@link EntityManager} é injetado pelo Quarkus (contexto de transação
 * controlado pela camada de serviço), substituindo o antigo {@code JPAUtil}.</p>
 */
@ApplicationScoped
public class PacienteRepositorio {

    private final EntityManager em;

    public PacienteRepositorio(EntityManager em) {
        this.em = em;
    }

    /**
     * Persiste um novo paciente.
     *
     * @param paciente paciente a ser persistido
     * @return o próprio paciente, já gerenciado
     */
    public Paciente salvar(Paciente paciente) {
        em.persist(paciente);
        return paciente;
    }

    /**
     * Atualiza um paciente existente.
     *
     * @param paciente paciente com os dados atualizados
     * @return instância gerenciada do paciente
     */
    public Paciente atualizar(Paciente paciente) {
        return em.merge(paciente);
    }

    /**
     * Busca um paciente pelo identificador.
     *
     * @param id identificador do paciente
     * @return paciente encontrado ou {@link Optional#empty()}
     */
    public Optional<Paciente> buscarPorId(Long id) {
        return Optional.ofNullable(em.find(Paciente.class, id));
    }

    /**
     * Remove um paciente.
     *
     * @param paciente paciente a ser removido
     */
    public void excluir(Paciente paciente) {
        em.remove(em.contains(paciente) ? paciente : em.merge(paciente));
    }

    /**
     * @return todos os pacientes ordenados por nome
     */
    public List<Paciente> listarTodos() {
        return em.createQuery("SELECT p FROM Paciente p ORDER BY p.nome", Paciente.class)
                .getResultList();
    }

    /**
     * Busca um paciente pelo CPF (comparação exata, sem máscara).
     *
     * @param cpf CPF normalizado (somente dígitos)
     * @return paciente encontrado ou {@link Optional#empty()}
     */
    public Optional<Paciente> buscarPorCpf(String cpf) {
        TypedQuery<Paciente> consulta = em.createQuery(
                "SELECT p FROM Paciente p WHERE p.cpf = :cpf", Paciente.class);
        consulta.setParameter("cpf", cpf);
        consulta.setMaxResults(1);
        return consulta.getResultList().stream().findFirst();
    }

    /**
     * Busca pacientes cujo nome contenha o trecho informado (sem diferenciar maiúsculas).
     *
     * @param trecho parte do nome a ser pesquisada
     * @return lista de pacientes encontrados
     */
    public List<Paciente> buscarPorNome(String trecho) {
        TypedQuery<Paciente> consulta = em.createQuery(
                "SELECT p FROM Paciente p WHERE LOWER(p.nome) LIKE :trecho ORDER BY p.nome",
                Paciente.class);
        consulta.setParameter("trecho", "%" + trecho.toLowerCase(Locale.ROOT) + "%");
        return consulta.getResultList();
    }

    /**
     * @return quantidade total de pacientes cadastrados
     */
    public long contar() {
        return em.createQuery("SELECT COUNT(p) FROM Paciente p", Long.class)
                .getSingleResult();
    }
}
