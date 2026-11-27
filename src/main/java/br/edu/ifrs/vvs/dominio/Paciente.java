package br.edu.ifrs.vvs.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Paciente da clínica. Herda de {@link Pessoa} e acrescenta dados clínicos,
 * de contato e o prontuário.
 */
@Entity
@PrimaryKeyJoinColumn(name = "idPaciente", referencedColumnName = "idPessoa")
public class Paciente extends Pessoa implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Formato brasileiro usado na exibição de datas. */
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String convenio;

    private String cpf;

    @Column(name = "data_Nascimento", nullable = false)
    private LocalDate dataNascimento;

    @ElementCollection
    @CollectionTable(name = "Lista_emails")
    private List<String> emails = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Situacao situacao;

    /**
     * Prontuário do paciente (relação 1:1).
     * O cascade PERSIST/MERGE permite gravar o prontuário junto com o paciente.
     */
    @OneToOne(orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "idProntuario")
    private Prontuario prontuario;

    /**
     * Consultas do paciente. As consultas são gerenciadas pelo serviço de consultas,
     * por isso o cascade é usado apenas para exclusão em cascata.
     */
    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Consulta> consultas = new ArrayList<>();

    public Paciente() {
        // Construtor exigido pela especificação Jakarta Persistence.
    }

    public Paciente(String nome, String endereco, List<Telefone> telefones, String convenio,
                    String cpf, LocalDate dataNascimento, List<String> emails, Situacao situacao,
                    Prontuario prontuario) {
        super(nome, endereco, telefones);
        this.convenio = convenio;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.emails = emails == null ? new ArrayList<>() : new ArrayList<>(emails);
        this.situacao = situacao;
        this.prontuario = prontuario;
    }

    public String getConvenio() {
        return convenio;
    }

    public void setConvenio(String convenio) {
        this.convenio = convenio;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public List<String> getEmails() {
        return emails;
    }

    /**
     * Substitui a lista de e-mails reutilizando a coleção gerenciada.
     *
     * <p>Ver {@link Pessoa#setTelefones(List)}: a coleção não é substituída por uma
     * nova instância para não invalidar o rastreamento do Hibernate.</p>
     *
     * @param emails novos e-mails (nulo é tratado como lista vazia)
     */
    public void setEmails(List<String> emails) {
        this.emails.clear();
        if (emails != null) {
            this.emails.addAll(emails);
        }
    }

    public Situacao getSituacao() {
        return situacao;
    }

    public void setSituacao(Situacao situacao) {
        this.situacao = situacao;
    }

    public Prontuario getProntuario() {
        return prontuario;
    }

    public void setProntuario(Prontuario prontuario) {
        this.prontuario = prontuario;
    }

    public List<Consulta> getConsultas() {
        return consultas;
    }

    /**
     * Substitui as consultas reutilizando a coleção gerenciada.
     *
     * <p>A coleção é alterada no lugar (clear + addAll) porque o mapeamento usa
     * {@code orphanRemoval}; substituir a referência causaria exceção do Hibernate
     * ao sincronizar a sessão.</p>
     *
     * @param consultas novas consultas (nulo é tratado como lista vazia)
     */
    public void setConsultas(List<Consulta> consultas) {
        this.consultas.clear();
        if (consultas != null) {
            this.consultas.addAll(consultas);
        }
    }

    /**
     * Vincula uma consulta ao paciente, mantendo ambos os lados da relação consistentes.
     *
     * @param consulta consulta a ser vinculada
     */
    public void adicionarConsulta(Consulta consulta) {
        if (consulta != null) {
            consulta.setPaciente(this);
            this.consultas.add(consulta);
        }
    }

    @Override
    public String toString() {
        String emailsFormatados = emails == null || emails.isEmpty()
                ? "Sem e-mail"
                : String.join(", ", emails);
        String consultasFormatadas = consultas == null || consultas.isEmpty()
                ? "Sem consultas"
                : consultas.toString();
        String dataFormatada = dataNascimento == null ? "" : dataNascimento.format(FORMATO_DATA);
        String situacaoFormatada = situacao == null ? "" : situacao.getDescricao();
        String prontuarioFormatado = prontuario == null ? "" : prontuario.toString();

        return super.toString()
                + "\n\t - Convenio = " + convenio
                + "\n\t - CPF = " + cpf
                + "\n\t - Nascimento = " + dataFormatada
                + "\n\t - Emails = " + emailsFormatados
                + "\n\t - Situação = " + situacaoFormatada
                + prontuarioFormatado
                + "\n\t - Consultas = " + consultasFormatadas;
    }
}
