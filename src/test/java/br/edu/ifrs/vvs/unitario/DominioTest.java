package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifrs.vvs.dominio.Consulta;
import br.edu.ifrs.vvs.dominio.Medico;
import br.edu.ifrs.vvs.dominio.Paciente;
import br.edu.ifrs.vvs.dominio.Pessoa;
import br.edu.ifrs.vvs.dominio.Prontuario;
import br.edu.ifrs.vvs.dominio.Situacao;
import br.edu.ifrs.vvs.dominio.Telefone;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários das entidades de domínio.
 *
 * <p><b>Objetivo:</b> validar comportamentos das entidades que não dependem do banco:
 * conversão de situações, consistência de relacionamentos bidirecionais, listas
 * sempre inicializadas e formatação de {@code toString()} com dados ausentes.</p>
 */
@DisplayName("Domínio - comportamentos das entidades")
class DominioTest {

    @Nested
    @DisplayName("Situacao")
    class SituacaoTestes {

        @Test
        @DisplayName("deve converter texto para enum sem diferenciar maiúsculas")
        void deveConverterTexto() {
            assertEquals(Situacao.NORMAL, Situacao.de("normal"));
            assertEquals(Situacao.HOSPITALIZADO, Situacao.de("  HOSPITALIZADO  "));
        }

        @Test
        @DisplayName("deve retornar nulo para texto inválido, vazio ou nulo")
        void deveRetornarNulo() {
            assertNull(Situacao.de("internado"));
            assertNull(Situacao.de(""));
            assertNull(Situacao.de("   "));
            assertNull(Situacao.de(null));
        }

        @Test
        @DisplayName("deve expor descrição e identificador de cada situação")
        void deveExporMetadados() {
            assertEquals("Em observação", Situacao.EM_OBSERVACAO.getDescricao());
            assertEquals(3, Situacao.HOSPITALIZADO.getId());
        }
    }

    @Nested
    @DisplayName("Pessoa e Paciente")
    class PessoaEPaciente {

        @Test
        @DisplayName("deve inicializar listas vazias por padrão evitando NullPointerException")
        void deveInicializarListas() {
            Pessoa pessoa = new Pessoa();
            Paciente paciente = new Paciente();
            Medico medico = new Medico();

            assertNotNull(pessoa.getTelefones());
            assertNotNull(paciente.getEmails());
            assertNotNull(paciente.getConsultas());
            assertNotNull(medico.getConsultas());
        }

        @Test
        @DisplayName("deve tratar nulos ao definir listas e telefones")
        void deveTratarNulos() {
            Pessoa pessoa = new Pessoa();
            pessoa.setTelefones(null);
            pessoa.adicionarTelefone(null);
            pessoa.adicionarTelefone(new Telefone(51, 1234L));

            Paciente paciente = new Paciente();
            paciente.setEmails(null);
            paciente.setConsultas(null);
            paciente.adicionarConsulta(null);

            assertEquals(1, pessoa.getTelefones().size());
            assertTrue(paciente.getEmails().isEmpty());
            assertTrue(paciente.getConsultas().isEmpty());
        }

        @Test
        @DisplayName("adicionarConsulta deve manter ambos os lados da relação consistentes")
        void deveVincularConsultaAoPaciente() {
            Paciente paciente = new Paciente();
            Consulta consulta = new Consulta(LocalDate.of(2030, 1, 1), LocalTime.of(9, 0),
                    new Medico("24016/RS", "Oncologista"));

            paciente.adicionarConsulta(consulta);

            assertEquals(paciente, consulta.getPaciente());
            assertEquals(1, paciente.getConsultas().size());
        }

        @Test
        @DisplayName("construtor de Paciente deve copiar a lista de telefones (defensive copy)")
        void deveCopiarListaDeTelefones() {
            List<Telefone> origem = new ArrayList<>();
            origem.add(new Telefone(51, 111L));
            Paciente paciente = new Paciente("Maria", "Rua A", origem, "SUS", "11144477735",
                    LocalDate.of(1990, 1, 1), List.of("m@mail.com"), Situacao.NORMAL, null);

            origem.add(new Telefone(51, 222L));

            assertEquals(1, paciente.getTelefones().size());
        }

