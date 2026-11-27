package br.edu.ifrs.vvs.componente;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de componente da API REST de médicos e consultas.
 *
 * <p><b>Objetivo:</b> verificar o contrato HTTP dos agendamentos, incluindo:
 * <ul>
 *   <li>cadastro de médico e validação de CRM (400/409);</li>
 *   <li>agendamento e reagendamento de consultas;</li>
 *   <li>conflito de agenda do médico e do paciente (409);</li>
 *   <li>horário fora do expediente e data no passado (400);</li>
 *   <li>filtros por paciente/médico e cancelamento.</li>
 * </ul>
 * Os CPFs e CRMs são gerados dinamicamente para evitar colisão entre execuções.</p>
 */
@QuarkusTest
@DisplayName("API REST de médicos e consultas - testes de componente")
class ApiConsultaComponenteTest extends BaseComponenteTest {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger(0);

    /** CPFs válidos usados nos cenários, sempre distintos. */
    private static final List<String> CPFS_VALIDOS = List.of(
            "11144477735", "52998224725", "16899535009", "01234567890", "39053344705");

    private static Map<String, Object> corpoMedico(String crm, String especialidade) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("crm", crm);
        corpo.put("especialidade", especialidade);
        return corpo;
    }

    private static Map<String, Object> corpoPaciente(String cpf, String nome) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", nome);
        corpo.put("cpf", cpf);
        corpo.put("dataNascimento", "1990-05-20");
        corpo.put("situacao", "NORMAL");
        return corpo;
    }

    private static Map<String, Object> corpoConsulta(String data, String horario, long idMedico, long idPaciente) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("data", data);
        corpo.put("horario", horario);
        corpo.put("idMedico", idMedico);
        corpo.put("idPaciente", idPaciente);
        return corpo;
    }

    private long criarMedico(String crm, String especialidade) {
        return given()
                .contentType(ContentType.JSON)
                .body(corpoMedico(crm, especialidade))
                .when().post("/api/medicos")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private long criarPaciente(String cpf, String nome) {
        return given()
                .contentType(ContentType.JSON)
                .body(corpoPaciente(cpf, nome))
                .when().post("/api/pacientes")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static String proximoCrm() {
        return String.format("%05d/RS", 30000 + SEQUENCIA.incrementAndGet());
    }

    @Test
    @DisplayName("POST /api/medicos deve cadastrar e retornar 201 com Location")
    void deveCadastrarMedico() {
        given()
                .contentType(ContentType.JSON)
                .body(corpoMedico("24016/RS", "Oncologista"))
                .when().post("/api/medicos")
                .then()
                .statusCode(201)
                .header("Location", containsString("/api/medicos/"))
                .body("crm", equalTo("24016/RS"))
                .body("especialidade", equalTo("Oncologista"))
                .body("totalConsultas", equalTo(0));
    }

    @Test
    @DisplayName("POST /api/medicos deve normalizar o CRM informado em minúsculas")
    void deveNormalizarCrm() {
        given()
                .contentType(ContentType.JSON)
                .body(corpoMedico(" 24016/rs ", "Oncologista"))
                .when().post("/api/medicos")
                .then()
                .statusCode(201)
                .body("crm", equalTo("24016/RS"));
    }

    @Test
    @DisplayName("POST /api/medicos deve retornar 400 para CRM inválido")
    void deveRecusarCrmInvalido() {
        given()
                .contentType(ContentType.JSON)
                .body(corpoMedico("invalido", "Oncologista"))
                .when().post("/api/medicos")
                .then()
                .statusCode(400)
                .body("detalhes[0]", containsString("CRM"));
    }

    @Test
    @DisplayName("POST /api/medicos deve retornar 409 para CRM duplicado")
    void deveRecusarCrmDuplicado() {
        criarMedico("18309/SC", "Ortopedista");

        given()
                .contentType(ContentType.JSON)
                .body(corpoMedico("18309/SC", "Outra Especialidade"))
                .when().post("/api/medicos")
                .then()
                .statusCode(409)
                .body("mensagem", containsString("18309/SC"));
    }

    @Test
    @DisplayName("GET /api/medicos deve filtrar por especialidade")
    void deveFiltrarMedicoPorEspecialidade() {
        String crm = proximoCrm();
        criarMedico(crm, "Dermatologista");

        given()
                .queryParam("especialidade", "Dermatologista")
                .when().get("/api/medicos")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].crm", equalTo(crm));
    }

    @Test
    @DisplayName("POST /api/consultas deve agendar consulta válida")
    void deveAgendarConsulta() {
        long medico = criarMedico(proximoCrm(), "Cardiologista");
        long paciente = criarPaciente("11144477735", "Antonio Carlos");

        given()
                .contentType(ContentType.JSON)
                .body(corpoConsulta("2030-05-20", "09:30", medico, paciente))
                .when().post("/api/consultas")
                .then()
                .statusCode(201)
                .header("Location", containsString("/api/consultas/"))
                .body("data", equalTo("2030-05-20"))
                .body("horario", equalTo("09:30:00"))
                .body("nomePaciente", equalTo("Antonio Carlos"));
    }

    @Test
    @DisplayName("POST /api/consultas deve retornar 409 quando o médico já tem consulta no horário")
    void deveRecusarConflitoDoMedico() {
        long medico = criarMedico(proximoCrm(), "Cardiologista");
        long paciente1 = criarPaciente("11144477735", "Antonio Carlos");
        long paciente2 = criarPaciente("52998224725", "Maria Silva");

        given().contentType(ContentType.JSON)
                .body(corpoConsulta("2030-06-20", "10:00", medico, paciente1))
                .when().post("/api/consultas").then().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(corpoConsulta("2030-06-20", "10:00", medico, paciente2))
                .when().post("/api/consultas")
                .then()
                .statusCode(409)
                .body("mensagem", containsString("médico"));
    }

    @Test
    @DisplayName("POST /api/consultas deve retornar 409 quando o paciente já tem consulta no horário")
    void deveRecusarConflitoDoPaciente() {
        long medico1 = criarMedico(proximoCrm(), "Cardiologista");
        long medico2 = criarMedico(proximoCrm(), "Ortopedista");
        long paciente = criarPaciente("11144477735", "Antonio Carlos");

        given().contentType(ContentType.JSON)
                .body(corpoConsulta("2030-07-20", "11:00", medico1, paciente))
                .when().post("/api/consultas").then().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(corpoConsulta("2030-07-20", "11:00", medico2, paciente))
                .when().post("/api/consultas")
                .then()
                .statusCode(409)
                .body("mensagem", containsString("paciente"));
    }

    @Test
    @DisplayName("POST /api/consultas deve retornar 400 para horário fora do expediente")
    void deveRecusarHorarioForaDoExpediente() {
        long medico = criarMedico(proximoCrm(), "Cardiologista");
        long paciente = criarPaciente("11144477735", "Antonio Carlos");

        given()
                .contentType(ContentType.JSON)
                .body(corpoConsulta("2030-08-20", "20:00", medico, paciente))
                .when().post("/api/consultas")
                .then()
                .statusCode(400)
                .body("detalhes[0]", containsString("18:00"));
    }

    @Test
    @DisplayName("POST /api/consultas deve retornar 400 para data no passado")
    void deveRecusarDataNoPassado() {
        long medico = criarMedico(proximoCrm(), "Cardiologista");
        long paciente = criarPaciente("11144477735", "Antonio Carlos");

        given()
                .contentType(ContentType.JSON)
                .body(corpoConsulta("2000-01-01", "09:00", medico, paciente))
                .when().post("/api/consultas")
                .then()
                .statusCode(400)
                .body("detalhes[0]", containsString("passado"));
    }

    @Test
    @DisplayName("POST /api/consultas deve retornar 404 quando o médico não existe")
    void deveRetornar404ParaMedicoInexistente() {
        long paciente = criarPaciente("11144477735", "Antonio Carlos");

        given()
                .contentType(ContentType.JSON)
                .body(corpoConsulta("2030-09-20", "09:00", 999999L, paciente))
                .when().post("/api/consultas")
                .then()
                .statusCode(404)
                .body("mensagem", containsString("Médico"));
    }

    @Test
    @DisplayName("POST /api/consultas deve retornar 404 quando o paciente não existe")
    void deveRetornar404ParaPacienteInexistente() {
        long medico = criarMedico(proximoCrm(), "Cardiologista");

        given()
                .contentType(ContentType.JSON)
                .body(corpoConsulta("2030-09-20", "09:00", medico, 999999L))
                .when().post("/api/consultas")
                .then()
                .statusCode(404)
                .body("mensagem", containsString("Paciente"));
    }

    @Test
    @DisplayName("GET /api/consultas deve filtrar por médico e por paciente")
    void deveFiltrarConsultas() {
        long medico = criarMedico(proximoCrm(), "Neurologista");
        long paciente = criarPaciente("11144477735", "Antonio Carlos");

        given().contentType(ContentType.JSON)
                .body(corpoConsulta("2030-10-20", "14:00", medico, paciente))
                .when().post("/api/consultas").then().statusCode(201);

        given()
                .queryParam("medicoId", medico)
                .when().get("/api/consultas")
                .then()
                .statusCode(200)
                .body("$", hasSize(1));

        given()
                .queryParam("pacienteId", paciente)
                .when().get("/api/consultas")
                .then()
                .statusCode(200)
                .body("[0].idPaciente", equalTo((int) paciente));
    }

    @Test
    @DisplayName("PUT /api/consultas deve reagendar e DELETE deve cancelar a consulta")
    void deveReagendarECancelarConsulta() {
        long medico = criarMedico(proximoCrm(), "Neurologista");
        long paciente = criarPaciente("11144477735", "Antonio Carlos");

        int idConsulta = given().contentType(ContentType.JSON)
                .body(corpoConsulta("2030-11-20", "14:00", medico, paciente))
                .when().post("/api/consultas").then().statusCode(201)
                .extract().jsonPath().getInt("id");

        given()
                .contentType(ContentType.JSON)
                .body(corpoConsulta("2030-11-21", "15:30", medico, paciente))
                .when().put("/api/consultas/" + idConsulta)
                .then()
                .statusCode(200)
                .body("data", equalTo("2030-11-21"))
                .body("horario", equalTo("15:30:00"));

        given()
                .when().delete("/api/consultas/" + idConsulta)
                .then()
                .statusCode(204);

        given()
                .when().get("/api/consultas/" + idConsulta)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("DELETE /api/medicos deve retornar 409 quando há consultas vinculadas")
    void deveImpedirExclusaoDeMedicoComConsulta() {
        long medico = criarMedico(proximoCrm(), "Neurologista");
        long paciente = criarPaciente("11144477735", "Antonio Carlos");

        given().contentType(ContentType.JSON)
                .body(corpoConsulta("2030-12-20", "08:30", medico, paciente))
                .when().post("/api/consultas").then().statusCode(201);

        given()
                .when().delete("/api/medicos/" + medico)
                .then()
                .statusCode(409)
                .body("mensagem", containsString("consulta"));
    }

    @Test
    @DisplayName("DELETE /api/medicos deve excluir médico sem consultas e retornar 204")
    void deveExcluirMedicoSemConsulta() {
        long medico = criarMedico(proximoCrm(), "Pneumologista");

        given()
                .when().delete("/api/medicos/" + medico)
                .then()
                .statusCode(204);

        given()
                .when().get("/api/medicos/" + medico)
                .then()
                .statusCode(404);
    }
}
