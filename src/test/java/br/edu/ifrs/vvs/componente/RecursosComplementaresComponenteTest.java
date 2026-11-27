package br.edu.ifrs.vvs.componente;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import br.edu.ifrs.vvs.dominio.Prontuario;
import br.edu.ifrs.vvs.repositorio.ConsultaRepositorio;
import br.edu.ifrs.vvs.repositorio.MedicoRepositorio;
import br.edu.ifrs.vvs.repositorio.PacienteRepositorio;
import br.edu.ifrs.vvs.repositorio.ProntuarioRepositorio;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de componente complementares.
 *
 * <p><b>Objetivo:</b> cobrir operações ainda não exercitadas e que fazem parte do
 * contrato público do sistema: consulta e atualização de médicos por identificador,
 * listagem geral de consultas, listagem de prontuários e contagens utilizadas pela
 * camada de serviço. Também valida caminhos de erro (recurso inexistente).</p>
 */
@QuarkusTest
@TestTransaction
@DisplayName("Componentes complementares - endpoints e repositórios")
class RecursosComplementaresComponenteTest extends BaseComponenteTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Inject
    PacienteRepositorio pacienteRepositorio;

    @Inject
    MedicoRepositorio medicoRepositorio;

    @Inject
    ConsultaRepositorio consultaRepositorio;

    @Inject
    ProntuarioRepositorio prontuarioRepositorio;

    private static String proximoCrm() {
        return String.format("%05d/RS", 50000 + SEQ.incrementAndGet());
    }

    private static Map<String, Object> pacienteJson(String cpf) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", "Paciente Complementar");
        corpo.put("cpf", cpf);
        corpo.put("dataNascimento", "1988-02-02");
        corpo.put("emails", List.of("complementar@mail.com"));
        corpo.put("telefones", List.of(Map.of("ddd", 51, "numero", 12345678L)));
        return corpo;
    }

    @Test
    @DisplayName("GET /api/medicos/{id} deve retornar o médico e 404 quando não existe")
    void deveBuscarMedicoPorId() {
        long id = given().contentType(ContentType.JSON)
                .body(Map.of("crm", proximoCrm(), "especialidade", "Clínica"))
                .when().post("/api/medicos").then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given().when().get("/api/medicos/" + id)
                .then().statusCode(200)
                .body("especialidade", equalTo("Clínica"))
                .body("totalConsultas", equalTo(0));

        given().when().get("/api/medicos/888888")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("PUT /api/medicos/{id} deve atualizar especialidade e recusar CRM de terceiros")
    void deveAtualizarMedico() {
        String crmA = proximoCrm();
        long idA = given().contentType(ContentType.JSON)
                .body(Map.of("crm", crmA, "especialidade", "Clínica"))
                .when().post("/api/medicos").then().statusCode(201)
                .extract().jsonPath().getLong("id");
        String crmB = proximoCrm();
        given().contentType(ContentType.JSON)
                .body(Map.of("crm", crmB, "especialidade", "Ortopedia"))
                .when().post("/api/medicos").then().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("crm", crmA, "especialidade", "Cardiologia"))
                .when().put("/api/medicos/" + idA)
                .then().statusCode(200)
                .body("especialidade", equalTo("Cardiologia"));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("crm", crmB, "especialidade", "Cardiologia"))
                .when().put("/api/medicos/" + idA)
                .then().statusCode(409);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("crm", proximoCrm(), "especialidade", "Clínica"))
                .when().put("/api/medicos/777777")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("GET /api/consultas deve listar todas e retornar lista vazia sem agendamentos")
    void deveListarTodasConsultas() {
        given().when().get("/api/consultas")
                .then().statusCode(200).body("$", hasSize(0));

        long medico = given().contentType(ContentType.JSON)
                .body(Map.of("crm", proximoCrm(), "especialidade", "Clínica"))
                .when().post("/api/medicos").then().statusCode(201)
                .extract().jsonPath().getLong("id");
        long paciente = given().contentType(ContentType.JSON)
                .body(pacienteJson("11144477735"))
                .when().post("/api/pacientes").then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given().contentType(ContentType.JSON)
                .body(Map.of("data", "2030-03-03", "horario", "16:00",
                        "idMedico", medico, "idPaciente", paciente))
                .when().post("/api/consultas").then().statusCode(201);

        given().when().get("/api/consultas")
                .then().statusCode(200).body("$", hasSize(1));
    }

    @Test
    @DisplayName("GET /api/consultas/{id} deve retornar 404 para consulta inexistente")
    void deveRetornar404Consulta() {
        given().when().get("/api/consultas/123456").then().statusCode(404);
    }

    @Test
    @DisplayName("GET /api/pacientes com filtro em branco deve listar todos")
    void deveIgnorarFiltroEmBranco() {
        given().contentType(ContentType.JSON).body(pacienteJson("11144477735"))
                .when().post("/api/pacientes").then().statusCode(201);

        given().queryParam("nome", "   ")
                .when().get("/api/pacientes")
                .then().statusCode(200).body("$", hasSize(1));
    }

    @Test
    @DisplayName("repositórios devem listar prontuários e contabilizar registros")
    void repositoriosDevemListarEContar() {
        Prontuario prontuario = new Prontuario(LocalDate.of(2024, 4, 4), "Avaliação");
        prontuarioRepositorio.salvar(prontuario);

        assertEquals(1, prontuarioRepositorio.listarTodos().size());
        assertEquals("Avaliação", prontuarioRepositorio.buscarPorId(
                prontuario.getIdProntuario()).orElseThrow().getDescricao());
        assertEquals(0L, pacienteRepositorio.contar());
        assertEquals(0L, medicoRepositorio.contar());
        assertTrue(consultaRepositorio.listarTodas().isEmpty());
    }

    private static void assertEquals(Object esperado, Object obtido) {
        org.junit.jupiter.api.Assertions.assertEquals(esperado, obtido);
    }

    private static void assertTrue(boolean condicao) {
        org.junit.jupiter.api.Assertions.assertTrue(condicao);
    }
}
