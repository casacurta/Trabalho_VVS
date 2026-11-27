package br.edu.ifrs.vvs.repositorio;

import br.edu.ifrs.vvs.dominio.Prontuario;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de acesso a dados de {@link Prontuario}.
 */
@ApplicationScoped
public class ProntuarioRepositorio {

    private final EntityManager em;

    public ProntuarioRepositorio(EntityManager em) {
        this.em = em;
    }

    /**
     * Persiste um novo prontuário.
     *
     * @param prontuario prontuário a ser persistido
     * @return o próprio prontuário
     */
    public Prontuario salvar(Prontuario prontuario) {
        em.persist(prontuario);
        return prontuario;
    }

    /**
     * Atualiza um prontuário existente.
     *
     * @param prontuario prontuário com os dados atualizados
     * @return instância gerenciada do prontuário
     */
    public Prontuario atualizar(Prontuario prontuario) {
        return em.merge(prontuario);
    }

    /**
     * Busca um prontuário pelo identificador.
     *
     * @param id identificador do prontuário
     * @return prontuário encontrado ou {@link Optional#empty()}
     */
    public Optional<Prontuario> buscarPorId(Long id) {
        return Optional.ofNullable(em.find(Prontuario.class, id));
    }

    /**
     * Remove um prontuário.
     *
     * @param prontuario prontuário a ser removido
     */
    public void excluir(Prontuario prontuario) {
        em.remove(em.contains(prontuario) ? prontuario : em.merge(prontuario));
    }

    /**
     * @return todos os prontuários ordenados por data
     */
    public List<Prontuario> listarTodos() {
        return em.createQuery(
                "SELECT p FROM Prontuario p ORDER BY p.data DESC", Prontuario.class)
                .getResultList();
    }
}
