package br.edu.ifrs.vvs.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Dados enviados pelo cliente para agendar uma consulta.
 *
 * @param data       data da consulta (ISO-8601)
 * @param horario    horário da consulta (ISO-8601, ex.: {@code 14:30})
 * @param idMedico   identificador do médico responsável
 * @param idPaciente identificador do paciente
 */
public record ConsultaRequisicao(LocalDate data, LocalTime horario, Long idMedico, Long idPaciente) {
}
