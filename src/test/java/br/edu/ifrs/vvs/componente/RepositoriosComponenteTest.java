package br.edu.ifrs.vvs.componente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifrs.vvs.dominio.Consulta;
import br.edu.ifrs.vvs.dominio.Medico;
import br.edu.ifrs.vvs.dominio.Paciente;
import br.edu.ifrs.vvs.dominio.Prontuario;
import br.edu.ifrs.vvs.dominio.Situacao;
import br.edu.ifrs.vvs.dominio.Telefone;
import br.edu.ifrs.vvs.repositorio.ConsultaRepositorio;
import br.edu.ifrs.vvs.repositorio.MedicoRepositorio;
import br.edu.ifrs.vvs.repositorio.PacienteRepositorio;
import br.edu.ifrs.vvs.repositorio.ProntuarioRepositorio;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de componente dos repositórios.
 *
 * <p><b>Objetivo:</b> validar a camada de persistência de forma integrada
 * (JPA + Hibernate + banco H2 em memória + CDI do Quarkus), sem mocks. Cada teste
 * roda em uma transação revertida ao final ({@link TestTransaction}), garantindo
 * isolamento entre casos.</p>
 *
 * <p>Os cenários cobrem: inclusão, consulta, atualização, exclusão, consultas
 * derivadas (busca por CPF, CRM, nome, especialidade) e regras de contagem usadas
 * pela camada de serviço.</p>
 */
@QuarkusTest
@TestTransaction
@DisplayName("Repositórios - integração com banco H2")
class RepositoriosComponenteTest extends BaseComponenteTest {

    @Inject
    PacienteRepositorio pacienteRepositorio;

    @Inject
    MedicoRepositorio medicoRepositorio;

    @Inject
    ConsultaRepositorio consultaRepositorio;

    @Inject
    ProntuarioRepositorio prontuarioRepositorio;

    private Paciente novoPaciente(String cpf, String nome) {
        Paciente paciente = new Paciente(nome, "Rua das Flores, 100",
                List.of(new Telefone(51, 987654321L)), "IPE", cpf,
                LocalDate.of(1990, 5, 20), List.of("contato@mail.com"),
                Situacao.NORMAL, new Prontuario(LocalDate.now(), "Quadro estável"));
        pacienteRepositorio.salvar(paciente);
        return paciente;
    }

    private Medico novoMedico(String crm, String especialidade) {
        Medico medico = new Medico(crm, especialidade);
        medicoRepositorio.salvar(medico);
        return medico;
    }

    @Test
    @DisplayName("deve persistir paciente com telefones, e-mails e prontuário (cascade)")
    void devePersistirPacienteCompleto() {
        Paciente paciente = novoPaciente("11144477735", "Antonio Carlos");

        assertNotNull(paciente.getIdPessoa(), "O identificador deveria ser gerado pelo banco.");

        Paciente recuperado = pacienteRepositorio.buscarPorId(paciente.getIdPessoa()).orElseThrow();
        assertEquals("Antonio Carlos", recuperado.getNome());
        assertEquals("11144477735", recuperado.getCpf());
        assertEquals(Situacao.NORMAL, recuperado.getSituacao());
        assertEquals(1, recuperado.getTelefones().size());
        assertEquals(List.of("contato@mail.com"), recuperado.getEmails());
        assertNotNull(recuperado.getProntuario().getIdProntuario());
    }

    @Test
    @DisplayName("não deve persistir paciente sem data de nascimento (restrição NOT NULL)")
    void deveFalharSemDataNascimento() {
        Paciente paciente = new Paciente("Sem Data", null, null, "SUS", "11144477735",
                null, null, Situacao.NORMAL, null);

        boolean falhou;
        try {
            pacienteRepositorio.salvar(paciente);
            pacienteRepositorio.buscarPorId(paciente.getIdPessoa());
            falhou = false;
        } catch (RuntimeException excecao) {
            falhou = true;
        }
        assertTrue(falhou, "A persistência deveria falhar por violar a restrição NOT NULL.");
    }

