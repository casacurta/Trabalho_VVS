package br.edu.ifrs.vvs.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe base de pessoas do sistema, mapeada com estratégia de herança {@code JOINED}.
 *
 * <p>Os telefones são mantidos em mapeamento unidirecional: a coluna de ligação
 * ({@code idPessoa}) fica na tabela {@code Telefone}.</p>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Pessoa implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPessoa;

    private String nome;

    private String endereco;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, orphanRemoval = true)
    @JoinColumn(name = "idPessoa")
    private List<Telefone> telefones = new ArrayList<>();

    public Pessoa() {
        // Construtor exigido pela especificação Jakarta Persistence.
    }

    public Pessoa(String nome, String endereco, List<Telefone> telefones) {
        this.nome = nome;
        this.endereco = endereco;
        this.telefones = telefones == null ? new ArrayList<>() : new ArrayList<>(telefones);
    }

    public Long getIdPessoa() {
        return idPessoa;
    }

    public void setIdPessoa(Long idPessoa) {
        this.idPessoa = idPessoa;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public List<Telefone> getTelefones() {
        return telefones;
    }

    /**
     * Substitui os telefones da pessoa.
     *
     * <p>A lista existente é <b>reutilizada</b> (clear + addAll) em vez de trocada por
     * uma nova instância. Trocar a referência de uma coleção gerenciada pelo Hibernate
     * com {@code orphanRemoval} provoca {@code HibernateException}: "A collection with
     * orphan deletion was no longer referenced by the owning entity instance".</p>
     *
     * @param telefones novos telefones (nulo é tratado como lista vazia)
     */
    public void setTelefones(List<Telefone> telefones) {
        this.telefones.clear();
        if (telefones != null) {
            this.telefones.addAll(telefones);
        }
    }

    /**
     * Adiciona um telefone à pessoa, mantendo a lista sempre inicializada.
     *
     * @param telefone telefone a ser adicionado; valores nulos são ignorados
     */
    public void adicionarTelefone(Telefone telefone) {
        if (telefone != null) {
            this.telefones.add(telefone);
        }
    }

    @Override
    public String toString() {
        String auxiliar;
        if (telefones == null || telefones.isEmpty()) {
            auxiliar = "Sem telefone";
        } else {
            StringBuilder texto = new StringBuilder();
            for (Telefone telefone : telefones) {
                if (telefone != null) {
                    texto.append(telefone).append(", ");
                }
            }
            auxiliar = texto.toString();
        }
        return "\nPessoa " + idPessoa + " :\n\t - nome = " + nome
                + "\n\t - endereco = " + endereco
                + "\n\t - Telefone(s) = " + auxiliar;
    }
}
