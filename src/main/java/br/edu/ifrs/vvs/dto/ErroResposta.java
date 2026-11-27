package br.edu.ifrs.vvs.dto;

import java.time.Instant;
import java.util.List;

/**
 * Corpo padronizado de resposta de erro da API.
 *
 * @param status    código HTTP do erro
 * @param erro      categoria resumida do erro (ex.: "Erro de validação")
 * @param mensagem  mensagem principal
 * @param detalhes  lista de inconsistências encontradas
 * @param caminho   caminho da requisição que originou o erro
 * @param momento   instante em que o erro foi registrado
 */
public record ErroResposta(
        int status,
        String erro,
        String mensagem,
        List<String> detalhes,
        String caminho,
        Instant momento) {
}
