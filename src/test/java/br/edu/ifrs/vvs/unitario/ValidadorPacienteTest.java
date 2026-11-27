package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifrs.vvs.dto.PacienteRequisicao;
import br.edu.ifrs.vvs.dto.ProntuarioDTO;
import br.edu.ifrs.vvs.dto.TelefoneDTO;
import br.edu.ifrs.vvs.excecao.DadosInvalidosException;
import br.edu.ifrs.vvs.servico.ValidadorPaciente;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Testes unitários de {@link ValidadorPaciente}.
 *
 * <p><b>Objetivo:</b> validar as regras de aceitação do cadastro de pacientes
 * (nome, CPF, data de nascimento, situação, e-mails e telefones), verificando
 * casos de sucesso, falha e valores de borda, como a idade máxima de 120 anos.</p>
 */
@DisplayName("ValidadorPaciente - regras de cadastro de paciente")
class ValidadorPacienteTest {

    /** Data de referência fixa para tornar os testes determinísticos. */
    private static final LocalDate HOJE = LocalDate.of(2026, 6, 1);

    private static PacienteRequisicao requisicaoValida() {
        return new PacienteRequisicao(
                "Antonio Carlos",
                "Av. 24 de Outubro, 267",
                List.of(new TelefoneDTO(51, 987654321L)),
                "IPE",
                "111.444.777-35",
                LocalDate.of(2002, 1, 8),
                List.of("antonio@mail.com"),
                "EM_OBSERVACAO",
                new ProntuarioDTO(HOJE, "Paciente estável"));
    }

    @Test
    @DisplayName("deve aceitar cadastro completamente válido")
    void deveAceitarCadastroValido() {
        assertDoesNotThrow(() -> ValidadorPaciente.validar(requisicaoValida(), HOJE));
    }

    @Test
    @DisplayName("deve aceitar cadastro sem prontuário, sem e-mails e sem telefones")
    void deveAceitarCamposOpcionaisAusentes() {
        PacienteRequisicao requisicao = new PacienteRequisicao(
                "Maria Silva", null, null, null, "11144477735",
                LocalDate.of(1990, 5, 20), null, null, null);
        assertDoesNotThrow(() -> ValidadorPaciente.validar(requisicao, HOJE));
    }

    @Nested
    @DisplayName("Nome")
    class Nome {

        @Test
        @DisplayName("deve recusar nome nulo")
        void deveRecusarNomeNulo() {
            assertMensagem(new PacienteRequisicao(null, null, null, null, "11144477735",
                    LocalDate.of(1990, 5, 20), null, null, null), "O nome do paciente é obrigatório.");
        }

        @Test
        @DisplayName("deve recusar nome em branco")
        void deveRecusarNomeEmBranco() {
            assertMensagem(new PacienteRequisicao("   ", null, null, null, "11144477735",
                    LocalDate.of(1990, 5, 20), null, null, null), "O nome do paciente é obrigatório.");
        }

        @Test
        @DisplayName("deve recusar nome com menos de 3 caracteres (valor de borda)")
        void deveRecusarNomeCurto() {
            assertMensagem(new PacienteRequisicao("Jo", null, null, null, "11144477735",
                    LocalDate.of(1990, 5, 20), null, null, null),
                    "O nome do paciente deve ter ao menos 3 caracteres.");
        }

        @Test
        @DisplayName("deve aceitar nome com exatamente 3 caracteres (valor de borda)")
        void deveAceitarNomeComTresCaracteres() {
            PacienteRequisicao requisicao = new PacienteRequisicao("Ana", null, null, null,
                    "11144477735", LocalDate.of(1990, 5, 20), null, null, null);
            assertDoesNotThrow(() -> ValidadorPaciente.validar(requisicao, HOJE));
        }
    }

    @Nested
    @DisplayName("CPF")
    class Cpf {

        @Test
        @DisplayName("deve recusar CPF ausente")
        void deveRecusarCpfAusente() {
            assertMensagem(new PacienteRequisicao("Maria Silva", null, null, null, null,
                    LocalDate.of(1990, 5, 20), null, null, null), "O CPF do paciente é obrigatório.");
        }

