package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários dos acessores (getters/setters) das entidades.
 *
 * <p><b>Objetivo:</b> garantir que todos os acessores de domínio devolvem exatamente o
 * valor atribuído (ou o valor tratado, no caso de listas) e que campos nulos não
 * quebram a formatação textual. Essa verificação complementa os testes de regra de
 * negócio, cobrindo os caminhos simples das entidades.</p>
 */
@DisplayName("Domínio - acessores das entidades")
class DominioAcessoresTest {

    @Test
    @DisplayName("Pessoa: deve devolver os valores atribuídos")
    void pessoaDeveDevolverValores() {
        Pessoa pessoa = new Pessoa();
        pessoa.setIdPessoa(3L);
        pessoa.setNome("Maria");
        pessoa.setEndereco("Rua B, 20");
        pessoa.setTelefones(List.of(new Telefone(51, 111L)));

        assertEquals(3L, pessoa.getIdPessoa());
        assertEquals("Maria", pessoa.getNome());
        assertEquals("Rua B, 20", pessoa.getEndereco());
        assertEquals(1, pessoa.getTelefones().size());
    }

    @Test
    @DisplayName("Telefone: deve devolver os valores atribuídos")
    void telefoneDeveDevolverValores() {
        Telefone telefone = new Telefone();
        telefone.setIdTelefone(9L);
        telefone.setDdd(11);
        telefone.setNumero(99999L);

        assertEquals(9L, telefone.getIdTelefone());
        assertEquals(11, telefone.getDdd());
        assertEquals(99999L, telefone.getNumero());
    }

    @Test
    @DisplayName("Medico: deve devolver os valores atribuídos e lista de consultas")
    void medicoDeveDevolverValores() {
        Medico medico = new Medico();
        medico.setIdMedico(4L);
        medico.setCrm("1234/DF");
        medico.setEspecialidade("Clínica");
        Consulta consulta = new Consulta(LocalDate.now(), LocalTime.NOON, medico);
        medico.setConsultas(List.of(consulta));

        assertEquals(4L, medico.getIdMedico());
        assertEquals("1234/DF", medico.getCrm());
        assertEquals("Clínica", medico.getEspecialidade());
        assertEquals(1, medico.getConsultas().size());
    }

    @Test
    @DisplayName("Consulta: deve devolver os valores atribuídos (incluindo paciente e médico)")
    void consultaDeveDevolverValores() {
        Medico medico = new Medico("1234/DF", "Clínica");
        Paciente paciente = new Paciente();
        Consulta consulta = new Consulta();
        consulta.setIdConsulta(8L);
        consulta.setData(LocalDate.of(2030, 1, 1));
        consulta.setHorario(LocalTime.of(8, 15));
        consulta.setMedico(medico);
        consulta.setPaciente(paciente);

        assertEquals(8L, consulta.getIdConsulta());
        assertEquals(LocalDate.of(2030, 1, 1), consulta.getData());
        assertEquals(LocalTime.of(8, 15), consulta.getHorario());
        assertEquals(medico, consulta.getMedico());
        assertEquals(paciente, consulta.getPaciente());
    }

    @Test
    @DisplayName("Prontuario: deve devolver os valores atribuídos")
    void prontuarioDeveDevolverValores() {
        Prontuario prontuario = new Prontuario();
        prontuario.setIdProntuario(5L);
        prontuario.setData(LocalDate.of(2024, 2, 2));
        prontuario.setDescricao("Estável");

        assertEquals(5L, prontuario.getIdProntuario());
        assertEquals(LocalDate.of(2024, 2, 2), prontuario.getData());
        assertEquals("Estável", prontuario.getDescricao());
    }

    @Test
    @DisplayName("Paciente: deve devolver todos os campos atribuídos")
    void pacienteDeveDevolverValores() {
        Prontuario prontuario = new Prontuario(LocalDate.now(), "Ok");
        Paciente paciente = new Paciente();
        paciente.setIdPessoa(10L);
        paciente.setNome("Laura");
        paciente.setConvenio("SUS");
        paciente.setCpf("11144477735");
        paciente.setDataNascimento(LocalDate.of(2013, 5, 19));
        paciente.setEmails(List.of("laura@mail.com"));
        paciente.setSituacao(Situacao.HOSPITALIZADO);
        paciente.setProntuario(prontuario);
        paciente.setConsultas(List.of());

        assertEquals(10L, paciente.getIdPessoa());
        assertEquals("Laura", paciente.getNome());
        assertEquals("SUS", paciente.getConvenio());
        assertEquals("11144477735", paciente.getCpf());
        assertEquals(LocalDate.of(2013, 5, 19), paciente.getDataNascimento());
        assertEquals(1, paciente.getEmails().size());
        assertEquals(Situacao.HOSPITALIZADO, paciente.getSituacao());
        assertEquals(prontuario, paciente.getProntuario());
        assertNotNull(paciente.getConsultas());
    }

    @Test
    @DisplayName("toString de Pessoa com telefone nulo na lista não deve falhar")
    void pessoaComTelefoneNuloNaLista() {
        Pessoa pessoa = new Pessoa();
        pessoa.getTelefones().add(null);
        pessoa.getTelefones().add(new Telefone(51, 12345L));

        String texto = pessoa.toString();

        assertTrue(texto.contains("(51) 12345"));
    }

    @Test
    @DisplayName("toString de Paciente com consulta nula na lista não deve falhar")
    void pacienteComConsultaNulaNaLista() {
        Paciente paciente = new Paciente();
        paciente.getConsultas().add(null);
        paciente.setSituacao(Situacao.NORMAL);

        String texto = paciente.toString();

        assertTrue(texto.contains("Normal"));
    }

    @Test
    @DisplayName("transformações de lista devem manter a mesma instância (evita erro do Hibernate)")
    void transformacoesDevemManterInstancia() {
        Pessoa pessoa = new Pessoa();
        var listaTelefones = pessoa.getTelefones();
        pessoa.setTelefones(List.of(new Telefone(51, 1L)));
        pessoa.setTelefones(null);

        Medico medico = new Medico();
        var listaConsultas = medico.getConsultas();
        medico.setConsultas(null);

        assertEquals(listaTelefones, pessoa.getTelefones());
        assertEquals(listaConsultas, medico.getConsultas());
        assertTrue(pessoa.getTelefones().isEmpty());
    }
}