    @Test
    @DisplayName("deve buscar paciente por CPF e por trecho do nome")
    void deveBuscarPacientePorCpfENome() {
        novoPaciente("11144477735", "Antonio Carlos");

        assertTrue(pacienteRepositorio.buscarPorCpf("11144477735").isPresent());
        assertFalse(pacienteRepositorio.buscarPorCpf("00000000000").isPresent());
        assertEquals(1, pacienteRepositorio.buscarPorNome("antonio").size());
        assertEquals(0, pacienteRepositorio.buscarPorNome("inexistente").size());
    }

    @Test
    @DisplayName("deve atualizar pacote de dados do paciente (merge)")
    void deveAtualizarPaciente() {
        Paciente paciente = novoPaciente("11144477735", "Antonio Carlos");
        paciente.setConvenio("UNIMED");
        paciente.setSituacao(Situacao.HOSPITALIZADO);

        pacienteRepositorio.atualizar(paciente);

        Paciente recuperado = pacienteRepositorio.buscarPorId(paciente.getIdPessoa()).orElseThrow();
        assertEquals("UNIMED", recuperado.getConvenio());
        assertEquals(Situacao.HOSPITALIZADO, recuperado.getSituacao());
    }

    @Test
    @DisplayName("deve excluir paciente e dependentes em cascata")
    void deveExcluirPaciente() {
        Paciente paciente = novoPaciente("11144477735", "Antonio Carlos");
        Long id = paciente.getIdPessoa();

        pacienteRepositorio.excluir(paciente);
        pacienteRepositorio.buscarPorId(id);

        assertFalse(pacienteRepositorio.buscarPorId(id).isPresent());
    }

    @Test
    @DisplayName("deve persistir e buscar médico por CRM e especialidade")
    void devePersistirEBuscarMedico() {
        novoMedico("24016/RS", "Oncologista");

        assertTrue(medicoRepositorio.buscarPorCrm("24016/RS").isPresent());
        assertTrue(medicoRepositorio.buscarPorCrm("24016/rs").isPresent(),
                "A busca por CRM deve ser insensível a maiúsculas/minúsculas.");
        assertEquals(1, medicoRepositorio.buscarPorEspecialidade("oncologista").size());
        assertEquals(0, medicoRepositorio.buscarPorEspecialidade("pediatra").size());
    }

    @Test
    @DisplayName("deve agendar consulta vinculada a médico e paciente")
    void deveAgendarConsulta() {
        Paciente paciente = novoPaciente("11144477735", "Antonio Carlos");
        Medico medico = novoMedico("24016/RS", "Oncologista");
        Consulta consulta = new Consulta(LocalDate.of(2030, 1, 10), LocalTime.of(10, 30), medico);
        consulta.setPaciente(paciente);

        consultaRepositorio.salvar(consulta);

        assertNotNull(consulta.getIdConsulta());
        assertEquals(1, consultaRepositorio.listarPorPaciente(paciente.getIdPessoa()).size());
        assertEquals(1, consultaRepositorio.listarPorMedico(medico.getIdMedico()).size());
        assertEquals(1L, consultaRepositorio.contarPorPaciente(paciente.getIdPessoa()));
        assertEquals(1L, consultaRepositorio.contarPorMedico(medico.getIdMedico()));
    }

