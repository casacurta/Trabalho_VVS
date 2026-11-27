package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import br.edu.ifrs.vvs.dominio.Consulta;
import br.edu.ifrs.vvs.dominio.Medico;
import br.edu.ifrs.vvs.dominio.Paciente;
import br.edu.ifrs.vvs.dto.ConsultaRequisicao;
import br.edu.ifrs.vvs.dto.ConsultaResposta;
import br.edu.ifrs.vvs.dto.MedicoRequisicao;
import br.edu.ifrs.vvs.dto.MedicoResposta;
import br.edu.ifrs.vvs.servico.MapeadorConsulta;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários de {@link MapeadorConsulta}.
 *
 * <p><b>Objetivo:</b> verificar a conversão de médicos e consultas, incluindo o
 * tratamento de relacionamentos ausentes (médico/paciente nulos na consulta).</p>
 */
@DisplayName("MapeadorConsulta - conversão de médicos e consultas")
class MapeadorConsultaTest {

    @Test
    @DisplayName("deve normalizar CRM e especialidade ao criar entidade Médico")
    void deveCriarMedicoNormalizado() {
        Medico medico = MapeadorConsulta.paraEntidade(new MedicoRequisicao(" 24016/rs ", " Oncologista "));

        assertEquals("24016/RS", medico.getCrm());
        assertEquals("Oncologista", medico.getEspecialidade());
    }

    @Test
    @DisplayName("deve converter médico em DTO com total de consultas")
    void deveConverterMedicoEmResposta() {
        Medico medico = new Medico("24016/RS", "Oncologista");
        medico.setIdMedico(2L);

        MedicoResposta resposta = MapeadorConsulta.paraResposta(medico, 3);

        assertEquals(2L, resposta.id());
        assertEquals("24016/RS", resposta.crm());
        assertEquals("Oncologista", resposta.especialidade());
        assertEquals(3, resposta.totalConsultas());
    }

    @Test
    @DisplayName("deve criar consulta com paciente vinculado")
    void deveCriarConsultaComPaciente() {
        Medico medico = new Medico("24016/RS", "Oncologista");
        Paciente paciente = new Paciente();

        Consulta consulta = MapeadorConsulta.paraEntidade(
                new ConsultaRequisicao(LocalDate.of(2030, 1, 10), LocalTime.of(10, 30), 1L, 1L),
                medico, paciente);

        assertEquals(LocalDate.of(2030, 1, 10), consulta.getData());
        assertEquals(LocalTime.of(10, 30), consulta.getHorario());
        assertEquals(medico, consulta.getMedico());
        assertEquals(paciente, consulta.getPaciente());
    }

    @Test
    @DisplayName("deve converter consulta em DTO com dados de médico e paciente")
    void deveConverterConsultaEmResposta() {
        Medico medico = new Medico("24016/RS", "Oncologista");
        medico.setIdMedico(5L);
        Paciente paciente = new Paciente();
        paciente.setIdPessoa(9L);
        paciente.setNome("Maria");
        Consulta consulta = new Consulta(LocalDate.of(2030, 1, 10), LocalTime.of(10, 30), medico);
        consulta.setIdConsulta(11L);
        consulta.setPaciente(paciente);

        ConsultaResposta resposta = MapeadorConsulta.paraResposta(consulta);

        assertEquals(11L, resposta.id());
        assertEquals(5L, resposta.idMedico());
        assertEquals("24016/RS - Oncologista", resposta.nomeMedico());
        assertEquals(9L, resposta.idPaciente());
        assertEquals("Maria", resposta.nomePaciente());
    }

    @Test
    @DisplayName("deve converter consulta sem médico e sem paciente sem lançar exceção")
    void deveConverterConsultaMinima() {
        ConsultaResposta resposta = MapeadorConsulta.paraResposta(new Consulta());

        assertEquals("[Data:  - ]", new Consulta().toString());
        assertNull(resposta.idMedico());
        assertNull(resposta.nomeMedico());
        assertNull(resposta.idPaciente());
        assertNull(resposta.nomePaciente());
    }
}
