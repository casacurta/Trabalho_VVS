package br.edu.ifrs.vvs.dominio;

/**
 * Situação clínica em que o paciente se encontra.
 *
 * <p>Enumeração persistida como texto ({@code @Enumerated(EnumType.STRING)}),
 * mantendo o significado legível no banco de dados.</p>
 */
public enum Situacao {

    /** Paciente sem restrições de acompanhamento. */
    NORMAL("Normal", 1),

    /** Paciente em acompanhamento/observação. */
    EM_OBSERVACAO("Em observação", 2),

    /** Paciente internado. */
    HOSPITALIZADO("Hospitalizado", 3);

    private final String descricao;
    private final int id;

    Situacao(String descricao, int id) {
        this.descricao = descricao;
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getId() {
        return id;
    }

    /**
     * Recupera a situação a partir do nome do enum (sem diferenciar maiúsculas/minúsculas).
     *
     * @param valor nome da situação (ex.: {@code "normal"})
     * @return situação correspondente ou {@code null} quando o valor não é reconhecido
     */
    public static Situacao de(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        for (Situacao situacao : values()) {
            if (situacao.name().equalsIgnoreCase(valor.trim())) {
                return situacao;
            }
        }
        return null;
    }
}
