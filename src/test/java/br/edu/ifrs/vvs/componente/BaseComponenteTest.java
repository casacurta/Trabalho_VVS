package br.edu.ifrs.vvs.componente;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;

/**
 * Classe base dos testes de componente e de sistema.
 *
 * <p><b>Objetivo:</b> garantir isolamento entre os testes que utilizam o banco H2.
 * Enquanto os testes de repositório revertem a transação, os testes de API REST
 * confirmam os dados (commit). Por isso, a base é limpa antes de cada teste, na ordem
 * que respeita as chaves estrangeiras.</p>
 *
 * <p>Este cuidado evita falsos positivos/negativos (por exemplo, um CPF que
 * aparentaria estar duplicado por causa de um teste anterior).</p>
 */
public abstract class BaseComponenteTest {

    @Inject
    EntityManager em;

    /**
     * Remove todos os registros de todas as tabelas do domínio.
     * A ordem importa: filhas antes das mães.
     */
    @BeforeEach
    void limparBaseDados() {
        QuarkusTransaction.requiringNew().run(() -> {
            em.createNativeQuery("DELETE FROM Consulta").executeUpdate();
            em.createNativeQuery("DELETE FROM Lista_emails").executeUpdate();
            em.createNativeQuery("DELETE FROM Telefone").executeUpdate();
            em.createNativeQuery("DELETE FROM Paciente").executeUpdate();
            em.createNativeQuery("DELETE FROM Pessoa").executeUpdate();
            em.createNativeQuery("DELETE FROM Prontuario").executeUpdate();
            em.createNativeQuery("DELETE FROM Medico").executeUpdate();
        });
    }
}
