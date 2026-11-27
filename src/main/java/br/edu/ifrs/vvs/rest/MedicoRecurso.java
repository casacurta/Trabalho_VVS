package br.edu.ifrs.vvs.rest;

import br.edu.ifrs.vvs.dto.MedicoRequisicao;
import br.edu.ifrs.vvs.dto.MedicoResposta;
import br.edu.ifrs.vvs.servico.MedicoServico;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * Endpoints REST de médicos.
 *
 * <pre>
 * POST   /api/medicos                          cadastra um médico
 * GET    /api/medicos                          lista médicos (?especialidade=)
 * GET    /api/medicos/{id}                     busca médico por identificador
 * PUT    /api/medicos/{id}                     atualiza médico
 * DELETE /api/medicos/{id}                     remove médico sem consultas
 * </pre>
 */
@Path("/api/medicos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MedicoRecurso {

    private final MedicoServico servico;

    public MedicoRecurso(MedicoServico servico) {
        this.servico = servico;
    }

    /**
     * Cadastra um médico.
     *
     * @param requisicao dados do médico
     * @return 201 com o médico criado e cabeçalho Location
     */
    @POST
    public Response cadastrar(MedicoRequisicao requisicao) {
        MedicoResposta resposta = servico.cadastrar(requisicao);
        return Response.created(URI.create("/api/medicos/" + resposta.id()))
                .entity(resposta)
                .build();
    }

    /**
     * Lista médicos, opcionalmente filtrando por especialidade.
     *
     * @param especialidade filtro opcional
     * @return lista de médicos
     */
    @GET
    public List<MedicoResposta> listar(@QueryParam("especialidade") String especialidade) {
        return especialidade == null || especialidade.isBlank()
                ? servico.listarTodos()
                : servico.buscarPorEspecialidade(especialidade);
    }

    /**
     * Busca um médico pelo identificador.
     *
     * @param id identificador do médico
     * @return dados do médico
     */
    @GET
    @Path("/{id}")
    public MedicoResposta buscar(@PathParam("id") Long id) {
        return servico.buscarPorId(id);
    }

    /**
     * Atualiza um médico.
     *
     * @param id         identificador do médico
     * @param requisicao novos dados
     * @return médico atualizado
     */
    @PUT
    @Path("/{id}")
    public MedicoResposta atualizar(@PathParam("id") Long id, MedicoRequisicao requisicao) {
        return servico.atualizar(id, requisicao);
    }

    /**
     * Remove um médico (somente quando não há consultas vinculadas).
     *
     * @param id identificador do médico
     * @return 204 sem conteúdo
     */
    @DELETE
    @Path("/{id}")
    public Response excluir(@PathParam("id") Long id) {
        servico.excluir(id);
        return Response.noContent().build();
    }
}
