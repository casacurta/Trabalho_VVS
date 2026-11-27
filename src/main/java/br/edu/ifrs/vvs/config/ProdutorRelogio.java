package br.edu.ifrs.vvs.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Disponibiliza um {@link Clock} para os serviços.
 *
 * <p>Usar um relógio injetável permite que as validações de data (idade máxima,
 * agendamento no passado) sejam testadas de forma determinística.</p>
 */
@ApplicationScoped
public class ProdutorRelogio {

    /**
     * @return relógio padrão do sistema (fuso da máquina)
     */
    @Produces
    @ApplicationScoped
    public Clock relogio() {
        return Clock.systemDefaultZone();
    }

    /**
     * @param relogio relógio injetado
     * @return data atual segundo o relógio
     */
    public static LocalDate hoje(Clock relogio) {
        return LocalDate.now(relogio);
    }

    /**
     * @param relogio relógio injetado
     * @return data e hora atuais segundo o relógio
     */
    public static LocalDateTime agora(Clock relogio) {
        return LocalDateTime.now(relogio);
    }
}
