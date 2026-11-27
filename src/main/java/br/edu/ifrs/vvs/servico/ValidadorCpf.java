package br.edu.ifrs.vvs.servico;

/**
 * Utilitários de validação e formatação de CPF.
 *
 * <p>O CPF é armazenado normalizado (somente dígitos) e validado pelos
 * dígitos verificadores. A classe é puramente funcional, o que facilita
 * os testes unitários com particionamento de equivalência e valores de borda.</p>
 */
public final class ValidadorCpf {

    private static final int TAMANHO_CPF = 11;

    private static final int[] PESOS_PRIMEIRO_DIGITO = {10, 9, 8, 7, 6, 5, 4, 3, 2};

    private static final int[] PESOS_SEGUNDO_DIGITO = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};

    private ValidadorCpf() {
        // Classe utilitária: não deve ser instanciada.
    }

    /**
     * Remove qualquer caractere que não seja dígito.
     *
     * @param cpf CPF possivelmente formatado (ex.: {@code 222.333.444-55})
     * @return CPF somente com dígitos ou {@code null} quando a entrada é nula
     */
    public static String normalizar(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    /**
     * Valida o CPF considerando tamanho, repetição de dígitos e dígitos verificadores.
     *
     * @param cpf CPF com ou sem formatação
     * @return {@code true} quando o CPF é válido
     */
    public static boolean ehValido(String cpf) {
        String normalizado = normalizar(cpf);
        if (normalizado == null || normalizado.length() != TAMANHO_CPF) {
            return false;
        }
        if (todosDigitosIguais(normalizado)) {
            return false;
        }
        int primeiroDigito = calcularDigito(normalizado, PESOS_PRIMEIRO_DIGITO);
        int segundoDigito = calcularDigito(normalizado, PESOS_SEGUNDO_DIGITO);
        return normalizado.charAt(9) - '0' == primeiroDigito
                && normalizado.charAt(10) - '0' == segundoDigito;
    }

    /**
     * Formata o CPF no padrão {@code 000.000.000-00}.
     *
     * @param cpf CPF com ou sem formatação
     * @return CPF formatado ou a própria entrada quando não possui 11 dígitos
     */
    public static String formatar(String cpf) {
        String normalizado = normalizar(cpf);
        if (normalizado == null || normalizado.length() != TAMANHO_CPF) {
            return cpf;
        }
        return normalizado.substring(0, 3) + "." + normalizado.substring(3, 6) + "."
                + normalizado.substring(6, 9) + "-" + normalizado.substring(9);
    }

    private static boolean todosDigitosIguais(String cpf) {
        for (int indice = 1; indice < cpf.length(); indice++) {
            if (cpf.charAt(indice) != cpf.charAt(0)) {
                return false;
            }
        }
        return true;
    }

    private static int calcularDigito(String cpf, int... pesos) {
        int soma = 0;
        for (int indice = 0; indice < pesos.length; indice++) {
            soma += (cpf.charAt(indice) - '0') * pesos[indice];
        }
        int resto = soma % TAMANHO_CPF;
        return resto < 2 ? 0 : TAMANHO_CPF - resto;
    }
}
