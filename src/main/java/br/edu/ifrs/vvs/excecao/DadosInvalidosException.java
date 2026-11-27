package br.edu.ifrs.vvs.excecao;

import java.util.List;

/**
 * Exceção lançada quando os dados recebidos são inválidos.
 * Carrega a lista completa de inconsistências para facilitar a correção pelo cliente.
 * Resulta em resposta HTTP 400.
 */
public class DadosInvalidosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<String> detalhes;

    public DadosInvalidosException(List<String> detalhes) {
        super("Dados inválidos: " + String.join("; ", detalhes));
        this.detalhes = List.copyOf(detalhes);
    }

    public DadosInvalidosException(String detalhe) {
        this(List.of(detalhe));
    }

    /**
     * @return lista imutável com todas as inconsistências encontradas
     */
    public List<String> getDetalhes() {
        return detalhes;
    }
}
