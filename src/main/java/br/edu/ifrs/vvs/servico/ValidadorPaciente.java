package br.edu.ifrs.vvs.servico;

import br.edu.ifrs.vvs.dominio.Situacao;
import br.edu.ifrs.vvs.dto.PacienteRequisicao;
import br.edu.ifrs.vvs.dto.TelefoneDTO;
import br.edu.ifrs.vvs.excecao.DadosInvalidosException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validação dos dados de cadastro de paciente.
 *
 * <p>Cada regra adiciona uma mensagem à lista de inconsistências, permitindo que o
 * cliente receba, em uma única resposta, todos os problemas encontrados (e não
 * apenas o primeiro). A validação é independente de banco de dados; a unicidade de
 * CPF é verificada na camada de serviço.</p>
 */
public final class ValidadorPaciente {

    /** Limite de idade (em anos) para aceitar o cadastro sem revisão. */
    public static final int IDADE_MAXIMA = 120;

    private static final int TAMANHO_MINIMO_NOME = 3;

    private static final Pattern FORMATO_EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private static final int DDD_MINIMO = 11;

    private static final int DDD_MAXIMO = 99;

    private ValidadorPaciente() {
        // Classe utilitária: não deve ser instanciada.
    }

    /**
     * Valida a requisição de paciente e lança {@link DadosInvalidosException} caso haja erros.
     *
     * @param requisicao dados enviados pelo cliente
     * @param hoje       data considerada como "hoje" (injetada para tornar o teste determinístico)
     * @throws DadosInvalidosException quando ao menos uma regra é violada
     */
    public static void validar(PacienteRequisicao requisicao, LocalDate hoje) {
        List<String> erros = coletarErros(requisicao, hoje);
        if (!erros.isEmpty()) {
            throw new DadosInvalidosException(erros);
        }
    }

    /**
     * Coleta todas as inconsistências dos dados de paciente.
     *
     * @param requisicao dados enviados pelo cliente
     * @param hoje       data de referência para o cálculo de idade
     * @return lista de mensagens de erro (vazia quando os dados são válidos)
     */
    public static List<String> coletarErros(PacienteRequisicao requisicao, LocalDate hoje) {
        List<String> erros = new ArrayList<>();
        if (requisicao == null) {
            erros.add("O corpo da requisição é obrigatório.");
            return erros;
        }
        validarNome(requisicao.nome(), erros);
        validarCpf(requisicao.cpf(), erros);
        validarDataNascimento(requisicao.dataNascimento(), hoje, erros);
        validarSituacao(requisicao.situacao(), erros);
        validarEmails(requisicao.emails(), erros);
        validarTelefones(requisicao.telefones(), erros);
        return erros;
    }

    private static void validarNome(String nome, List<String> erros) {
        if (nome == null || nome.isBlank()) {
            erros.add("O nome do paciente é obrigatório.");
        } else if (nome.trim().length() < TAMANHO_MINIMO_NOME) {
            erros.add("O nome do paciente deve ter ao menos " + TAMANHO_MINIMO_NOME + " caracteres.");
        }
    }

    private static void validarCpf(String cpf, List<String> erros) {
        if (cpf == null || cpf.isBlank()) {
            erros.add("O CPF do paciente é obrigatório.");
        } else if (!ValidadorCpf.ehValido(cpf)) {
            erros.add("O CPF informado é inválido.");
        }
    }

    private static void validarDataNascimento(LocalDate dataNascimento, LocalDate hoje, List<String> erros) {
        if (dataNascimento == null) {
            erros.add("A data de nascimento é obrigatória.");
            return;
        }
        if (dataNascimento.isAfter(hoje)) {
            erros.add("A data de nascimento não pode ser posterior à data atual.");
            return;
        }
        if (dataNascimento.isBefore(hoje.minusYears(IDADE_MAXIMA))) {
            erros.add("A data de nascimento indica idade superior a " + IDADE_MAXIMA + " anos.");
        }
    }

    private static void validarSituacao(String situacao, List<String> erros) {
        if (situacao != null && !situacao.isBlank() && Situacao.de(situacao) == null) {
            erros.add("Situação inválida. Valores aceitos: NORMAL, EM_OBSERVACAO, HOSPITALIZADO.");
        }
    }

    private static void validarEmails(List<String> emails, List<String> erros) {
        if (emails == null) {
            return;
        }
        for (String email : emails) {
            if (email == null || email.isBlank()) {
                erros.add("E-mail não pode ser vazio.");
            } else if (!FORMATO_EMAIL.matcher(email.trim()).matches()) {
                erros.add("E-mail inválido: " + email + ".");
            }
        }
    }

    private static void validarTelefones(List<TelefoneDTO> telefones, List<String> erros) {
        if (telefones == null) {
            return;
        }
        for (TelefoneDTO telefone : telefones) {
            if (telefone == null) {
                erros.add("Telefone não pode ser nulo.");
                continue;
            }
            if (telefone.ddd() == null || telefone.ddd() < DDD_MINIMO || telefone.ddd() > DDD_MAXIMO) {
                erros.add("DDD inválido: " + telefone.ddd() + ".");
            }
            if (telefone.numero() == null || telefone.numero() <= 0) {
                erros.add("Número de telefone inválido: " + telefone.numero() + ".");
            }
        }
    }
}