        @Test
        @DisplayName("deve recusar CPF inválido")
        void deveRecusarCpfInvalido() {
            assertMensagem(new PacienteRequisicao("Maria Silva", null, null, null, "12345678900",
                    LocalDate.of(1990, 5, 20), null, null, null), "O CPF informado é inválido.");
        }
    }

    @Nested
    @DisplayName("Data de nascimento")
    class DataNascimento {

        @Test
        @DisplayName("deve recusar data de nascimento ausente")
        void deveRecusarDataAusente() {
            assertMensagem(new PacienteRequisicao("Maria Silva", null, null, null, "11144477735",
                    null, null, null, null), "A data de nascimento é obrigatória.");
        }

        @Test
        @DisplayName("deve recusar data de nascimento no futuro")
        void deveRecusarDataFutura() {
            assertMensagem(new PacienteRequisicao("Maria Silva", null, null, null, "11144477735",
                    HOJE.plusDays(1), null, null, null),
                    "A data de nascimento não pode ser posterior à data atual.");
        }

        @Test
        @DisplayName("deve recusar idade superior a 120 anos (valor de borda)")
        void deveRecusarIdadeAcimaDoLimite() {
            assertMensagem(new PacienteRequisicao("Maria Silva", null, null, null, "11144477735",
                    HOJE.minusYears(121), null, null, null),
                    "A data de nascimento indica idade superior a 120 anos.");
        }

        @Test
        @DisplayName("deve aceitar idade exatamente igual a 120 anos (valor de borda)")
        void deveAceitarIdadeDe120Anos() {
            PacienteRequisicao requisicao = new PacienteRequisicao("Maria Silva", null, null, null,
                    "11144477735", HOJE.minusYears(120), null, null, null);
            assertDoesNotThrow(() -> ValidadorPaciente.validar(requisicao, HOJE));
        }

        @Test
        @DisplayName("deve aceitar data de nascimento igual a hoje (recém-nascido)")
        void deveAceitarNascimentoHoje() {
            PacienteRequisicao requisicao = new PacienteRequisicao("Bebê Silva", null, null, null,
                    "11144477735", HOJE, null, null, null);
            assertDoesNotThrow(() -> ValidadorPaciente.validar(requisicao, HOJE));
        }
    }

    @Nested
    @DisplayName("Situação, e-mails e telefones")
    class DemaisRegras {

        @Test
        @DisplayName("deve recusar situação inexistente")
        void deveRecusarSituacaoInvalida() {
            PacienteRequisicao requisicao = new PacienteRequisicao("Maria Silva", null, null, null,
                    "11144477735", LocalDate.of(1990, 5, 20), null, "INTERNADO", null);
            assertMensagem(requisicao,
                    "Situação inválida. Valores aceitos: NORMAL, EM_OBSERVACAO, HOSPITALIZADO.");
        }

        @Test
        @DisplayName("deve aceitar situação em minúsculas (sem diferenciação de caixa)")
        void deveAceitarSituacaoMinuscula() {
            PacienteRequisicao requisicao = new PacienteRequisicao("Maria Silva", null, null, null,
                    "11144477735", LocalDate.of(1990, 5, 20), null, "hospitalizado", null);
            assertDoesNotThrow(() -> ValidadorPaciente.validar(requisicao, HOJE));
        }

        @Test
        @DisplayName("deve recusar e-mail com formato inválido")
        void deveRecusarEmailInvalido() {
            PacienteRequisicao requisicao = new PacienteRequisicao("Maria Silva", null, null, null,
                    "11144477735", LocalDate.of(1990, 5, 20), List.of("maria.mail.com"), null, null);
            assertThrows(DadosInvalidosException.class,
                    () -> ValidadorPaciente.validar(requisicao, HOJE));
        }

        @Test
        @DisplayName("deve recusar e-mail nulo na lista")
        void deveRecusarEmailNulo() {
            PacienteRequisicao requisicao = new PacienteRequisicao("Maria Silva", null, null, null,
                    "11144477735", LocalDate.of(1990, 5, 20),
                    java.util.Arrays.asList("ok@mail.com", null), null, null);
            assertMensagem(requisicao, "E-mail não pode ser vazio.");
        }

