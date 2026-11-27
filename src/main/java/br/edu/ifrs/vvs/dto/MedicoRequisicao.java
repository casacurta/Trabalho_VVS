package br.edu.ifrs.vvs.dto;

/**
 * Dados enviados pelo cliente para cadastrar ou atualizar um médico.
 *
 * @param crm           registro no CRM, no formato {@code 12345/UF} (obrigatório e único)
 * @param especialidade especialidade médica (obrigatória)
 */
public record MedicoRequisicao(String crm, String especialidade) {
}
