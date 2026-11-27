package br.edu.ifrs.vvs.servico;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validação do registro CRM no formato {@code 12345/UF}.
 *
 * <p>Formato aceito: de 4 a 6 dígitos, seguidos de barra e da sigla do estado
 * com duas letras maiúsculas (ex.: {@code 24016/RS}).</p>
 */
public final class ValidadorCrm {

    private static final Pattern FORMATO_CRM = Pattern.compile("^\\d{4,6}/[A-Z]{2}$");

    private ValidadorCrm() {
        // Classe utilitária: não deve ser instanciada.
    }

    /**
     * @param crm registro a ser normalizado
     * @return CRM em maiúsculas e sem espaços nas extremidades; {@code null} se a entrada for nula
     */
    public static String normalizar(String crm) {
        return crm == null ? null : crm.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Informa se o CRM está no formato esperado.
     *
     * @param crm registro com ou sem espaços
     * @return {@code true} quando o formato é válido
     */
    public static boolean ehValido(String crm) {
        String normalizado = normalizar(crm);
        return normalizado != null && FORMATO_CRM.matcher(normalizado).matches();
    }
}
