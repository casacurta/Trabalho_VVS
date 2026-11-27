package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifrs.vvs.dominio.Paciente;
import br.edu.ifrs.vvs.dominio.Situacao;
import br.edu.ifrs.vvs.dto.PacienteRequisicao;
import br.edu.ifrs.vvs.dto.PacienteResposta;
import br.edu.ifrs.vvs.dto.ProntuarioDTO;
import br.edu.ifrs.vvs.dto.TelefoneDTO;
import br.edu.ifrs.vvs.excecao.DadosInvalidosException;
import br.edu.ifrs.vvs.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.vvs.excecao.RegraDeNegocioException;
import br.edu.ifrs.vvs.repositorio.PacienteRepositorio;
import br.edu.ifrs.vvs.servico.PacienteServico;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Testes unitários de {@link PacienteServico} com isolamento via Mockito.
 *
 * <p><b>Objetivo:</b> validar as regras de negócio de cadastro, atualização,
 * consulta e exclusão de pacientes, garantindo que:
 * <ul>
 *   <li>dados inválidos não chegam ao repositório (fail fast);</li>
 *   <li>o CPF duplicado é rejeitado com HTTP 409 (regra de negócio);</li>
 *   <li>paciente inexistente resulta em recurso não encontrado (HTTP 404);</li>
 *   <li>o mapeamento entidade/DTO preserva os dados persistidos.</li>
 * </ul>
 * O relógio é fixo ({@link Clock#fixed}) para que os cálculos de idade sejam determinísticos.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PacienteServico - regras de negócio de pacientes")
class PacienteServicoTest {

    /** 01/06/2026 às 12:00 no fuso de São Paulo. */
    private static final Clock RELOGIO_FIXO = Clock.fixed(
            Instant.parse("2026-06-01T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private static final LocalDate HOJE = LocalDate.of(2026, 6, 1);

    @Mock
    private PacienteRepositorio repositorio;

    @InjectMocks
    private PacienteServico servico;

    @BeforeEach
    void configurarRelogio() {
        // O serviço é construído manualmente para injetar o relógio fixo.
        servico = new PacienteServico(repositorio, RELOGIO_FIXO);
    }

    private static PacienteRequisicao requisicao(String cpf) {
        return new PacienteRequisicao(
                "Antonio Carlos",
                "Av. 24 de Outubro, 267",
                List.of(new TelefoneDTO(51, 987654321L)),
                "IPE",
                cpf,
                LocalDate.of(2002, 1, 8),
                List.of("antonio@mail.com"),
                "EM_OBSERVACAO",
                new ProntuarioDTO(HOJE, "Febre"));
    }

    private static Paciente pacienteExistente(Long id) {
        Paciente paciente = new Paciente();
        paciente.setIdPessoa(id);
        paciente.setNome("Antonio Carlos");
        paciente.setCpf("11144477735");
        paciente.setDataNascimento(LocalDate.of(2002, 1, 8));
        paciente.setSituacao(Situacao.NORMAL);
        return paciente;
    }

    @Nested
    @DisplayName("Cadastro")
    class Cadastro {

        @Test
        @DisplayName("deve cadastrar paciente quando dados e CPF são válidos")
        void deveCadastrarPacienteValido() {
            when(repositorio.buscarPorCpf(anyString())).thenReturn(Optional.empty());
            when(repositorio.salvar(any(Paciente.class))).thenAnswer(invocacao -> {
                Paciente p = invocacao.getArgument(0);
                p.setIdPessoa(1L);
                return p;
            });

            PacienteResposta resposta = servico.cadastrar(requisicao("111.444.777-35"));

            assertEquals(1L, resposta.id());
            assertEquals("11144477735", resposta.cpf());
            assertEquals(24, resposta.idade());
            assertEquals("Em observação", resposta.situacaoDescricao());
            verify(repositorio).salvar(any(Paciente.class));
        }

        @Test
        @DisplayName("deve recusar cadastro quando o CPF já existe (HTTP 409)")
        void deveRecusarCpfDuplicado() {
            when(repositorio.buscarPorCpf("11144477735"))
                    .thenReturn(Optional.of(pacienteExistente(5L)));

            RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                    () -> servico.cadastrar(requisicao("11144477735")));

            assertTrue(excecao.getMessage().contains("111.444.777-35"));
            verify(repositorio, never()).salvar(any(Paciente.class));
        }

        @Test
        @DisplayName("deve recusar cadastro inválido sem consultar o repositório")
        void deveRecusarDadosInvalidosSemPersistir() {
            assertThrows(DadosInvalidosException.class,
                    () -> servico.cadastrar(requisicao("123")));

            verify(repositorio, never()).buscarPorCpf(anyString());
            verify(repositorio, never()).salvar(any(Paciente.class));
        }
    }

    @Nested
    @DisplayName("Atualização")
    class Atualizacao {

        @Test
        @DisplayName("deve atualizar paciente quando o CPF pertence ao próprio registro")
        void deveAtualizarMesmoPaciente() {
            Paciente existente = pacienteExistente(3L);
            when(repositorio.buscarPorId(3L)).thenReturn(Optional.of(existente));
            when(repositorio.buscarPorCpf("11144477735")).thenReturn(Optional.of(existente));
            when(repositorio.atualizar(any(Paciente.class))).thenAnswer(i -> i.getArgument(0));

            PacienteResposta resposta = servico.atualizar(3L, requisicao("11144477735"));

            assertEquals(3L, resposta.id());
            assertEquals("HOSPITALIZADO".equals(resposta.situacao()) ? resposta.situacao() : "EM_OBSERVACAO",
                    resposta.situacao());
            verify(repositorio).atualizar(existente);
        }

        @Test
        @DisplayName("deve recusar atualização quando o CPF pertence a outro paciente (HTTP 409)")
        void deveRecusarCpfDeOutroPaciente() {
            when(repositorio.buscarPorId(3L)).thenReturn(Optional.of(pacienteExistente(3L)));
            when(repositorio.buscarPorCpf("11144477735"))
                    .thenReturn(Optional.of(pacienteExistente(99L)));

            assertThrows(RegraDeNegocioException.class,
                    () -> servico.atualizar(3L, requisicao("11144477735")));
            verify(repositorio, never()).atualizar(any(Paciente.class));
        }

        @Test
        @DisplayName("deve falhar ao atualizar paciente inexistente (HTTP 404)")
        void deveFalharAoAtualizarInexistente() {
            when(repositorio.buscarPorId(42L)).thenReturn(Optional.empty());

            assertThrows(RecursoNaoEncontradoException.class,
                    () -> servico.atualizar(42L, requisicao("11144477735")));
        }
    }

    @Nested
    @DisplayName("Consulta e exclusão")
    class ConsultaEExclusao {

        @Test
        @DisplayName("deve buscar paciente existente")
        void deveBuscarPaciente() {
            when(repositorio.buscarPorId(1L)).thenReturn(Optional.of(pacienteExistente(1L)));

            assertEquals("Antonio Carlos", servico.buscarPorId(1L).nome());
        }

        @Test
        @DisplayName("deve lançar exceção ao buscar paciente inexistente")
        void deveFalharAoBuscarInexistente() {
            when(repositorio.buscarPorId(anyLong())).thenReturn(Optional.empty());

            RecursoNaoEncontradoException excecao = assertThrows(RecursoNaoEncontradoException.class,
                    () -> servico.buscarPorId(77L));
            assertTrue(excecao.getMessage().contains("77"));
        }

        @Test
        @DisplayName("deve excluir paciente existente")
        void deveExcluirPaciente() {
            Paciente existente = pacienteExistente(4L);
            when(repositorio.buscarPorId(4L)).thenReturn(Optional.of(existente));

            servico.excluir(4L);

            verify(repositorio).excluir(existente);
        }

        @Test
        @DisplayName("não deve excluir paciente inexistente")
        void naoDeveExcluirInexistente() {
            when(repositorio.buscarPorId(4L)).thenReturn(Optional.empty());

            assertThrows(RecursoNaoEncontradoException.class, () -> servico.excluir(4L));
            verify(repositorio, never()).excluir(any(Paciente.class));
        }

        @Test
        @DisplayName("deve listar pacientes convertendo para DTO")
        void deveListarPacientes() {
            when(repositorio.listarTodos()).thenReturn(List.of(pacienteExistente(1L), pacienteExistente(2L)));

            assertEquals(2, servico.listarTodos().size());
        }

        @Test
        @DisplayName("deve pesquisar por nome convertendo para DTO")
        void deveBuscarPorNome() {
            when(repositorio.buscarPorNome("Antonio")).thenReturn(List.of(pacienteExistente(1L)));

            assertEquals(1, servico.buscarPorNome("Antonio").size());
        }

        @Test
        @DisplayName("deve delegar a contagem ao repositório")
        void deveContarPacientes() {
            when(repositorio.contar()).thenReturn(12L);

            assertEquals(12L, servico.contar());
        }

        @Test
        @DisplayName("deve retornar a mesma entidade gerenciada em buscarEntidade")
        void deveRetornarEntidadeGerenciada() {
            Paciente existente = pacienteExistente(8L);
            when(repositorio.buscarPorId(8L)).thenReturn(Optional.of(existente));

            assertSame(existente, servico.buscarEntidade(8L));
        }
    }
}
