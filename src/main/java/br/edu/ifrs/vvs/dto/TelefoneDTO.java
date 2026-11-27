package br.edu.ifrs.vvs.dto;

/**
 * Representação de um telefone nas requisições e respostas da API.
 *
 * @param ddd    código de área (DDD)
 * @param numero número do telefone, sem formatação
 */
public record TelefoneDTO(Integer ddd, Long numero) {
}