    @Test
    @DisplayName("deve detectar conflito de agenda do médico e do paciente (borda: ignorarId)")
    void deveDetectarConflitoDeAgenda() {
        Paciente paciente = novoPaciente("11144477735", "Antonio Carlos");
        Medico medico = novoMedico("24016/RS", "Oncologista");
        LocalDate data = LocalDate.of(2030, 1, 10);
        LocalTime horario = LocalTime.of(10, 30);
        Consulta consulta = new Consulta(data, horario, medico);
        consulta.setPaciente(paciente);
        consultaRepositorio.salvar(consulta);

        assertTrue(consultaRepositorio.existeConsultaDoMedico(medico.getIdMedico(), data, horario, null));
        assertTrue(consultaRepositorio.existeConsultaDoPaciente(paciente.getIdPessoa(), data, horario, null));
        assertFalse(consultaRepositorio.existeConsultaDoMedico(
                medico.getIdMedico(), data, horario, consulta.getIdConsulta()),
                "Ao reagendar a mesma consulta não deve haver conflito consigo mesma.");
        assertFalse(consultaRepositorio.existeConsultaDoMedico(
                medico.getIdMedico(), data, LocalTime.of(11, 0), null));
    }

    @Test
    @DisplayName("deve impedir consultas duplicadas para o mesmo médico (restrição única)")
    void deveImpedirDuplicidadeDeAgendaDoMedico() {
        Paciente paciente = novoPaciente("11144477735", "Antonio Carlos");
        Medico medico = novoMedico("24016/RS", "Oncologista");
        LocalDate data = LocalDate.of(2030, 2, 10);
        LocalTime horario = LocalTime.of(9, 0);

        Consulta primeira = new Consulta(data, horario, medico);
        primeira.setPaciente(paciente);
        consultaRepositorio.salvar(primeira);
        consultaRepositorio.buscarPorId(primeira.getIdConsulta());

        Consulta duplicada = new Consulta(data, horario, medico);
        duplicada.setPaciente(paciente);

        boolean falhou;
        try {
            consultaRepositorio.salvar(duplicada);
            consultaRepositorio.buscarPorId(duplicada.getIdConsulta());
            falhou = false;
        } catch (RuntimeException excecao) {
            falhou = true;
        }
        assertTrue(falhou, "A restrição uk_consulta_agenda deveria impedir a duplicidade.");
    }

    @Test
    @DisplayName("deve listar médicos ordenados por especialidade e executar count")
    void deveListarEContar() {
        novoMedico("24016/RS", "Oncologista");
        novoMedico("18309/SC", "Ortopedista");
        novoPaciente("11144477735", "Antonio Carlos");

        List<Medico> medicos = medicoRepositorio.listarTodos();
        assertEquals("Oncologista", medicos.get(0).getEspecialidade());
        assertEquals(2L, medicoRepositorio.contar());
        assertEquals(1L, pacienteRepositorio.contar());
    }

    @Test
    @DisplayName("deve manipular prontuários de forma independente")
    void deveManipularProntuario() {
        Prontuario prontuario = new Prontuario(LocalDate.of(2024, 3, 10), "Febre alta");
        prontuarioRepositorio.salvar(prontuario);

        Prontuario recuperado = prontuarioRepositorio.buscarPorId(prontuario.getIdProntuario()).orElseThrow();
        assertEquals("Febre alta", recuperado.getDescricao());

        recuperado.setDescricao("Febre controlada");
        prontuarioRepositorio.atualizar(recuperado);
        assertEquals("Febre controlada",
                prontuarioRepositorio.buscarPorId(prontuario.getIdProntuario()).orElseThrow().getDescricao());

        prontuarioRepositorio.excluir(recuperado);
        prontuarioRepositorio.buscarPorId(prontuario.getIdProntuario());
        assertEquals(0, prontuarioRepositorio.listarTodos().size());
    }

    @Test
    @DisplayName("deve excluir consulta existente")
    void deveExcluirConsulta() {
        Paciente paciente = novoPaciente("11144477735", "Antonio Carlos");
        Medico medico = novoMedico("24016/RS", "Oncologista");
        Consulta consulta = new Consulta(LocalDate.of(2030, 3, 10), LocalTime.of(11, 0), medico);
        consulta.setPaciente(paciente);
        consultaRepositorio.salvar(consulta);

        consultaRepositorio.excluir(consulta);
        consultaRepositorio.buscarPorId(consulta.getIdConsulta());

        assertTrue(consultaRepositorio.listarTodas().isEmpty());
    }
}
