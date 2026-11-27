package br.edu.ifrs.vvs.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Prontuário com a descrição clínica do paciente em determinada data.
 */
@Entity
public class Prontuario implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProntuario;

    private LocalDate data;

    @Column(length = 500)
    private String descricao;

    public Prontuario() {
        // Construtor exigido pela especificação Jakarta Persistence.
    }

    public Prontuario(LocalDate data, String descricao) {
        this.data = data;
        this.descricao = descricao;
    }

    public Long getIdProntuario() {
        return idProntuario;
    }

    public void setIdProntuario(Long idProntuario) {
        this.idProntuario = idProntuario;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        String dataFormatada = data == null ? "" : data.format(FORMATO_DATA);
        return "\n\t - Prontuario (" + dataFormatada + ") = " + descricao;
    }
}