        @Test
        @DisplayName("deve recusar e-mail em branco")
        void deveRecusarEmailEmBranco() {
            PacienteRequisicao requisicao = new PacienteRequisicao("Maria Silva", null, null, null,
                    "11144477735", LocalDate.of(1990, 5, 20), List.of(" "), null, null);
            assertMensagem(requisicao, "E-mail não pode ser vazio.");
        }

        @Test
        @DisplayName("deve recusar DDD fora da faixa 11 a 99 (valor de borda)")
        void deveRecusarDddInvalido() {
            PacienteRequisicao requisicao = new PacienteRequisicao("Maria Silva", null,
                    List.of(new TelefoneDTO(10, 12345678L)), null, "11144477735",
                    LocalDate.of(1990, 5, 20), null, null, null);
            assertMensagem(requisicao, "DDD inválido: 10.");
        }

        @Test
        @DisplayName("deve aceitar DDD e número nos limites válidos")
        void deveAceitarDddNoLimite() {
            PacienteRequisicao requisicao = new PacienteRequisicao("Maria Silva", null,
                    List.of(new TelefoneDTO(99, 1L)), null, "11144477735",
                    LocalDate.of(1990, 5, 20), null, null, null);
            assertDoesNotThrow(() -> ValidadorPaciente.validar(requisicao, HOJE));
        }

        @Test
        @DisplayName("deve recusar telefone com número não positivo")
        void deveRecusarNumeroTelefoneInvalido() {
            PacienteRequisicao requisicao = new PacienteRequisicao("Maria Silva", null,
                    List.of(new TelefoneDTO(51, 0L)), null, "11144477735",
                    LocalDate.of(1990, 5, 20), null, null, null);
            assertMensagem(requisicao, "Número de telefone inválido: 0.");
        }

        @Test
        @DisplayName("deve recusar telefone nulo na lista")
        void deveRecusarTelefoneNulo() {
            PacienteRequisicao requisicao = new PacienteRequisicao("Maria Silva", null,
                    java.util.Collections.singletonList(null), null, "11144477735",
                    LocalDate.of(1990, 5, 20), null, null, null);
            assertMensagem(requisicao, "Telefone não pode ser nulo.");
        }
    }

    @Test
    @DisplayName("deve recusar corpo de requisição nulo")
    void deveRecusarRequisicaoNula() {
        List<String> erros = ValidadorPaciente.coletarErros(null, HOJE);
        assertEquals(List.of("O corpo da requisição é obrigatório."), erros);
    }

    @Test
    @DisplayName("deve acumular múltiplos erros em uma única validação")
    void deveAcumularMultiplosErros() {
        PacienteRequisicao requisicao = new PacienteRequisicao("", null, null, null, "000",
                null, null, "XYZ", null);
        DadosInvalidosException excecao = assertThrows(DadosInvalidosException.class,
                () -> ValidadorPaciente.validar(requisicao, HOJE));
        assertEquals(4, excecao.getDetalhes().size(),
                "Esperava-se 4 inconsistências, mas foram: " + excecao.getDetalhes());
    }

    @ParameterizedTest(name = "situação aceita: [{0}]")
    @MethodSource("situacoesValidas")
    @DisplayName("deve aceitar todas as situações previstas no domínio")
    void deveAceitarSituacoesDoDominio(String situacao) {
        PacienteRequisicao requisicao = new PacienteRequisicao("Maria Silva", null, null, null,
                "11144477735", LocalDate.of(1990, 5, 20), null, situacao, null);
        assertDoesNotThrow(() -> ValidadorPaciente.validar(requisicao, HOJE));
    }

    static Stream<Arguments> situacoesValidas() {
        return Stream.of("NORMAL", "EM_OBSERVACAO", "HOSPITALIZADO", null, "")
                .map(Arguments::of);
    }

    private static void assertMensagem(PacienteRequisicao requisicao, String mensagemEsperada) {
        DadosInvalidosException excecao = assertThrows(DadosInvalidosException.class,
                () -> ValidadorPaciente.validar(requisicao, HOJE));
        assertTrue(excecao.getDetalhes().contains(mensagemEsperada),
                "Mensagens obtidas: " + excecao.getDetalhes().stream()
                        .collect(Collectors.joining(" | ")));
    }
}
