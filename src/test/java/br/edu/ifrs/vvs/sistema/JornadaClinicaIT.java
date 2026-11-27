package br.edu.ifrs.vvs.sistema;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.ifrs.vvs.componente.BaseComponenteTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de sistema (ponta a ponta) da jornada clínica.
 *
 * <p><b>Objetivo:</b> validar o comportamento do sistema como um todo, do ponto de
 * vista de quem o utiliza, exercitando a cadeia completa HTTP → validação → serviço
 * → persistência → resposta, sem conhecer detalhes internos.</p>
 *
 * <p>Os fluxos reproduzem a rotina real de uma clínica:
 * <ol>
 *   <li>cadastro do médico e do paciente;</li>
 *   <li>agendamento da consulta;</li>
 *   <li>consulta da agenda por paciente e por médico;</li>
 *   <li>atualização do quadro clínico (prontuário e situação);</li>
 *   <li>cancelamento da consulta;</li>
 *   <li>exclusão dos cadastros;</li>
 * </ol>
 * e também os fluxos alternativos de erro (agenda ocupada, dados inválidos,
 * entidades inexistentes).</p>
 *
 * <p>Executados pelo Failsafe na fase {@code verify} (sufixo {@code IT}).</p>
 */
@QuarkusTest
@DisplayName("Sistema - jornada clínica ponta a ponta")
class JornadaClinicaIT extends BaseComponenteTest {

    private static final String MEDICOS = "/api/medicos";

    private static final String PACIENTES = "/api/pacientes";

    private static final String CONSULTAS = "/api/consultas";

