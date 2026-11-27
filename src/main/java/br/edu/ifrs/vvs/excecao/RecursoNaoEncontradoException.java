package br.edu.ifrs.vvs.excecao;

/**
 * Exceção lançada quando o recurso solicitado não existe.
 * Resulta em resposta HTTP 404.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    /**
     * Cria a exceção com mensagem padronizada.
     *
     * @param recurso nome do recurso (ex.: "Paciente")
     * @param id      identificador consultado
     * @return exceção pronta para lançamento
     */
    public static RecursoNaoEncontradoException de(String recurso, Object id) {
        return new RecursoNaoEncontradoException(recurso + " não encontrado(a) para o identificador " + id + ".");
    }
}
