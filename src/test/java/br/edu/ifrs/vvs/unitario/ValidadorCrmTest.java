package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifrs.vvs.servico.ValidadorCrm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários de {@link ValidadorCrm}.
 *
 * <p><b>Objetivo:</b> garantir que o formato {@code 12345/UF} seja aceito e que
 * variações inválidas (sem barra, UF minúscula, poucos dígitos, nulo) sejam recusadas.</p>
 */
@DisplayName("ValidadorCrm - formato do registro CRM")
class ValidadorCrmTest {

    @ParameterizedTest(name = "CRM aceito: [{0}]")
    @ValueSource(strings = {"24016/RS", "18309/SC", "04985/SP", "1234/DF"})
    @DisplayName("deve aceitar CRM no padrão dígitos/UF")
    void deveAceitarCrmValido(String crm) {
        assertTrue(ValidadorCrm.ehValido(crm));
    }

    @ParameterizedTest(name = "CRM recusado: [{0}]")
    @ValueSource(strings = {"24016", "24016/", "123/RS", "1234567/RS", "24016/RSS", "24A16/RS", "24016-RS"})
    @DisplayName("deve recusar CRM fora do padrão")
    void deveRecusarCrmInvalido(String crm) {
        assertFalse(ValidadorCrm.ehValido(crm));
    }

    @Test
    @DisplayName("deve aceitar UF em minúsculas porque a normalização converte para maiúsculas")
    void deveAceitarUfMinuscula() {
        assertTrue(ValidadorCrm.ehValido("24016/rs"));
    }

    @Test
    @DisplayName("deve tratar entrada nula como inválida")
    void deveRecusarCrmNulo() {
        assertFalse(ValidadorCrm.ehValido(null));
        assertNull(ValidadorCrm.normalizar(null));
    }

    @Test
    @DisplayName("deve normalizar removendo espaços e convertendo a UF para maiúsculas")
    void deveNormalizarCrm() {
        assertEquals("24016/RS", ValidadorCrm.normalizar("  24016/rs  "));
    }

    @Test
    @DisplayName("deve aceitar CRM com espaços ao redor após normalização")
    void deveAceitarCrmComEspacos() {
        assertTrue(ValidadorCrm.ehValido(" 18309/sc "));
    }
}