        @Test
        @DisplayName("toString de Pessoa sem telefones deve indicar ausência")
        void deveIndicarSemTelefone() {
            assertTrue(new Pessoa("Maria", "Rua A", null).toString().contains("Sem telefone"));
        }

        @Test
        @DisplayName("toString completo de Paciente deve conter todos os campos formatados")
        void deveFormatarPacienteCompleto() {
            Paciente paciente = new Paciente("Maria", "Rua A", List.of(new Telefone(51, 1234L)),
                    "SUS", "11144477735", LocalDate.of(1990, 5, 20), List.of("m@mail.com"),
                    Situacao.HOSPITALIZADO, new Prontuario(LocalDate.of(2024, 1, 1), "Febre"));
            paciente.adicionarConsulta(new Consulta(LocalDate.of(2030, 1, 1), LocalTime.of(9, 0),
                    new Medico("24016/RS", "Oncologista")));

            String texto = paciente.toString();

            assertTrue(texto.contains("20/05/1990"));
            assertTrue(texto.contains("Hospitalizado"));
            assertTrue(texto.contains("Febre"));
            assertTrue(texto.contains("m@mail.com"));
            assertTrue(texto.contains("(51) 1234"));
        }

        @Test
        @DisplayName("toString de Paciente sem dados opcionais deve indicar ausências")
        void deveFormatarPacienteVazio() {
            Paciente paciente = new Paciente();
            String texto = paciente.toString();

            assertTrue(texto.contains("Sem e-mail"));
            assertTrue(texto.contains("Sem consultas"));
            assertTrue(texto.contains("Nascimento = "));
        }
    }

    @Nested
    @DisplayName("Telefone, Consulta e Prontuario")
    class OutrasEntidades {

        @Test
        @DisplayName("toString de Telefone deve seguir o formato (DDD) número")
        void deveFormatarTelefone() {
            assertEquals("(51) 987654321", new Telefone(51, 987654321L).toString());
        }

        @Test
        @DisplayName("toString de Consulta deve conter data, hora e médico")
        void deveFormatarConsulta() {
            Medico medico = new Medico("24016/RS", "Oncologista");
            Consulta consulta = new Consulta(LocalDate.of(2030, 5, 20), LocalTime.of(14, 30), medico);

            String texto = consulta.toString();

            assertTrue(texto.contains("20/05/2030"));
            assertTrue(texto.contains("14:30"));
            assertTrue(texto.contains("24016/RS"));
        }

        @Test
        @DisplayName("toString de Consulta vazia não deve lançar exceção")
        void deveFormatarConsultaVazia() {
            assertEquals("[Data:  - ]", new Consulta().toString());
        }

        @Test
        @DisplayName("toString de Prontuario deve formatar a data e a descrição")
        void deveFormatarProntuario() {
            String texto = new Prontuario(LocalDate.of(2024, 3, 10), "Estável").toString();

            assertTrue(texto.contains("10/03/2024"));
            assertTrue(texto.contains("Estável"));
        }

        @Test
        @DisplayName("toString de Prontuario sem data deve exibir campo de data vazio")
        void deveFormatarProntuarioSemData() {
            String texto = new Prontuario().toString();

            assertTrue(texto.contains("Prontuario ()"),
                    "Data ausente deveria ser exibida como vazio, mas foi: " + texto);
        }

        @Test
        @DisplayName("toString de Medico deve conter CRM e especialidade")
        void deveFormatarMedico() {
            String texto = new Medico("24016/RS", "Oncologista").toString();

            assertTrue(texto.contains("24016/RS"));
            assertTrue(texto.contains("Oncologista"));
        }
    }
}
