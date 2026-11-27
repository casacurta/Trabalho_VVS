package br.edu.ifrs.vvs.dto;

import java.time.LocalDate;

/**
 * Dados do prontuário do paciente.
 *
 * @param data      data do registro clínico
 * @param descricao descrição do quadro clínico
 */
public record ProntuarioDTO(LocalDate data, String descricao) {
}
