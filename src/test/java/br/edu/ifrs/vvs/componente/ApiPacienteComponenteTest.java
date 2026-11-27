package br.edu.ifrs.vvs.componente;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de componente da API REST de pacientes.
 *
 * <p><b>Objetivo:</b> exercitar o contrato HTTP completo (serialização JSON, códigos
 * de status, cabeçalhos e mapeamento de exceções) com o banco H2 real. Os cenários
 * cobrem sucesso, erro de validação (400), recurso inexistente (404), conflito de
 * CPF (409) e casos de borda de filtros.</p>
 */
@QuarkusTest
@DisplayName("API REST de pacientes - testes de componente")
class ApiPacienteComponenteTest extends BaseComponenteTest {

    private static final String CAMINHO = "/api/pacientes";

    private static Map<String, Object> corpoValido(String cpf, String nome) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", nome);
        corpo.put("endereco", "Av. 24 de Outubro, 267");
        corpo.put("telefones", List.of(Map.of("ddd", 51, "numero", 987654321L)));
        corpo.put("convenio", "IPE");
        corpo.put("cpf", cpf);
        corpo.put("dataNascimento", "2002-01-08");
        corpo.put("emails", List.of("antonio@mail.com"));
        corpo.put("situacao", "EM_OBSERVACAO");
        corpo.put("prontuario", Map.of("data", "2026-06-01", "descricao", "Paciente estável"));
        return corpo;
    }

    private String criarPaciente(String cpf, String nome) {
        return given()
                .contentType(ContentType.JSON)
                .body(corpoValido(cpf, nome))
                .when().post(CAMINHO)
                .then()
                .statusCode(201)
                .extract().path("id").toString();
    }

    @Test
    @DisplayName("POST deve cadastrar paciente e retornar 201 com Location e corpo completo")
    void deveCadastrarPaciente() {
        given()
                .contentType(ContentType.JSON)
                .body(corpoValido("11144477735", "Antonio Carlos"))
                .when().post(CAMINHO)
                .then()
                .statusCode(201)
                .header("Location", containsString("/api/pacientes/"))
                .body("id", notNullValue())
                .body("nome", equalTo("Antonio Carlos"))
                .body("cpf", equalTo("11144477735"))
                .body("situacao", equalTo("EM_OBSERVACAO"))
                .body("situacaoDescricao", equalTo("Em observação"))
                .body("telefones", hasSize(1))
                .body("emails", hasSize(1))
                .body("prontuario.descricao", equalTo("Paciente estável"))
                .body("totalConsultas", equalTo(0));
    }

    @Test
    @DisplayName("POST deve aceitar CPF com máscara e normalizá-lo")
    void deveNormalizarCpfComMascara() {
        given()
                .contentType(ContentType.JSON)
                .body(corpoValido("111.444.777-35", "Maria Silva"))
                .when().post(CAMINHO)
                .then()
                .statusCode(201)
                .body("cpf", equalTo("11144477735"));
    }

    @Test
    @DisplayName("POST deve retornar 400 quando o nome é muito curto")
    void deveRecusarNomeInvalido() {
        given()
                .contentType(ContentType.JSON)
                .body(corpoValido("11144477735", "Jo"))
                .when().post(CAMINHO)
                .then()
                .statusCode(400)
                .body("erro", equalTo("Erro de validação"))
                .body("detalhes", everyItem(notNullValue()))
                .body("detalhes[0]", containsString("nome"));
    }

    @Test
    @DisplayName("POST deve retornar 400 quando o CPF é inválido")
    void deveRecusarCpfInvalido() {
        given()
                .contentType(ContentType.JSON)
                .body(corpoValido("12345678900", "Maria Silva"))
                .when().post(CAMINHO)
                .then()
                .statusCode(400)
                .body("detalhes[0]", containsString("CPF"));
    }

    @Test
    @DisplayName("POST deve retornar 409 quando o CPF já existe")
    void deveRecusarCpfDuplicado() {
        criarPaciente("11144477735", "Antonio Carlos");

        given()
                .contentType(ContentType.JSON)
                .body(corpoValido("11144477735", "Outro Nome"))
                .when().post(CAMINHO)
                .then()
                .statusCode(409)
                .body("erro", equalTo("Violação de regra de negócio"))
                .body("mensagem", containsString("111.444.777-35"));
    }

    @Test
    @DisplayName("POST deve retornar 400 para JSON malformado")
    void deveRecusarJsonMalformado() {
        given()
                .contentType(ContentType.JSON)
                .body("{ nome: invalido ")
                .when().post(CAMINHO)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("GET deve listar pacientes cadastrados")
    void deveListarPacientes() {
        criarPaciente("11144477735", "Antonio Carlos");

        given()
                .when().get(CAMINHO)
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("nome", org.hamcrest.Matchers.hasItem("Antonio Carlos"));
    }

    @Test
    @DisplayName("GET com filtro de nome deve retornar apenas correspondências (inclusive lista vazia)")
    void deveFiltrarPorNome() {
        criarPaciente("11144477735", "Antonio Carlos");

        given()
                .queryParam("nome", "Antonio")
                .when().get(CAMINHO)
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].nome", equalTo("Antonio Carlos"));

        given()
                .queryParam("nome", "Inexistente")
                .when().get(CAMINHO)
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("GET por ID deve retornar o paciente e 404 quando não existe")
    void deveBuscarPacientePorId() {
        String id = criarPaciente("11144477735", "Antonio Carlos");

        given()
                .when().get(CAMINHO + "/" + id)
                .then()
                .statusCode(200)
                .body("nome", equalTo("Antonio Carlos"));

        given()
                .when().get(CAMINHO + "/999999")
                .then()
                .statusCode(404)
                .body("erro", equalTo("Recurso não encontrado"))
                .body("mensagem", containsString("999999"));
    }

    @Test
    @DisplayName("PUT deve atualizar os dados do paciente")
    void deveAtualizarPaciente() {
        String id = criarPaciente("11144477735", "Antonio Carlos");
        Map<String, Object> atualizado = corpoValido("11144477735", "Antonio Carlos Junior");
        atualizado.put("situacao", "HOSPITALIZADO");
        atualizado.put("convenio", "UNIMED");

        given()
                .contentType(ContentType.JSON)
                .body(atualizado)
                .when().put(CAMINHO + "/" + id)
                .then()
                .statusCode(200)
                .body("nome", equalTo("Antonio Carlos Junior"))
                .body("situacao", equalTo("HOSPITALIZADO"))
                .body("convenio", equalTo("UNIMED"));
    }

    @Test
    @DisplayName("PUT deve retornar 404 para paciente inexistente")
    void deveRetornar404AoAtualizarInexistente() {
        given()
                .contentType(ContentType.JSON)
                .body(corpoValido("11144477735", "Maria Silva"))
                .when().put(CAMINHO + "/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("PUT deve retornar 409 ao tentar usar o CPF de outro paciente")
    void deveRetornar409AoTransferirCpf() {
        String idAntonio = criarPaciente("11144477735", "Antonio Carlos");
        criarPaciente("52998224725", "Maria Silva");

        Map<String, Object> corpo = corpoValido("52998224725", "Antonio Carlos");

        given()
                .contentType(ContentType.JSON)
                .body(corpo)
                .when().put(CAMINHO + "/" + idAntonio)
                .then()
                .statusCode(409)
                .body("mensagem", containsString("pertence a outro paciente"));
    }

    @Test
    @DisplayName("DELETE deve remover o paciente e retornar 204; segunda exclusão retorna 404")
    void deveExcluirPaciente() {
        String id = criarPaciente("11144477735", "Antonio Carlos");

        given()
                .when().delete(CAMINHO + "/" + id)
                .then()
                .statusCode(204);

        given()
                .when().get(CAMINHO + "/" + id)
                .then()
                .statusCode(404);

        given()
                .when().delete(CAMINHO + "/" + id)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("endpoint de saúde deve responder 200 com status OK")
    void deveResponderSaude() {
        given()
                .when().get("/api/saude")
                .then()
                .statusCode(200)
                .body("status", equalTo("OK"))
                .body("aplicacao", equalTo("Trabalho_VVS"));
    }
}
