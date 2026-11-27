package br.edu.ifrs.vvs.rest;

import br.edu.ifrs.vvs.dto.ConsultaRequisicao;
import br.edu.ifrs.vvs.dto.ConsultaResposta;
import br.edu.ifrs.vvs.servico.ConsultaServico;
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
 * Endpoints REST de consultas.
 *
 * <pre>
 * POST   /api/consultas                       agenda uma consulta
 * GET    /api/consultas                       lista consultas (?pacienteId= | ?medicoId=)
 * GET    /api/consultas/{id}                  busca consulta por identificador
 * PUT    /api/consultas/{id}                  reagenda consulta
 * DELETE /api/consultas/{id}                  cancela consulta
 * </pre>
 */
@Path("/api/consultas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConsultaRecurso {

    private final ConsultaServico servico;

    public ConsultaRecurso(ConsultaServico servico) {
        this.servico = servico;
    }

    /**
     * Agenda uma consulta.
     *
     * @param requisicao dados da consulta
     * @return 201 com a consulta criada e cabeçalho Location
     */
    @POST
    public Response agendar(ConsultaRequisicao requisicao) {
        ConsultaResposta resposta = servico.agendar(requisicao);
        return Response.created(URI.create("/api/consultas/" + resposta.id()))
                .entity(resposta)
                .build();
    }

    /**
     * Lista consultas, permitindo filtrar por paciente ou por médico.
     *
     * @param pacienteId filtro opcional por paciente
     * @param medicoId   filtro opcional por médico
     * @return lista de consultas
     */
    @GET
    public List<ConsultaResposta> listar(@QueryParam("pacienteId") Long pacienteId,
                                         @QueryParam("medicoId") Long medicoId) {
        if (pacienteId != null) {
            return servico.listarPorPaciente(pacienteId);
        }
        if (medicoId != null) {
            return servico.listarPorMedico(medicoId);
        }
        return servico.listarTodas();
    }

    /**
     * Busca uma consulta pelo identificador.
     *
     * @param id identificador da consulta
     * @return dados da consulta
     */
    @GET
    @Path("/{id}")
    public ConsultaResposta buscar(@PathParam("id") Long id) {
        return servico.buscarPorId(id);
    }

    /**
     * Reagenda uma consulta.
     *
     * @param id         identificador da consulta
     * @param requisicao novos dados
     * @return consulta atualizada
     */
    @PUT
    @Path("/{id}")
    public ConsultaResposta reagendar(@PathParam("id") Long id, ConsultaRequisicao requisicao) {
        return servico.reagendar(id, requisicao);
    }

    /**
     * Cancela uma consulta.
     *
     * @param id identificador da consulta
     * @return 204 sem conteúdo
     */
    @DELETE
    @Path("/{id}")
    public Response cancelar(@PathParam("id") Long id) {
        servico.cancelar(id);
        return Response.noContent().build();
    }
}
