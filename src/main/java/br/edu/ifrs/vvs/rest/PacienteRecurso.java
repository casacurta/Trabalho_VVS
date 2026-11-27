package br.edu.ifrs.vvs.rest;

import br.edu.ifrs.vvs.dto.PacienteRequisicao;
import br.edu.ifrs.vvs.dto.PacienteResposta;
import br.edu.ifrs.vvs.servico.PacienteServico;
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
 * Endpoints REST de pacientes.
 *
 * <pre>
 * POST   /api/pacientes              cadastra um paciente
 * GET    /api/pacientes              lista pacientes (filtro opcional ?nome=)
 * GET    /api/pacientes/{id}         busca paciente por identificador
 * PUT    /api/pacientes/{id}         atualiza paciente
 * DELETE /api/pacientes/{id}         remove paciente
 * </pre>
 */
@Path("/api/pacientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PacienteRecurso {

    private final PacienteServico servico;

    public PacienteRecurso(PacienteServico servico) {
        this.servico = servico;
    }

    /**
     * Cadastra um paciente.
     *
     * @param requisicao dados do paciente
     * @return 201 com o paciente criado e cabeçalho Location
     */
    @POST
    public Response cadastrar(PacienteRequisicao requisicao) {
        PacienteResposta resposta = servico.cadastrar(requisicao);
        return Response.created(URI.create("/api/pacientes/" + resposta.id()))
                .entity(resposta)
                .build();
    }

    /**
     * Lista pacientes; quando o parâmetro {@code nome} é informado, filtra por trecho do nome.
     *
     * @param nome filtro opcional por nome
     * @return lista de pacientes
     */
    @GET
    public List<PacienteResposta> listar(@QueryParam("nome") String nome) {
        return nome == null || nome.isBlank()
                ? servico.listarTodos()
                : servico.buscarPorNome(nome);
    }

    /**
     * Busca um paciente pelo identificador.
     *
     * @param id identificador do paciente
     * @return dados do paciente
     */
    @GET
    @Path("/{id}")
    public PacienteResposta buscar(@PathParam("id") Long id) {
        return servico.buscarPorId(id);
    }

    /**
     * Atualiza um paciente.
     *
     * @param id         identificador do paciente
     * @param requisicao novos dados
     * @return paciente atualizado
     */
    @PUT
    @Path("/{id}")
    public PacienteResposta atualizar(@PathParam("id") Long id, PacienteRequisicao requisicao) {
        return servico.atualizar(id, requisicao);
    }

    /**
     * Remove um paciente.
     *
     * @param id identificador do paciente
     * @return 204 sem conteúdo
     */
    @DELETE
    @Path("/{id}")
    public Response excluir(@PathParam("id") Long id) {
        servico.excluir(id);
        return Response.noContent().build();
    }
}
