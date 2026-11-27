package br.edu.ifrs.vvs.servico;

import br.edu.ifrs.vvs.dto.ConsultaRequisicao;
import br.edu.ifrs.vvs.excecao.DadosInvalidosException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Validação dos dados de agendamento de consulta.
 */
public final class ValidadorConsulta {

    /** Horário de abertura da clínica. */
    public static final LocalTime ABERTURA = LocalTime.of(8, 0);

    /** Horário de fechamento da clínica (exclusivo). */
    public static final LocalTime FECHAMENTO = LocalTime.of(18, 0);

    private ValidadorConsulta() {
        // Classe utilitária: não deve ser instanciada.
    }

    /**
     * Valida a requisição de consulta e lança {@link DadosInvalidosException} caso haja erros.
     *
     * @param requisicao dados enviados pelo cliente
     * @param agora      instante de referência (injetado para testes determinísticos)
     */
    public static void validar(ConsultaRequisicao requisicao, LocalDateTime agora) {
        List<String> erros = coletarErros(requisicao, agora);
        if (!erros.isEmpty()) {
            throw new DadosInvalidosException(erros);
        }
    }

    /**
     * Coleta todas as inconsistências dos dados de consulta.
     *
     * @param requisicao dados enviados pelo cliente
     * @param agora      instante de referência para verificar agendamentos no passado
     * @return lista de mensagens de erro
     */
    public static List<String> coletarErros(ConsultaRequisicao requisicao, LocalDateTime agora) {
        List<String> erros = new ArrayList<>();
        if (requisicao == null) {
            erros.add("O corpo da requisição é obrigatório.");
            return erros;
        }
        if (requisicao.idMedico() == null) {
            erros.add("O médico é obrigatório.");
        }
        if (requisicao.idPaciente() == null) {
            erros.add("O paciente é obrigatório.");
        }
        if (requisicao.data() == null) {
            erros.add("A data da consulta é obrigatória.");
        }
        if (requisicao.horario() == null) {
            erros.add("O horário da consulta é obrigatório.");
        }
        if (requisicao.data() != null && requisicao.horario() != null) {
            validarAgendamento(requisicao.data(), requisicao.horario(), agora, erros);
        }
        return erros;
    }

    private static void validarAgendamento(LocalDate data, LocalTime horario,
                                           LocalDateTime agora, List<String> erros) {
        LocalDateTime momentoConsulta = LocalDateTime.of(data, horario);
        if (momentoConsulta.isBefore(agora)) {
            erros.add("Não é permitido agendar consulta para data/horário no passado.");
        }
        if (horario.isBefore(ABERTURA) || !horario.isBefore(FECHAMENTO)) {
            erros.add("O horário deve estar entre " + ABERTURA + " e " + FECHAMENTO + ".");
        }
    }
}
