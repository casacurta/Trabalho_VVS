package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifrs.vvs.servico.ValidadorCpf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes unitários de {@link ValidadorCpf}.
 *
 * <p><b>Objetivo:</b> verificar o algoritmo de validação dos dígitos verificadores,
 * a normalização e a formatação do CPF, cobrindo casos de sucesso, falha e exceção
 * de borda (entradas nulas, vazias, com letras e com dígitos repetidos).</p>
 */
@DisplayName("ValidadorCpf - regras de validação de CPF")
class ValidadorCpfTest {

    @Nested
    @DisplayName("Casos válidos")
    class CasosValidos {

        @Test
        @DisplayName("deve aceitar CPF válido sem formatação")
        void deveAceitarCpfValidoSemFormatacao() {
            assertTrue(ValidadorCpf.ehValido("11144477735"));
        }

        @Test
        @DisplayName("deve aceitar CPF válido com máscara")
        void deveAceitarCpfValidoComMascara() {
            assertTrue(ValidadorCpf.ehValido("111.444.777-35"));
        }

        @Test
        @DisplayName("deve ignorar caracteres não numéricos ao normalizar")
        void deveNormalizarRemovendoCaracteres() {
            assertEquals("11144477735", ValidadorCpf.normalizar("111.444.777-35"));
        }
    }

    @Nested
    @DisplayName("Casos inválidos")
    class CasosInvalidos {

        @ParameterizedTest(name = "CPF inválido: [{0}]")
        @ValueSource(strings = {
                "11144477736",   // segundo dígito verificador incorreto
                "11144477725",   // primeiro dígito verificador incorreto
                "12345678900",   // sequência numérica sem dígitos válidos
                "111",           // tamanho menor que 11
                "111444777350"   // tamanho maior que 11
        })
        @DisplayName("deve recusar CPFs com dígitos verificadores ou tamanho inválidos")
        void deveRecusarCpfInvalido(String cpf) {
            assertFalse(ValidadorCpf.ehValido(cpf));
        }

        @Test
        @DisplayName("deve recusar CPF nulo (entrada de borda)")
        void deveRecusarCpfNulo() {
            assertFalse(ValidadorCpf.ehValido(null));
            assertNull(ValidadorCpf.normalizar(null));
        }

        @Test
        @DisplayName("deve recusar CPF vazio (entrada de borda)")
        void deveRecusarCpfVazio() {
            assertFalse(ValidadorCpf.ehValido(""));
            assertFalse(ValidadorCpf.ehValido("       "));
        }

        @Test
        @DisplayName("deve recusar CPF com todos os dígitos iguais")
        void deveRecusarCpfComDigitosRepetidos() {
            assertFalse(ValidadorCpf.ehValido("11111111111"));
            assertFalse(ValidadorCpf.ehValido("00000000000"));
            assertFalse(ValidadorCpf.ehValido("99999999999"));
        }

        @Test
        @DisplayName("deve recusar CPF contendo letras que, normalizadas, ficam inválidas")
        void deveRecusarCpfComLetras() {
            assertFalse(ValidadorCpf.ehValido("111.444.777-3a"));
        }
    }

    @Nested
    @DisplayName("Formatação")
    class Formatacao {

        @Test
        @DisplayName("deve formatar CPF válido no padrão 000.000.000-00")
        void deveFormatarCpf() {
            assertEquals("111.444.777-35", ValidadorCpf.formatar("11144477735"));
        }

        @Test
        @DisplayName("deve devolver a entrada original quando não há 11 dígitos")
        void deveDevolverEntradaInalterada() {
            assertEquals("123", ValidadorCpf.formatar("123"));
            assertNull(ValidadorCpf.formatar(null));
        }
    }
}
