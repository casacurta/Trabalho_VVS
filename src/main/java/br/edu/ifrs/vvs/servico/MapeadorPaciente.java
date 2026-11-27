package br.edu.ifrs.vvs.servico;

import br.edu.ifrs.vvs.dominio.Paciente;
import br.edu.ifrs.vvs.dominio.Prontuario;
import br.edu.ifrs.vvs.dominio.Situacao;
import br.edu.ifrs.vvs.dominio.Telefone;
import br.edu.ifrs.vvs.dto.PacienteRequisicao;
import br.edu.ifrs.vvs.dto.PacienteResposta;
import br.edu.ifrs.vvs.dto.ProntuarioDTO;
import br.edu.ifrs.vvs.dto.TelefoneDTO;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Conversão entre {@link Paciente} (entidade) e os DTOs da API.
 *
 * <p>Centralizar a conversão evita que a entidade JPA vaze para fora da camada de
 * serviço e mantém as regras de formatação em um único lugar.</p>
 */
public final class MapeadorPaciente {

    private MapeadorPaciente() {
        // Classe utilitária: não deve ser instanciada.
    }

    /**
     * Converte a requisição em uma nova entidade Paciente.
     *
     * @param requisicao dados validados enviados pelo cliente
     * @return entidade pronta para persistência
     */
    public static Paciente paraEntidade(PacienteRequisicao requisicao) {
        Paciente paciente = new Paciente();
        aplicarDados(paciente, requisicao);
        return paciente;
    }

    /**
     * Aplica os dados da requisição sobre uma entidade existente.
     *
     * @param paciente   entidade a ser atualizada
     * @param requisicao dados validados enviados pelo cliente
     */
    public static void aplicarDados(Paciente paciente, PacienteRequisicao requisicao) {
        paciente.setNome(requisicao.nome().trim());
        paciente.setEndereco(requisicao.endereco());
        paciente.setConvenio(requisicao.convenio());
        paciente.setCpf(ValidadorCpf.normalizar(requisicao.cpf()));
        paciente.setDataNascimento(requisicao.dataNascimento());
        Situacao situacaoInformada = Situacao.de(requisicao.situacao());
        paciente.setSituacao(situacaoInformada == null ? Situacao.NORMAL : situacaoInformada);
        paciente.setEmails(extrairEmails(requisicao.emails()));
        paciente.setTelefones(extrairTelefones(requisicao.telefones()));
        aplicarProntuario(paciente, requisicao.prontuario());
    }

    /**
     * Converte a entidade em DTO de resposta.
     *
     * @param paciente entidade a ser convertida
     * @param hoje     data de referência para o cálculo da idade
     * @return DTO com os dados do paciente
     */
    public static PacienteResposta paraResposta(Paciente paciente, LocalDate hoje) {
        List<TelefoneDTO> telefones = new ArrayList<>();
        if (paciente.getTelefones() != null) {
            for (Telefone telefone : paciente.getTelefones()) {
                if (telefone != null) {
                    telefones.add(new TelefoneDTO(telefone.getDdd(), telefone.getNumero()));
                }
            }
        }
        ProntuarioDTO prontuario = paciente.getProntuario() == null
                ? null
                : new ProntuarioDTO(paciente.getProntuario().getData(),
                        paciente.getProntuario().getDescricao());
        Situacao situacao = paciente.getSituacao();
        int totalConsultas = paciente.getConsultas() == null ? 0 : paciente.getConsultas().size();

        return new PacienteResposta(
                paciente.getIdPessoa(),
                paciente.getNome(),
                paciente.getEndereco(),
                telefones,
                paciente.getConvenio(),
                paciente.getCpf(),
                paciente.getDataNascimento(),
                calcularIdade(paciente.getDataNascimento(), hoje),
                paciente.getEmails() == null ? List.of() : List.copyOf(paciente.getEmails()),
                situacao == null ? null : situacao.name(),
                situacao == null ? null : situacao.getDescricao(),
                prontuario,
                totalConsultas);
    }

    /**
     * Calcula a idade em anos completos.
     *
     * @param nascimento data de nascimento (pode ser nula)
     * @param hoje       data de referência
     * @return idade em anos ou {@code null} quando a data de nascimento não existe
     */
    public static Integer calcularIdade(LocalDate nascimento, LocalDate hoje) {
        return nascimento == null ? null : Period.between(nascimento, hoje).getYears();
    }

    private static void aplicarProntuario(Paciente paciente, ProntuarioDTO dto) {
        if (dto == null) {
            return;
        }
        Prontuario prontuario = paciente.getProntuario();
        if (prontuario == null) {
            prontuario = new Prontuario();
            paciente.setProntuario(prontuario);
        }
        prontuario.setData(dto.data() == null ? LocalDate.now() : dto.data());
        prontuario.setDescricao(dto.descricao());
    }

    private static List<String> extrairEmails(List<String> emails) {
        List<String> resultado = new ArrayList<>();
        if (emails != null) {
            for (String email : emails) {
                if (email != null && !email.isBlank()) {
                    resultado.add(email.trim());
                }
            }
        }
        return resultado;
    }

    private static List<Telefone> extrairTelefones(List<TelefoneDTO> telefones) {
        List<Telefone> resultado = new ArrayList<>();
        if (telefones != null) {
            for (TelefoneDTO dto : telefones) {
                if (dto != null && dto.ddd() != null && dto.numero() != null) {
                    resultado.add(new Telefone(dto.ddd(), dto.numero()));
                }
            }
        }
        return resultado;
    }
}
