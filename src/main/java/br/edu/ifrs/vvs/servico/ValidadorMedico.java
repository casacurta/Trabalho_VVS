package br.edu.ifrs.vvs.servico;

import br.edu.ifrs.vvs.dto.MedicoRequisicao;
import br.edu.ifrs.vvs.excecao.DadosInvalidosException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validação dos dados de cadastro de médico.
 */
public final class ValidadorMedico {

    private static final int TAMANHO_MINIMO_ESPECIALIDADE = 3;

    private ValidadorMedico() {
        // Classe utilitária: não deve ser instanciada.
    }

    /**
     * Valida a requisição de médico e lança {@link DadosInvalidosException} caso haja erros.
     *
     * @param requisicao dados enviados pelo cliente
     */
    public static void validar(MedicoRequisicao requisicao) {
        List<String> erros = coletarErros(requisicao);
        if (!erros.isEmpty()) {
            throw new DadosInvalidosException(erros);
        }
    }

    /**
     * Coleta todas as inconsistências dos dados de médico.
     *
     * @param requisicao dados enviados pelo cliente
     * @return lista de mensagens de erro
     */
    public static List<String> coletarErros(MedicoRequisicao requisicao) {
        List<String> erros = new ArrayList<>();
        if (requisicao == null) {
            erros.add("O corpo da requisição é obrigatório.");
            return erros;
        }
        if (requisicao.crm() == null || requisicao.crm().isBlank()) {
            erros.add("O CRM do médico é obrigatório.");
        } else if (!ValidadorCrm.ehValido(requisicao.crm())) {
            erros.add("CRM inválido. Use o formato 12345/UF (ex.: 24016/RS).");
        }
        if (requisicao.especialidade() == null || requisicao.especialidade().isBlank()) {
            erros.add("A especialidade do médico é obrigatória.");
        } else if (requisicao.especialidade().trim().length() < TAMANHO_MINIMO_ESPECIALIDADE) {
            erros.add("A especialidade deve ter ao menos " + TAMANHO_MINIMO_ESPECIALIDADE + " caracteres.");
        }
        return erros;
    }
}
