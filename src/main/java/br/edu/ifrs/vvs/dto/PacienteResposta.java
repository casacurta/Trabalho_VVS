package br.edu.ifrs.vvs.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Dados de um paciente devolvidos pela API.
 *
 * @param id                   identificador do paciente
 * @param nome                 nome completo
 * @param endereco             endereço residencial
 * @param telefones            telefones de contato
 * @param convenio             convênio médico
 * @param cpf                  CPF
 * @param dataNascimento       data de nascimento
 * @param idade                idade calculada em anos completos
 * @param emails               e-mails de contato
 * @param situacao             nome da situação clínica
 * @param situacaoDescricao    descrição legível da situação clínica
 * @param prontuario           prontuário atual (pode ser nulo)
 * @param totalConsultas       quantidade de consultas agendadas
 */
public record PacienteResposta(
        Long id,
        String nome,
        String endereco,
        List<TelefoneDTO> telefones,
        String convenio,
        String cpf,
        LocalDate dataNascimento,
        Integer idade,
        List<String> emails,
        String situacao,
        String situacaoDescricao,
        ProntuarioDTO prontuario,
        Integer totalConsultas) {
}
