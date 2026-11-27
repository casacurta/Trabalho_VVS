package br.edu.ifrs.vvs.dto;

/**
 * Dados de um médico devolvidos pela API.
 *
 * @param id            identificador do médico
 * @param crm           registro no CRM
 * @param especialidade especialidade médica
 * @param totalConsultas quantidade de consultas do médico
 */
public record MedicoResposta(Long id, String crm, String especialidade, Integer totalConsultas) {
}
