package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifrs.vvs.dominio.Paciente;
import br.edu.ifrs.vvs.dominio.Telefone;
import br.edu.ifrs.vvs.dto.ConsultaRequisicao;
import br.edu.ifrs.vvs.dto.MedicoRequisicao;
import br.edu.ifrs.vvs.dto.PacienteRequisicao;
import br.edu.ifrs.vvs.excecao.DadosInvalidosException;
import br.edu.ifrs.vvs.repositorio.ConsultaRepositorio;
import br.edu.ifrs.vvs.repositorio.MedicoRepositorio;
import br.edu.ifrs.vvs.repositorio.PacienteRepositorio;
import br.edu.ifrs.vvs.servico.ValidadorConsulta;
import br.edu.ifrs.vvs.servico.ValidadorCpf;
import br.edu.ifrs.vvs.servico.ValidadorMedico;
import br.edu.ifrs.vvs.servico.ValidadorPaciente;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes unitários para os caminhos alternativos das validações e regras de negócio.
 *
 * <p><b>Objetivo:</b> completar a cobertura de ramificações que representam erros de
 * entrada e situações de borda: CPF com caracteres mistos, e-mails com formatos
 * parcialmente válidos, situação em branco, consulta no mesmo dia sem conflito e
 * reposicionamento dos repositórios ainda não exercitados.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Caminhos alternativos - validações e regras")
class CaminhosAlternativosTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 6, 1, 10, 0);

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @Mock
    private MedicoRepositorio medicoRepositorio;

    @Mock
    private ConsultaRepositorio consultaRepositorio;

    @Test
    @DisplayName("ValidadorCpf: deve recusar CPF com letras em posições diferentes")
    void validadorCpfComLetras() {
        assertFalse(ValidadorCpf.ehValido(null));
        assertFalse(ValidadorCpf.ehValido(""));
        assertFalse(ValidadorCpf.ehValido("111.444.777-3a"));
        assertEquals("11144477735", ValidadorCpf.normalizar("111-444-777-35"));
        assertNull(ValidadorCpf.formatar(null));
    }

    @Test
    @DisplayName("ValidadorPaciente: deve aceitar e-mail com subdomínio e recusar sem domínio")
    void validadorPacienteEmails() {
        PacienteRequisicao valido = new PacienteRequisicao("Maria", null, null, null,
                "11144477735", LocalDate.of(1990, 1, 1),
                List.of("maria@clinica.com.br"), null, null);
        assertEquals(List.of(), ValidadorPaciente.coletarErros(valido, LocalDate.of(2026, 6, 1)));

        PacienteRequisicao invalido = new PacienteRequisicao("Maria", null, null, null,
                "11144477735", LocalDate.of(1990, 1, 1), List.of("maria@"), null, null);
        assertEquals(1, ValidadorPaciente.coletarErros(invalido, LocalDate.of(2026, 6, 1)).size());
    }

    @Test
    @DisplayName("ValidadorPaciente: situação em branco deve ser aceita como padrão")
    void validadorPacienteSituacaoEmBranco() {
        PacienteRequisicao requisicao = new PacienteRequisicao("Maria", null, null, null,
                "11144477735", LocalDate.of(1990, 1, 1), null, "   ", null);

        assertEquals(List.of(), ValidadorPaciente.coletarErros(requisicao, LocalDate.of(2026, 6, 1)));
    }

    @Test
    @DisplayName("ValidadorPaciente: telefone com DDD nulo deve ser recusado")
    void validadorPacienteTelefoneSemDdd() {
        PacienteRequisicao requisicao = new PacienteRequisicao("Maria", null,
                List.of(new br.edu.ifrs.vvs.dto.TelefoneDTO(null, 1L)), null,
                "11144477735", LocalDate.of(1990, 1, 1), null, null, null);

        Txt erro = new Txt(assertThrows(DadosInvalidosException.class,
                () -> ValidadorPaciente.validar(requisicao, LocalDate.of(2026, 6, 1))).getDetalhes());
        assertTrue(erro.contem("DDD inválido"));
    }

    @Test
    @DisplayName("ValidadorMedico: especialidade em branco deve ser recusada")
    void validadorMedicoEspecialidadeEmBranco() {
        DadosInvalidosException excecao = assertThrows(DadosInvalidosException.class,
                () -> ValidadorMedico.validar(new MedicoRequisicao("24016/RS", "  ")));
        assertTrue(excecao.getDetalhes().contains("A especialidade do médico é obrigatória."));
    }

    @Test
    @DisplayName("ValidadorConsulta: consulta no mesmo dia do agendamento, mas em horário futuro, deve ser aceita")
    void validadorConsultaMesmoDiaFuturo() {
        ConsultaRequisicao requisicao = new ConsultaRequisicao(AGORA.toLocalDate(),
                LocalTime.of(11, 0), 1L, 1L);

        assertEquals(List.of(), ValidadorConsulta.coletarErros(requisicao, AGORA));
    }

    @Test
    @DisplayName("ValidadorConsulta: horário antes do expediente e no passado acumulam erros")
    void validadorConsultaAcumulaErros() {
        ConsultaRequisicao requisicao = new ConsultaRequisicao(AGORA.toLocalDate().minusDays(2),
                LocalTime.of(6, 30), 1L, 1L);

        assertEquals(2, ValidadorConsulta.coletarErros(requisicao, AGORA).size());
    }

    /** Utilitário local para verificar mensagens de erro de forma legível. */
    private record Txt(List<String> mensagens) {
        boolean contem(String trecho) {
            return mensagens.stream().anyMatch(m -> m.contains(trecho));
        }
    }
}
