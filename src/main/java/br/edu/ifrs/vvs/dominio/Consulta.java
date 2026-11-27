package br.edu.ifrs.vvs.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Consulta médica agendada.
 *
 * <p>A restrição {@code uk_consulta_agenda} impede dois agendamentos para o mesmo
 * médico na mesma data e horário. A duplicidade por paciente (mesmo dia/horário) é
 * validada na camada de serviço, pois o paciente pode ter mais de um médico.</p>
 */
@Entity
@Table(name = "Consulta",
        uniqueConstraints = @UniqueConstraint(name = "uk_consulta_agenda",
                columnNames = {"idMedico", "data", "horario"}))
public class Consulta implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idConsulta;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "horario", nullable = false)
    private LocalTime horario;

    /**
     * Sem cascade: o médico deve ser persistido/gerenciado separadamente.
     * O cascade PERSIST aqui gerava {@code detached entity passed to persist}
     * quando o mesmo médico era reutilizado em consultas de pacientes diferentes.
     */
    @ManyToOne
    @JoinColumn(name = "idMedico", nullable = false)
    private Medico medico;

    @ManyToOne
    @JoinColumn(name = "idPaciente")
    private Paciente paciente;

    public Consulta() {
        // Construtor exigido pela especificação Jakarta Persistence.
    }

    public Consulta(LocalDate data, LocalTime horario, Medico medico) {
        this.data = data;
        this.horario = horario;
        this.medico = medico;
    }

    public Long getIdConsulta() {
        return idConsulta;
    }

    public void setIdConsulta(Long idConsulta) {
        this.idConsulta = idConsulta;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public void setHorario(LocalTime horario) {
        this.horario = horario;
    }

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(Medico medico) {
        this.medico = medico;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    @Override
    public String toString() {
        String dataFormatada = data == null ? "" : data.format(FORMATO_DATA);
        String horaFormatada = horario == null ? "" : horario.format(FORMATO_HORA);
        String medicoFormatado = medico == null ? "" : ", " + medico;
        return "[Data: " + dataFormatada + " - " + horaFormatada + medicoFormatado + "]";
    }
}
