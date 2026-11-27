# Trabalho_VVS — Sistema de Clínica Médica com Verificação e Validação

Projeto desenvolvido para a disciplina de **Verificação e Validação de Sistemas**, a partir
do projeto de mapeamento JPA (pacientes, médicos, consultas e prontuários), evoluído para
uma **API REST em Quarkus** acompanhada de uma suíte completa de testes, análise estática e
métricas de cobertura.

| Item | Versão |
|---|---|
| Java | 25 |
| Quarkus | 3.39.2 |
| Maven | 3.9 |
| Testes | JUnit 5, Mockito, RestAssured, H2 |
| Qualidade | PMD 7.17, JaCoCo 0.8.13 |

---

## 1. Como executar

```bash
# Build completo com todos os testes, análise estática e cobertura
mvn clean verify

# Apenas testes rápidos (unitários e de componente)
mvn test

# Apenas testes de sistema
mvn verify -DskipITs=false -Dit.test='*IT'

# Execução manual em modo desenvolvimento (H2 em memória, dados de exemplo)
mvn quarkus:dev
# API disponível em http://localhost:8080
```

### Evidências geradas

| Evidência | Caminho |
|---|---|
| Plano de teste | [`plano-de-teste.md`](plano-de-teste.md) |
| Relatório de execução | [`docs/relatorios/relatorio-execucao.md`](docs/relatorios/relatorio-execucao.md) |
| Análise estática (PMD) | [`docs/relatorios/pmd/pmd.html`](docs/relatorios/pmd/pmd.html) |
| Cobertura (JaCoCo) | `target/site/jacoco/index.html` |
| Resultados Surefire / Failsafe | `target/surefire-reports/`, `target/failsafe-reports/` |

---

## 2. Arquitetura

```
src/main/java/br/edu/ifrs/vvs/
├── dominio/       Entidades JPA (Pessoa, Paciente, Medico, Consulta, Prontuario, Telefone, Situacao)
├── dto/           Records de requisição/resposta da API (inclui ErroResposta)
├── repositorio/   Acesso a dados com EntityManager (CDI) — sem JPAUtil manual
├── servico/       Regras de negócio, validações e mapeadores DTO ↔ entidade
├── rest/          Recursos Jakarta REST (/api/pacientes, /api/medicos, /api/consultas, /api/saude)
├── excecao/       Exceções de domínio e ExceptionMappers (400/404/409/500)
└── config/        Produção do Clock injetável

src/test/java/br/edu/ifrs/vvs/
├── unitario/      184 casos — JUnit 5 + Mockito (sem banco)
├── componente/    49 casos  — @QuarkusTest com H2 e RestAssured
└── sistema/       7 casos   — jornadas ponta a ponta (Failsafe, sufixo *IT)
```

### API

| Método | Caminho | Descrição |
|---|---|---|
| `GET` | `/api/saude` | Verificação de disponibilidade |
| `POST` | `/api/pacientes` | Cadastrar paciente |
| `GET` | `/api/pacientes?nome=` | Listar/pesquisar pacientes |
| `GET` | `/api/pacientes/{id}` | Buscar paciente |
| `PUT` | `/api/pacientes/{id}` | Atualizar paciente |
| `DELETE` | `/api/pacientes/{id}` | Excluir paciente |
| `POST` | `/api/medicos` | Cadastrar médico |
| `GET` | `/api/medicos?especialidade=` | Listar/pesquisar médicos |
| `GET` | `/api/medicos/{id}` | Buscar médico |
| `PUT` | `/api/medicos/{id}` | Atualizar médico |
| `DELETE` | `/api/medicos/{id}` | Excluir médico (somente sem consultas) |
| `POST` | `/api/consultas` | Agendar consulta |
| `GET` | `/api/consultas?pacienteId=&medicoId=` | Listar consultas |
| `PUT` | `/api/consultas/{id}` | Reagendar consulta |
| `DELETE` | `/api/consultas/{id}` | Cancelar consulta |

#### Exemplo de requisição

```bash
curl -X POST http://localhost:8080/api/pacientes \
  -H 'Content-Type: application/json' \
  -d '{
        "nome": "Antonio Carlos",
        "endereco": "Av. 24 de Outubro, 267",
        "telefones": [{ "ddd": 51, "numero": 987654321 }],
        "convenio": "IPE",
        "cpf": "111.444.777-35",
        "dataNascimento": "2002-01-08",
        "emails": ["antonio@mail.com"],
        "situacao": "EM_OBSERVACAO",
        "prontuario": { "data": "2026-06-01", "descricao": "Paciente estável" }
      }'
```

#### Contrato de erros (padronizado)

| Status | Situação | Exemplo de mensagem |
|---|---|---|
| `400` | Dados inválidos | Lista completa de inconsistências em `detalhes` |
| `404` | Recurso inexistente | `Paciente não encontrado(a) para o identificador 42.` |
| `409` | Regra de negócio | `O médico já possui consulta agendada para 2030-06-20 às 10:00.` |
| `500` | Erro inesperado | Mensagem neutra, sem detalhes internos (registrados no log) |

---

## 3. Funcionalidades mínimas adicionadas para viabilizar os testes

O projeto original era um *main* de demonstração de mapeamentos JPA, sem contrato externo
observável. Para permitir os testes **de componente** e **de sistema** — que precisam de uma
interface pública — foram adicionadas as funcionalidades abaixo, todas com o mínimo
necessário e devidamente documentadas.

