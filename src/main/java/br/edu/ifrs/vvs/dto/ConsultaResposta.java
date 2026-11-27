package br.edu.ifrs.vvs.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Dados de uma consulta devolvidos pela API.
 *
 * @param id              identificador da consulta
 * @param data            data da consulta
 * @param horario         horário da consulta
 * @param idMedico        identificador do médico
 * @param nomeMedico      nome/identificação do médico (CRM e especialidade)
 * @param idPaciente      identificador do paciente
 * @param nomePaciente    nome do paciente
 */
public record ConsultaResposta(
        Long id,
        LocalDate data,
        LocalTime horario,
        Long idMedico,
        String nomeMedico,
        Long idPaciente,
        String nomePaciente) {
}
