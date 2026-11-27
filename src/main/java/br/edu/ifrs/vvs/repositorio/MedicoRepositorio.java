package br.edu.ifrs.vvs.repositorio;

import br.edu.ifrs.vvs.dominio.Medico;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Repositório de acesso a dados de {@link Medico}.
 */
@ApplicationScoped
public class MedicoRepositorio {

    private final EntityManager em;

    public MedicoRepositorio(EntityManager em) {
        this.em = em;
    }

    /**
     * Persiste um novo médico.
     *
     * @param medico médico a ser persistido
     * @return o próprio médico
     */
    public Medico salvar(Medico medico) {
        em.persist(medico);
        return medico;
    }

    /**
     * Atualiza um médico existente.
     *
     * @param medico médico com os dados atualizados
     * @return instância gerenciada do médico
     */
    public Medico atualizar(Medico medico) {
        return em.merge(medico);
    }

    /**
     * Busca um médico pelo identificador.
     *
     * @param id identificador do médico
     * @return médico encontrado ou {@link Optional#empty()}
     */
    public Optional<Medico> buscarPorId(Long id) {
        return Optional.ofNullable(em.find(Medico.class, id));
    }

    /**
     * Remove um médico.
     *
     * @param medico médico a ser removido
     */
    public void excluir(Medico medico) {
        em.remove(em.contains(medico) ? medico : em.merge(medico));
    }

    /**
     * @return todos os médicos ordenados por especialidade
     */
    public List<Medico> listarTodos() {
        return em.createQuery("SELECT m FROM Medico m ORDER BY m.especialidade, m.crm", Medico.class)
                .getResultList();
    }

    /**
     * Busca um médico pelo CRM.
     *
     * @param crm registro do CRM
     * @return médico encontrado ou {@link Optional#empty()}
     */
    public Optional<Medico> buscarPorCrm(String crm) {
        TypedQuery<Medico> consulta = em.createQuery(
                "SELECT m FROM Medico m WHERE UPPER(m.crm) = :crm", Medico.class);
        consulta.setParameter("crm", crm.toUpperCase(Locale.ROOT));
        consulta.setMaxResults(1);
        return consulta.getResultList().stream().findFirst();
    }

    /**
     * @param especialidade especialidade pesquisada
     * @return médicos da especialidade informada
     */
    public List<Medico> buscarPorEspecialidade(String especialidade) {
        TypedQuery<Medico> consulta = em.createQuery(
                "SELECT m FROM Medico m WHERE LOWER(m.especialidade) = :especialidade ORDER BY m.crm",
                Medico.class);
        consulta.setParameter("especialidade", especialidade.toLowerCase(Locale.ROOT));
        return consulta.getResultList();
    }

    /**
     * @return quantidade total de médicos cadastrados
     */
    public long contar() {
        return em.createQuery("SELECT COUNT(m) FROM Medico m", Long.class)
                .getSingleResult();
    }
}