### 3.1 Camada REST (`rest/`)

Antes não havia como o sistema ser exercitado “de fora”; só existiam chamadas diretas aos
DAOs. Foram criados quatro recursos Jakarta REST:

* [`PacienteRecurso`](src/main/java/br/edu/ifrs/vvs/rest/PacienteRecurso.java:31) — CRUD + busca por nome;
* [`MedicoRecurso`](src/main/java/br/edu/ifrs/vvs/rest/MedicoRecurso.java:34) — CRUD + busca por especialidade;
* [`ConsultaRecurso`](src/main/java/br/edu/ifrs/vvs/rest/ConsultaRecurso.java:34) — agendamento, reagendamento, cancelamento e filtros;
* [`SaudeRecurso`](src/main/java/br/edu/ifrs/vvs/rest/SaudeRecurso.java:16) — *health check* usado pelos testes de sistema.

**Justificativa:** sem endpoint público não é possível aplicar RestAssured nem testar o
sistema do ponto de vista do usuário final (critérios 4 e 5 da avaliação).

### 3.2 Camada de serviço (`servico/`)

Os DAOs originais abriam e fechavam transações internamente, o que impedia testar regras de
negócio isoladamente. Foram criadas classes de serviço com `@Transactional`, concentrando as
regras antes dispersas no `main`:

* [`PacienteServico`](src/main/java/br/edu/ifrs/vvs/servico/PacienteServico.java:24) — unicidade de CPF, validação e mapeamento;
* [`MedicoServico`](src/main/java/br/edu/ifrs/vvs/servico/MedicoServico.java:20) — unicidade de CRM e bloqueio de exclusão com consultas;
* [`ConsultaServico`](src/main/java/br/edu/ifrs/vvs/servico/ConsultaServico.java:24) — conflito de agenda de médico e de paciente.

**Justificativa:** regras de negócio explícitas e injetáveis são pré-requisito para testes
unitários com Mockito e para testar caminhos de erro (critério 3).

### 3.3 Validações dedicadas (`servico/Validador*.java`)

Algoritmo completo de CPF (dígitos verificadores), formato de CRM (`12345/UF`), regras de
paciente (nome, idade máxima de 120 anos, e-mails, DDD) e de consulta (expediente das 08:00
às 18:00, proibição de datas passadas).

**Justificativa:** regras de formato e de borda são o alvo natural de particionamento de
equivalência e análise de valor limite. Foram implementadas como classes puras para permitir
testes determinísticos sem infraestrutura. *Bean Validation não foi usado porque a extensão
`quarkus-hibernate-validator` não estava disponível no ambiente offline do laboratório.*

### 3.4 Relógio injetável (`config/ProdutorRelogio.java`)

Substitui chamadas diretas a `LocalDate.now()`/`new Date()` por um `Clock` injetado.

**Justificativa:** sem isso, testes de idade e de “data no passado” quebrariam com o passar
do tempo (risco R1 do plano de teste). Nos testes usa-se `Clock.fixed`.

### 3.5 Dados de demonstração (`src/main/resources/import-dev.sql`)

Script SQL carregado apenas no perfil `%dev` (`quarkus.hibernate-orm.sql-load-script`).

**Justificativa:** permite demonstração manual da API sem MySQL populado. Não afeta os
testes (o perfil `test` usa H2 próprio, sem carga de dados).

### 3.6 Tratamento padronizado de erros (`excecao/`)

Quatro `ExceptionMapper`s traduzem exceções de domínio em HTTP 400/404/409/500, com corpo
JSON padronizado ([`ErroResposta`](src/main/java/br/edu/ifrs/vvs/dto/ErroResposta.java:15)).

**Justificativa:** permite testar o contrato de erro da API (critério de “cenários de erro”).

### 3.7 O que **não** foi adicionado

Para manter o escopo mínimo: não há autenticação, interface web, paginação, *soft delete*,
versionamento de API nem migrações Flyway/Liquibase.

---

## 4. Correções de defeitos motivadas pelos testes

| Defeito | Detectado por | Correção |
|---|---|---|
| `HibernateException` ao atualizar coleção com `orphanRemoval` | `ApiPacienteComponenteTest` | Setters mutam a coleção gerenciada (`clear`+`addAll`) em [`Pessoa`](src/main/java/br/edu/ifrs/vvs/dominio/Pessoa.java:79) |
| Vazamento de dados entre testes de API | Suíte de componente | Limpeza determinística em [`BaseComponenteTest`](src/test/java/br/edu/ifrs/vvs/componente/BaseComponenteTest.java:36) |
| `toUpperCase()` dependente de locale (falha em locales como *tr-TR*) | PMD (`UseLocaleWithCaseConversions`) | `Locale.ROOT` em repositórios e validadores |

---

## 5. Migração do projeto original

| Original | Atual |
|---|---|
| `consultas/`, `pessoas/`, `contatos/`, `util/` (pacotes na raiz) | `br.edu.ifrs.vvs.dominio` |
| `dao/*DAO` com `JPAUtil` e `EntityManager` manual | `repositorio/*` com `EntityManager` injetado |
| `testes/TestesMapeamentos2.main()` | 247 testes automatizados |
| `META-INF/persistence.xml` (MySQL fixo) | `application.properties` com perfis `dev`, `test` e produção |
| `java.util.Date` | `java.time.LocalDate` / `LocalTime` |
| `artifactId = trabalho2_JPA` | `artifactId = Trabalho_VVS` |
