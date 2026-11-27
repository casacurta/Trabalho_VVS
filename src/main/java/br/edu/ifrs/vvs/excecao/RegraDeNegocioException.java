package br.edu.ifrs.vvs.excecao;

/**
 * Exceção lançada quando uma regra de negócio é violada
 * (ex.: conflito de agenda, CPF já cadastrado). Resulta em resposta HTTP 409.
 */
public class RegraDeNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
