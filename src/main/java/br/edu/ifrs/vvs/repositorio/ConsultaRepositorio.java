package br.edu.ifrs.vvs.repositorio;

import br.edu.ifrs.vvs.dominio.Consulta;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de acesso a dados de {@link Consulta}.
 */
@ApplicationScoped
public class ConsultaRepositorio {

    private final EntityManager em;

    public ConsultaRepositorio(EntityManager em) {
        this.em = em;
    }

    /**
     * Persiste uma nova consulta.
     *
     * @param consulta consulta a ser persistida
     * @return a própria consulta
     */
    public Consulta salvar(Consulta consulta) {
        em.persist(consulta);
        return consulta;
    }

    /**
     * Atualiza uma consulta existente.
     *
     * @param consulta consulta com os dados atualizados
     * @return instância gerenciada da consulta
     */
    public Consulta atualizar(Consulta consulta) {
        return em.merge(consulta);
    }

    /**
     * Busca uma consulta pelo identificador.
     *
     * @param id identificador da consulta
     * @return consulta encontrada ou {@link Optional#empty()}
     */
    public Optional<Consulta> buscarPorId(Long id) {
        return Optional.ofNullable(em.find(Consulta.class, id));
    }

    /**
     * Remove uma consulta.
     *
     * @param consulta consulta a ser removida
     */
    public void excluir(Consulta consulta) {
        em.remove(em.contains(consulta) ? consulta : em.merge(consulta));
    }

    /**
     * @return todas as consultas ordenadas por data e horário
     */
    public List<Consulta> listarTodas() {
        return em.createQuery(
                "SELECT c FROM Consulta c ORDER BY c.data, c.horario", Consulta.class)
                .getResultList();
    }

    /**
     * @param pacienteId identificador do paciente
     * @return consultas do paciente, ordenadas por data e horário
     */
    public List<Consulta> listarPorPaciente(Long pacienteId) {
        TypedQuery<Consulta> consulta = em.createQuery(
                "SELECT c FROM Consulta c WHERE c.paciente.idPessoa = :id ORDER BY c.data, c.horario",
                Consulta.class);
        consulta.setParameter("id", pacienteId);
        return consulta.getResultList();
    }

    /**
     * @param medicoId identificador do médico
     * @return consultas do médico, ordenadas por data e horário
     */
    public List<Consulta> listarPorMedico(Long medicoId) {
        TypedQuery<Consulta> consulta = em.createQuery(
                "SELECT c FROM Consulta c WHERE c.medico.idMedico = :id ORDER BY c.data, c.horario",
                Consulta.class);
        consulta.setParameter("id", medicoId);
        return consulta.getResultList();
    }

    /**
     * Verifica se o médico já possui consulta na data e horário informados.
     *
     * @param medicoId    identificador do médico
     * @param data        data da consulta
     * @param horario     horário da consulta
     * @param ignorarId   consulta a desconsiderar na verificação (atualização)
     * @return {@code true} quando já existe consulta no mesmo horário
     */
    public boolean existeConsultaDoMedico(Long medicoId, LocalDate data, LocalTime horario, Long ignorarId) {
        TypedQuery<Long> consulta = em.createQuery(
                "SELECT COUNT(c) FROM Consulta c WHERE c.medico.idMedico = :medico "
                        + "AND c.data = :data AND c.horario = :horario "
                        + "AND (:ignorar IS NULL OR c.idConsulta <> :ignorar)",
                Long.class);
        consulta.setParameter("medico", medicoId);
        consulta.setParameter("data", data);
        consulta.setParameter("horario", horario);
        consulta.setParameter("ignorar", ignorarId);
        return consulta.getSingleResult() > 0;
    }

    /**
     * Verifica se o paciente já possui consulta na data e horário informados.
     *
     * @param pacienteId identificador do paciente
     * @param data       data da consulta
     * @param horario    horário da consulta
     * @param ignorarId  consulta a desconsiderar na verificação (atualização)
     * @return {@code true} quando já existe consulta no mesmo horário
     */
    public boolean existeConsultaDoPaciente(Long pacienteId, LocalDate data, LocalTime horario, Long ignorarId) {
        TypedQuery<Long> consulta = em.createQuery(
                "SELECT COUNT(c) FROM Consulta c WHERE c.paciente.idPessoa = :paciente "
                        + "AND c.data = :data AND c.horario = :horario "
                        + "AND (:ignorar IS NULL OR c.idConsulta <> :ignorar)",
                Long.class);
        consulta.setParameter("paciente", pacienteId);
        consulta.setParameter("data", data);
        consulta.setParameter("horario", horario);
        consulta.setParameter("ignorar", ignorarId);
        return consulta.getSingleResult() > 0;
    }

    /**
     * @param pacienteId identificador do paciente
     * @return quantidade de consultas do paciente
     */
    public long contarPorPaciente(Long pacienteId) {
        return em.createQuery(
                "SELECT COUNT(c) FROM Consulta c WHERE c.paciente.idPessoa = :id", Long.class)
                .setParameter("id", pacienteId)
                .getSingleResult();
    }

    /**
     * @param medicoId identificador do médico
     * @return quantidade de consultas do médico
     */
    public long contarPorMedico(Long medicoId) {
        return em.createQuery(
                "SELECT COUNT(c) FROM Consulta c WHERE c.medico.idMedico = :id", Long.class)
                .setParameter("id", medicoId)
                .getSingleResult();
    }
}
