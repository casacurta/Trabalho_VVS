package br.edu.ifrs.vvs.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Médico responsável pelos atendimentos da clínica.
 */
@Entity
public class Medico implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMedico;

    private String crm;

    private String especialidade;

    /**
     * Consultas do médico. O cascade é aplicado somente na exclusão; a persistência
     * das consultas é responsabilidade do {@code ConsultaServico}.
     */
    @OneToMany(mappedBy = "medico", cascade = {CascadeType.REMOVE})
    private List<Consulta> consultas = new ArrayList<>();

    public Medico() {
        // Construtor exigido pela especificação Jakarta Persistence.
    }

    public Medico(String crm, String especialidade) {
        this.crm = crm;
        this.especialidade = especialidade;
    }

    public Long getIdMedico() {
        return idMedico;
    }

    public void setIdMedico(Long idMedico) {
        this.idMedico = idMedico;
    }

    public String getCrm() {
        return crm;
    }

    public void setCrm(String crm) {
        this.crm = crm;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public void setEspecialidade(String especialidade) {
        this.especialidade = especialidade;
    }

    public List<Consulta> getConsultas() {
        return consultas;
    }

    /**
     * Substitui as consultas reutilizando a coleção gerenciada, evitando
     * inconsistências no rastreamento do Hibernate.
     *
     * @param consultas novas consultas (nulo é tratado como lista vazia)
     */
    public void setConsultas(List<Consulta> consultas) {
        this.consultas.clear();
        if (consultas != null) {
            this.consultas.addAll(consultas);
        }
    }

    @Override
    public String toString() {
        return "Médico: crm = " + crm + ", especialidade = " + especialidade;
    }
}
