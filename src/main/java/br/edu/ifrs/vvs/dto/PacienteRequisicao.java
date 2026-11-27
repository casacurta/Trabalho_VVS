package br.edu.ifrs.vvs.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Dados enviados pelo cliente para cadastrar ou atualizar um paciente.
 *
 * @param nome           nome completo (obrigatório)
 * @param endereco       endereço residencial
 * @param telefones      telefones de contato
 * @param convenio       convênio médico
 * @param cpf            CPF (obrigatório e único)
 * @param dataNascimento data de nascimento (obrigatória, formato ISO-8601)
 * @param emails         e-mails de contato
 * @param situacao       situação clínica: NORMAL, EM_OBSERVACAO ou HOSPITALIZADO
 * @param prontuario     prontuário inicial/atualizado (opcional)
 */
public record PacienteRequisicao(
        String nome,
        String endereco,
        List<TelefoneDTO> telefones,
        String convenio,
        String cpf,
        LocalDate dataNascimento,
        List<String> emails,
        String situacao,
        ProntuarioDTO prontuario) {
}
