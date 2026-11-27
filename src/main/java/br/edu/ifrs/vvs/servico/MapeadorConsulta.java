package br.edu.ifrs.vvs.servico;

import br.edu.ifrs.vvs.dominio.Consulta;
import br.edu.ifrs.vvs.dominio.Medico;
import br.edu.ifrs.vvs.dominio.Paciente;
import br.edu.ifrs.vvs.dto.ConsultaRequisicao;
import br.edu.ifrs.vvs.dto.ConsultaResposta;
import br.edu.ifrs.vvs.dto.MedicoRequisicao;
import br.edu.ifrs.vvs.dto.MedicoResposta;

/**
 * Conversão entre as entidades {@link Medico}/{@link Consulta} e os DTOs da API.
 */
public final class MapeadorConsulta {

    private MapeadorConsulta() {
        // Classe utilitária: não deve ser instanciada.
    }

    /**
     * Cria a entidade médico a partir da requisição.
     *
     * @param requisicao dados validados
     * @return entidade pronta para persistência
     */
    public static Medico paraEntidade(MedicoRequisicao requisicao) {
        return new Medico(ValidadorCrm.normalizar(requisicao.crm()), requisicao.especialidade().trim());
    }

    /**
     * Converte o médico em DTO de resposta.
     *
     * @param medico     entidade a ser convertida
     * @param totalConsultas quantidade de consultas do médico
     * @return DTO com os dados do médico
     */
    public static MedicoResposta paraResposta(Medico medico, long totalConsultas) {
        return new MedicoResposta(medico.getIdMedico(), medico.getCrm(),
                medico.getEspecialidade(), (int) totalConsultas);
    }

    /**
     * Cria uma nova consulta a partir da requisição e das entidades relacionadas.
     *
     * @param requisicao dados validados
     * @param medico     médico responsável
     * @param paciente   paciente atendido
     * @return entidade pronta para persistência
     */
    public static Consulta paraEntidade(ConsultaRequisicao requisicao, Medico medico, Paciente paciente) {
        Consulta consulta = new Consulta(requisicao.data(), requisicao.horario(), medico);
        consulta.setPaciente(paciente);
        return consulta;
    }

    /**
     * Converte a consulta em DTO de resposta.
     *
     * @param consulta entidade a ser convertida
     * @return DTO com os dados da consulta
     */
    public static ConsultaResposta paraResposta(Consulta consulta) {
        Medico medico = consulta.getMedico();
        Paciente paciente = consulta.getPaciente();
        return new ConsultaResposta(
                consulta.getIdConsulta(),
                consulta.getData(),
                consulta.getHorario(),
                medico == null ? null : medico.getIdMedico(),
                medico == null ? null : medico.getCrm() + " - " + medico.getEspecialidade(),
                paciente == null ? null : paciente.getIdPessoa(),
                paciente == null ? null : paciente.getNome());
    }
}