    private static Map<String, Object> medico(String crm, String especialidade) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("crm", crm);
        corpo.put("especialidade", especialidade);
        return corpo;
    }

    private static Map<String, Object> paciente(String nome, String cpf, String convenio) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", nome);
        corpo.put("endereco", "Rua das Acácias, 55");
        corpo.put("telefones", List.of(Map.of("ddd", 51, "numero", 33334444L)));
        corpo.put("convenio", convenio);
        corpo.put("cpf", cpf);
        corpo.put("dataNascimento", "1985-03-12");
        corpo.put("emails", List.of("paciente@mail.com"));
        corpo.put("situacao", "NORMAL");
        corpo.put("prontuario", Map.of("data", LocalDate.now().toString(),
                "descricao", "Primeira avaliação"));
        return corpo;
    }

    private static Map<String, Object> consulta(String data, String horario, long idMedico, long idPaciente) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("data", data);
        corpo.put("horario", horario);
        corpo.put("idMedico", idMedico);
        corpo.put("idPaciente", idPaciente);
        return corpo;
    }

    @Test
    @DisplayName("Fluxo principal: cadastrar médico e paciente, agendar, consultar agenda, atualizar prontuário e cancelar")
    void deveExecutarJornadaCompleta() {
        // 1) Recepção cadastra o médico
        int idMedico = given()
                .contentType(ContentType.JSON)
                .body(medico("24016/RS", "Cardiologista"))
                .when().post(MEDICOS)
                .then().statusCode(201)
                .extract().jsonPath().getInt("id");

        // 2) Recepção cadastra o paciente (o sistema calcula a idade automaticamente)
        JsonPath pacienteCriado = given()
                .contentType(ContentType.JSON)
                .body(paciente("João da Silva", "11144477735", "UNIMED"))
                .when().post(PACIENTES)
                .then()
                .statusCode(201)
                .body("nome", equalTo("João da Silva"))
                .body("situacaoDescricao", equalTo("Normal"))
                .body("totalConsultas", equalTo(0))
                .extract().jsonPath();
        int idPaciente = pacienteCriado.getInt("id");

        // 3) Agendamento da consulta
        int idConsulta = given()
                .contentType(ContentType.JSON)
                .body(consulta("2030-04-15", "09:00", idMedico, idPaciente))
                .when().post(CONSULTAS)
                .then()
                .statusCode(201)
                .body("nomePaciente", equalTo("João da Silva"))
                .body("nomeMedico", equalTo("24016/RS - Cardiologista"))
                .extract().jsonPath().getInt("id");

        // 4) Agenda do paciente e do médico refletem a consulta
        given().queryParam("pacienteId", idPaciente)
                .when().get(CONSULTAS)
                .then().statusCode(200).body("$", hasSize(1));

        given().queryParam("medicoId", idMedico)
                .when().get(CONSULTAS)
                .then().statusCode(200).body("$", hasSize(1));

        // 5) Atendimento: quadro clínico é atualizado
        Map<String, Object> pacienteAtualizado = paciente("João da Silva", "11144477735", "UNIMED");
        pacienteAtualizado.put("situacao", "EM_OBSERVACAO");
        pacienteAtualizado.put("prontuario", Map.of("data", LocalDate.now().toString(),
                "descricao", "Paciente apresenta arritmia leve"));

        given()
                .contentType(ContentType.JSON)
                .body(pacienteAtualizado)
                .when().put(PACIENTES + "/" + idPaciente)
                .then()
                .statusCode(200)
                .body("situacao", equalTo("EM_OBSERVACAO"))
                .body("prontuario.descricao", equalTo("Paciente apresenta arritmia leve"))
                .body("totalConsultas", equalTo(1));

        // 6) Cancelamento da consulta pelo paciente
        given().when().delete(CONSULTAS + "/" + idConsulta).then().statusCode(204);
        given().when().get(CONSULTAS + "/" + idConsulta).then().statusCode(404);

        // 7) Encerramento do cadastro
        given().when().delete(PACIENTES + "/" + idPaciente).then().statusCode(204);
        given().when().delete(MEDICOS + "/" + idMedico).then().statusCode(204);
        given().when().get(PACIENTES + "/" + idPaciente).then().statusCode(404);
    }

    @Test
    @DisplayName("Fluxo alternativo: não é possível agendar dois pacientes no mesmo horário do médico")
    void deveRecusarAgendaOcupada() {
        int idMedico = given().contentType(ContentType.JSON)
                .body(medico("18309/SC", "Ortopedista"))
                .when().post(MEDICOS).then().statusCode(201)
                .extract().jsonPath().getInt("id");

        int pacienteA = given().contentType(ContentType.JSON)
                .body(paciente("Maria Souza", "11144477735", "SUS"))
                .when().post(PACIENTES).then().statusCode(201)
                .extract().jsonPath().getInt("id");

        int pacienteB = given().contentType(ContentType.JSON)
                .body(paciente("Carlos Pereira", "52998224725", "AMIL"))
                .when().post(PACIENTES).then().statusCode(201)
                .extract().jsonPath().getInt("id");

        given().contentType(ContentType.JSON)
                .body(consulta("2030-05-05", "10:30", idMedico, pacienteA))
                .when().post(CONSULTAS).then().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(consulta("2030-05-05", "10:30", idMedico, pacienteB))
                .when().post(CONSULTAS)
                .then()
                .statusCode(409)
                .body("mensagem", containsString("médico já possui consulta"));
    }

    @Test
    @DisplayName("Fluxo alternativo: o mesmo paciente não pode ter duas consultas no mesmo horário")
    void deveRecusarAgendaDuplicadaDoPaciente() {
        int medicoA = given().contentType(ContentType.JSON)
                .body(medico("04985/SP", "Clínico Geral"))
                .when().post(MEDICOS).then().statusCode(201)
                .extract().jsonPath().getInt("id");

        int medicoB = given().contentType(ContentType.JSON)
                .body(medico("29840/PR", "Pneumologista"))
                .when().post(MEDICOS).then().statusCode(201)
                .extract().jsonPath().getInt("id");

        int idPaciente = given().contentType(ContentType.JSON)
                .body(paciente("Ana Beatriz", "16899535009", "IPE"))
                .when().post(PACIENTES).then().statusCode(201)
                .extract().jsonPath().getInt("id");

        given().contentType(ContentType.JSON)
                .body(consulta("2030-06-06", "15:00", medicoA, idPaciente))
                .when().post(CONSULTAS).then().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(consulta("2030-06-06", "15:00", medicoB, idPaciente))
                .when().post(CONSULTAS)
                .then()
                .statusCode(409)
                .body("mensagem", containsString("paciente já possui consulta"));
    }

    @Test
    @DisplayName("Fluxo alternativo: cadastro com CPF inválido é rejeitado com lista de inconsistências")
    void deveRejeitarCadastroInvalidoComDetalhes() {
        Map<String, Object> invalido = paciente("A", "12345678900", null);
        invalido.put("emails", List.of("sem-arroba"));
        invalido.put("dataNascimento", LocalDate.now().plusDays(1).toString());

        JsonPath erro = given()
                .contentType(ContentType.JSON)
                .body(invalido)
                .when().post(PACIENTES)
                .then()
                .statusCode(400)
                .body("erro", equalTo("Erro de validação"))
                .extract().jsonPath();

        assertEquals(4, erro.getList("detalhes").size(),
                "Esperava-se nome, CPF, data de nascimento e e-mail inválidos: " + erro.getList("detalhes"));
    }

    @Test
    @DisplayName("Fluxo alternativo: agendamento para data no passado é recusado")
    void deveRecusarAgendamentoNoPassado() {
        int idMedico = given().contentType(ContentType.JSON)
                .body(medico("19849/RS", "Neurologista"))
                .when().post(MEDICOS).then().statusCode(201)
                .extract().jsonPath().getInt("id");

        int idPaciente = given().contentType(ContentType.JSON)
                .body(paciente("Pedro Alves", "39053344705", "SUS"))
                .when().post(PACIENTES).then().statusCode(201)
                .extract().jsonPath().getInt("id");

        given()
                .contentType(ContentType.JSON)
                .body(consulta("2010-01-10", "08:00", idMedico, idPaciente))
                .when().post(CONSULTAS)
                .then()
                .statusCode(400)
                .body("detalhes[0]", containsString("passado"));
    }

    @Test
    @DisplayName("Fluxo alternativo: paciente inexistente retorna 404 em todas as operações")
    void deveRetornar404ParaPacienteInexistente() {
        given().when().get(PACIENTES + "/424242").then().statusCode(404);
        given().contentType(ContentType.JSON)
                .body(paciente("Fantasma", "11144477735", "SUS"))
                .when().put(PACIENTES + "/424242").then().statusCode(404);
        given().when().delete(PACIENTES + "/424242").then().statusCode(404);
    }

    @Test
    @DisplayName("Fluxo alternativo: médico com consulta agendada não pode ser excluído")
    void deveImpedirExclusaoDeMedicoEmUso() {
        int idMedico = given().contentType(ContentType.JSON)
                .body(medico("03498/RS", "Dermatologista"))
                .when().post(MEDICOS).then().statusCode(201)
                .extract().jsonPath().getInt("id");

        int idPaciente = given().contentType(ContentType.JSON)
                .body(paciente("Lúcia Ferreira", "01234567890", "Bradesco"))
                .when().post(PACIENTES).then().statusCode(201)
                .extract().jsonPath().getInt("id");

        given().contentType(ContentType.JSON)
                .body(consulta("2030-07-07", "11:30", idMedico, idPaciente))
                .when().post(CONSULTAS).then().statusCode(201);

        given()
                .when().delete(MEDICOS + "/" + idMedico)
                .then()
                .statusCode(409)
                .body("mensagem", containsString("consulta"));
    }
}
