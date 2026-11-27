package br.edu.ifrs.vvs.unitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifrs.vvs.dominio.Medico;
import br.edu.ifrs.vvs.dto.MedicoRequisicao;
import br.edu.ifrs.vvs.dto.MedicoResposta;
import br.edu.ifrs.vvs.excecao.DadosInvalidosException;
import br.edu.ifrs.vvs.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.vvs.excecao.RegraDeNegocioException;
import br.edu.ifrs.vvs.repositorio.ConsultaRepositorio;
import br.edu.ifrs.vvs.repositorio.MedicoRepositorio;
import br.edu.ifrs.vvs.servico.MedicoServico;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Testes unitários de {@link MedicoServico}.
 *
 * <p><b>Objetivo:</b> verificar unicidade de CRM, bloqueio de exclusão de médico com
 * consultas agendadas, tratamento de médico inexistente e conversão das respostas.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MedicoServico - regras de negócio de médicos")
class MedicoServicoTest {

    @Mock
    private MedicoRepositorio repositorio;

    @Mock
    private ConsultaRepositorio consultaRepositorio;

    @InjectMocks
    private MedicoServico servico;

    private static Medico medico(Long id) {
        Medico medico = new Medico("24016/RS", "Oncologista");
        medico.setIdMedico(id);
        return medico;
    }

    @Test
    @DisplayName("deve cadastrar médico quando o CRM é único")
    void deveCadastrarMedicoValido() {
        when(repositorio.buscarPorCrm("24016/RS")).thenReturn(Optional.empty());
        when(repositorio.salvar(any(Medico.class))).thenAnswer(invocacao -> {
            Medico m = invocacao.getArgument(0);
            m.setIdMedico(10L);
            return m;
        });

        MedicoResposta resposta = servico.cadastrar(new MedicoRequisicao(" 24016/rs ", "Oncologista"));

        assertEquals(10L, resposta.id());
        assertEquals("24016/RS", resposta.crm());
        assertEquals(0, resposta.totalConsultas());
    }

    @Test
    @DisplayName("deve recusar CRM duplicado (HTTP 409)")
    void deveRecusarCrmDuplicado() {
        when(repositorio.buscarPorCrm("24016/RS")).thenReturn(Optional.of(medico(1L)));

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> servico.cadastrar(new MedicoRequisicao("24016/RS", "Oncologista")));

        assertTrue(excecao.getMessage().contains("24016/RS"));
        verify(repositorio, never()).salvar(any(Medico.class));
    }

    @Test
    @DisplayName("deve recusar cadastro inválido sem consultar o repositório")
    void deveRecusarCadastroInvalido() {
        assertThrows(DadosInvalidosException.class,
                () -> servico.cadastrar(new MedicoRequisicao("invalido", "")));
        verify(repositorio, never()).salvar(any(Medico.class));
    }

    @Test
    @DisplayName("deve atualizar médico quando o CRM pertence ao próprio registro")
    void deveAtualizarMedico() {
        Medico existente = medico(2L);
        when(repositorio.buscarPorId(2L)).thenReturn(Optional.of(existente));
        when(repositorio.buscarPorCrm("24016/RS")).thenReturn(Optional.of(existente));
        when(repositorio.atualizar(any(Medico.class))).thenAnswer(i -> i.getArgument(0));
        when(consultaRepositorio.contarPorMedico(2L)).thenReturn(4L);

        MedicoResposta resposta = servico.atualizar(2L,
                new MedicoRequisicao("24016/RS", "Oncologia Clínica"));

        assertEquals("Oncologia Clínica", resposta.especialidade());
        assertEquals(4, resposta.totalConsultas());
    }

    @Test
    @DisplayName("deve recusar atualização com CRM de outro médico")
    void deveRecusarCrmDeOutroMedico() {
        when(repositorio.buscarPorId(2L)).thenReturn(Optional.of(medico(2L)));
        when(repositorio.buscarPorCrm("24016/RS")).thenReturn(Optional.of(medico(9L)));

        assertThrows(RegraDeNegocioException.class,
                () -> servico.atualizar(2L, new MedicoRequisicao("24016/RS", "Oncologista")));
    }

    @Test
    @DisplayName("deve impedir exclusão de médico com consultas agendadas")
    void deveImpedirExclusaoComConsultas() {
        when(repositorio.buscarPorId(3L)).thenReturn(Optional.of(medico(3L)));
        when(consultaRepositorio.contarPorMedico(3L)).thenReturn(2L);

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> servico.excluir(3L));

        assertTrue(excecao.getMessage().contains("2 consulta"));
        verify(repositorio, never()).excluir(any(Medico.class));
    }

    @Test
    @DisplayName("deve excluir médico sem consultas")
    void deveExcluirMedicoSemConsultas() {
        Medico existente = medico(3L);
        when(repositorio.buscarPorId(3L)).thenReturn(Optional.of(existente));
        when(consultaRepositorio.contarPorMedico(3L)).thenReturn(0L);

        servico.excluir(3L);

        verify(repositorio).excluir(existente);
    }

    @Test
    @DisplayName("deve falhar ao buscar médico inexistente (HTTP 404)")
    void deveFalharAoBuscarInexistente() {
        when(repositorio.buscarPorId(anyLong())).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> servico.buscarPorId(55L));
    }

    @Test
    @DisplayName("deve listar médicos com total de consultas")
    void deveListarMedicos() {
        when(repositorio.listarTodos()).thenReturn(List.of(medico(1L), medico(2L)));
        when(consultaRepositorio.contarPorMedico(anyLong())).thenReturn(1L);

        List<MedicoResposta> resposta = servico.listarTodos();

        assertEquals(2, resposta.size());
        assertEquals(1, resposta.get(0).totalConsultas());
    }

    @Test
    @DisplayName("deve buscar médicos por especialidade")
    void deveBuscarPorEspecialidade() {
        when(repositorio.buscarPorEspecialidade("Oncologista")).thenReturn(List.of(medico(1L)));
        when(consultaRepositorio.contarPorMedico(anyLong())).thenReturn(0L);

        assertEquals(1, servico.buscarPorEspecialidade("Oncologista").size());
    }

    @Test
    @DisplayName("deve delegar a busca por ID retornando o DTO com contagem")
    void deveBuscarPorId() {
        when(repositorio.buscarPorId(7L)).thenReturn(Optional.of(medico(7L)));
        when(consultaRepositorio.contarPorMedico(7L)).thenReturn(3L);

        assertEquals(3, servico.buscarPorId(7L).totalConsultas());
    }

    @Test
    @DisplayName("não deve consultar o repositório de consultas ao validar CRM inexistente")
    void naoDeveConsultarAoValidarCrm() {
        when(repositorio.buscarPorCrm(anyString())).thenReturn(Optional.empty());
        when(repositorio.salvar(any(Medico.class))).thenReturn(medico(1L));

        servico.cadastrar(new MedicoRequisicao("1234/DF", "Clínica"));

        verify(consultaRepositorio, never()).contarPorMedico(anyLong());
    }
}
