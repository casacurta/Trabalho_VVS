# Plano de Teste — Trabalho_VVS

**Projeto:** Trabalho_VVS — Sistema de Clínica Médica (API REST)
**Disciplina:** Verificação e Validação de Sistemas
**Versão deste documento:** 1.0
**Base:** evolução do projeto de mapeamento JPA (pacientes, médicos, consultas e prontuários) para uma aplicação [Quarkus](pom.xml:19) com API REST, testes automatizados, análise estática e cobertura de código.

---

## 1. Objetivo

Aplicar, em um sistema real, as técnicas de Verificação e Validação: planejamento de testes, análise estática, teste unitário, teste de componente e teste de sistema, demonstrando por meio de evidências (relatórios e métricas) que o software atende aos requisitos funcionais e de qualidade definidos.

## 2. Escopo do teste

### 2.1 O que **será** testado

| Área | Artefatos | Técnica |
|---|---|---|
| Domínio | [`Paciente`](src/main/java/br/edu/ifrs/vvs/dominio/Paciente.java:25), [`Pessoa`](src/main/java/br/edu/ifrs/vvs/dominio/Pessoa.java:23), [`Medico`](src/main/java/br/edu/ifrs/vvs/dominio/Medico.java:16), [`Consulta`](src/main/java/br/edu/ifrs/vvs/dominio/Consulta.java:25), [`Prontuario`](src/main/java/br/edu/ifrs/vvs/dominio/Prontuario.java:17), [`Telefone`](src/main/java/br/edu/ifrs/vvs/dominio/Telefone.java:16), [`Situacao`](src/main/java/br/edu/ifrs/vvs/dominio/Situacao.java:10) | Unitário |
| Validações | [`ValidadorCpf`](src/main/java/br/edu/ifrs/vvs/servico/ValidadorCpf.java:10), [`ValidadorCrm`](src/main/java/br/edu/ifrs/vvs/servico/ValidadorCrm.java:11), [`ValidadorPaciente`](src/main/java/br/edu/ifrs/vvs/servico/ValidadorPaciente.java:20), [`ValidadorMedico`](src/main/java/br/edu/ifrs/vvs/servico/ValidadorMedico.java:10), [`ValidadorConsulta`](src/main/java/br/edu/ifrs/vvs/servico/ValidadorConsulta.java:13) | Unitário (particionamento de equivalência e análise de valor limite) |
| Mapeamento DTO ↔ entidade | [`MapeadorPaciente`](src/main/java/br/edu/ifrs/vvs/servico/MapeadorPaciente.java:22), [`MapeadorConsulta`](src/main/java/br/edu/ifrs/vvs/servico/MapeadorConsulta.java:12) | Unitário |
| Regras de negócio | [`PacienteServico`](src/main/java/br/edu/ifrs/vvs/servico/PacienteServico.java:24), [`MedicoServico`](src/main/java/br/edu/ifrs/vvs/servico/MedicoServico.java:20), [`ConsultaServico`](src/main/java/br/edu/ifrs/vvs/servico/ConsultaServico.java:24) | Unitário com mocks (Mockito) |
| Tratamento de erros HTTP | [`MapeadorDadosInvalidos`](src/main/java/br/edu/ifrs/vvs/excecao/MapeadorDadosInvalidos.java:16) e demais mapeadores | Unitário |
| Persistência | Repositórios JPA + H2 em memória | Componente |
| API REST | Endpoints `/api/pacientes`, `/api/medicos`, `/api/consultas`, `/api/saude` | Componente (RestAssured) |
| Fluxos de ponta a ponta | Jornada clínica completa e fluxos alternativos | Sistema |
| Código-fonte de produção | Todo o `src/main/java` | Análise estática (PMD) |

### 2.2 O que **não** será testado

* **Interface gráfica:** o sistema não possui UI; o consumo se dá via API REST/JSON.
* **Autenticação e autorização:** recurso não implementado (declarado como trabalho futuro).
* **Desempenho, carga e estresse:** fora do escopo de V&V funcional deste trabalho.
* **Testes de aceitação com usuários reais (UAT):** substituídos por testes de sistema automatizados.
* **Compatibilidade entre navegadores / dispositivos:** não aplicável a uma API.
* **Ambiente nativo (GraalVM):** o perfil `native` existe no [`pom.xml`](pom.xml:317), mas não é exercitado neste trabalho.
* **Integração real com MySQL:** os testes usam H2 em memória; a configuração MySQL é validada apenas em execução manual (`%dev`/produção).

## 3. Estratégia de teste

Pirâmide de testes adotada, com volume decrescente e granularidade crescente:

```
        /\
       /  \      Teste de Sistema     7 casos   (Failsafe, API ponta a ponta)
      /----\
     /      \    Teste de Componente  49 casos  (Surefire + QuarkusTest, H2/RestAssured)
    /--------\
   /          \  Teste Unitário       184 casos (JUnit 5 + Mockito, sem banco)
  /------------\
 /  Análise Estática (PMD + JaCoCo em todo o build)
/________________\
```

### 3.1 Teste unitário (JUnit 5 + Mockito)

* **Isolamento:** regras de negócio são testadas com repositórios substituídos por mocks (`@Mock`/`@InjectMocks`), garantindo foco na unidade.
* **Determinismo temporal:** serviços dependem de [`Clock`](src/main/java/br/edu/ifrs/vvs/config/ProdutorRelogio.java:14) injetado; nos testes usa-se [`Clock.fixed`](src/test/java/br/edu/ifrs/vvs/unitario/ConsultaServicoTest.java:63), eliminando flakiness.
* **Técnicas de projeto de casos:**
  * particionamento de equivalência (CPF válido/inválido, situação existente/inexistente);
  * análise de valor limite (nome com 2 e 3 caracteres; idade de 120 e 121 anos; horário 07:59/08:00/17:59/18:00; DDD 10/11/99);
  * tabela de decisão (regras de conflito de agenda: médico × paciente);
  * adivinhação de erros (nulos, listas vazias, elementos nulos, JSON malformado).
* **Casos de sucesso, falha e exceção** obrigatórios em cada unidade (critério de avaliação 3).

### 3.2 Teste de componente

* **Persistência:** [`@QuarkusTest`](src/test/java/br/edu/ifrs/vvs/componente/RepositoriosComponenteTest.java:43) + `@TestTransaction` — cada caso roda em transação revertida, com H2 real, validando cascatas, restrição `uk_consulta_agenda` e consultas JPQL.
* **API REST:** RestAssured contra a aplicação em execução, validando status HTTP (200/201/204/400/404/409), cabeçalho `Location`, corpo JSON e o contrato de erros.
* **Isolamento entre testes:** a classe base [`BaseComponenteTest`](src/test/java/br/edu/ifrs/vvs/componente/BaseComponenteTest.java:20) limpa as tabelas antes de cada teste (ordem que respeita chaves estrangeiras).

### 3.3 Teste de sistema

* RestAssured executando jornadas completas (cadastro → agendamento → atendimento → cancelamento → exclusão) e fluxos alternativos de erro.
* Executados pelo **Failsafe** na fase `verify` (sufixo `IT`), separados da suíte rápida.

### 3.4 Análise estática

* **PMD 7.17** integrado ao build ([`pom.xml`](pom.xml:186)), com conjunto de regras próprio em [`config/pmd/regras-pmd.xml`](config/pmd/regras-pmd.xml:1).
* Categorias: *errorprone* e *bestpractices* (foco em defeitos reais, não em estilo).
* **Gate:** violações de prioridade 1 e 2 interrompem o build; relatório HTML/XML copiado para [`docs/relatorios/pmd/`](docs/relatorios/pmd).

### 3.5 Cobertura de código

* **JaCoCo** via extensão `quarkus-jacoco` (instrumentação do Quarkus) + relatório e *check* do plugin JaCoCo.
* Dados acumulados entre Surefire e Failsafe (mesmo `jacoco-quarkus.exec`, `reuse-data-file=true`).
* **Gate:** mínimo de **80% de linhas** e **55% de ramos** — o build falha abaixo disso.

## 4. Critérios de entrada e de saída

### 4.1 Critérios de entrada (quando começar a testar)

1. Código compila sem erros (`mvn -q compile`).
2. Esquema do banco é criado automaticamente no H2 de teste (`drop-and-create`).
3. Suíte de testes versionada e executável via `mvn test` / `mvn verify`.
4. Regras de negócio documentadas e rastreáveis aos casos de teste.

### 4.2 Critérios de saída (quando considerar concluído)

| Critério | Meta | Situação |
|---|---|---|
| Testes unitários, de componente e de sistema verdes | 100% | ✅ 247/247 |
| Cobertura de linhas | ≥ 80% | ✅ 84,76% |
| Cobertura de ramos | ≥ 55% | ✅ 59,33% |
| Violações PMD de prioridade 1 e 2 | 0 | ✅ 0 |
| Requisitos funcionais principais cobertos por teste de sistema | 100% | ✅ 7 fluxos |
| `mvn clean verify` | Sucesso | ✅ Sucesso |

## 5. Riscos e mitigação

| # | Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|---|
| R1 | Testes dependentes de data real (`LocalDate.now()`) quebram em outra data | Média | Alto | Injeção de [`Clock`](src/main/java/br/edu/ifrs/vvs/config/ProdutorRelogio.java:14) e uso de `Clock.fixed` nos testes |
| R2 | Dados residuais entre testes de API causando falso 409 | Alta | Alto | Limpeza determinística em [`BaseComponenteTest`](src/test/java/br/edu/ifrs/vvs/componente/BaseComponenteTest.java:36) e `@TestTransaction` |
| R3 | Banco MySQL indisponível na máquina do avaliador | Alta | Alto | Perfil de teste com H2 em memória ([`src/test/resources/application.properties`](src/test/resources/application.properties:1)) |
| R4 | Substituição de coleções gerenciadas pelo Hibernate (`orphanRemoval`) causa exceção | Média | Alto | Setters mutam a coleção no lugar (`clear`+`addAll`) — ver [`Pessoa`](src/main/java/br/edu/ifrs/vvs/dominio/Pessoa.java:79) |
| R5 | Flakiness por ordem de execução | Média | Médio | Cada teste prepara seus próprios dados; sem dependência de estado global |
| R6 | Falsos negativos de cobertura (arquivo `.exec` sobrescrito entre JVMs) | Alta | Médio | `reuse-data-file=true` + caminho absoluto comum ao Surefire e Failsafe |
| R7 | Análise estática ruidosa prejudicando a leitura do relatório | Média | Baixo | Regras cosméticas de teste excluídas, com justificativa documentada |

## 6. Cronograma das atividades

| Etapa | Atividade | Duração | Responsável |
|---|---|---|---|
| 1 | Levantamento de requisitos e modelagem do domínio | 1 dia | Equipe |
| 2 | Migração do projeto JPA para Quarkus (CDI, REST, transações) | 3 dias | Equipe |
| 3 | Elaboração deste plano de teste e definição de RNFs | 0,5 dia | Equipe |
| 4 | Configuração de PMD + JaCoCo no build | 0,5 dia | Equipe |
| 5 | Implementação dos testes unitários (domínio, validadores, serviços) | 2 dias | Equipe |
| 6 | Implementação dos testes de componente (repositórios + API REST) | 2 dias | Equipe |
| 7 | Implementação dos testes de sistema (jornadas) | 1 dia | Equipe |
| 8 | Correção dos apontamentos de análise estática | 0,5 dia | Equipe |
| 9 | Execução do `clean verify`, coleta de evidências e relatório final | 1 dia | Equipe |
| **Total** | | **~11,5 dias** | |

## 7. Ambiente e ferramentas

| Item | Versão/Configuração |
|---|---|
| Linguagem | Java 25 ([`pom.xml`](pom.xml:14)) |
| Framework de aplicação | Quarkus 3.39.2 |
| Persistência | Hibernate ORM 6 (Jakarta Persistence 3.1) |
| Build | Apache Maven 3.9 |
| Framework de teste | JUnit 5 (Jupiter), Mockito 5, RestAssured |
| Banco de testes | H2 em memória (modo MySQL) |
| Banco de produção/dev | MySQL/MariaDB |
| Análise estática | PMD 7.17 + `maven-pmd-plugin` 3.28 |
| Cobertura | JaCoCo 0.8.13 + `quarkus-jacoco` |
| Relatórios | Surefire/Failsafe (XML/TXT), PMD (HTML/XML), JaCoCo (HTML/CSV) |
| Sistema operacional de referência | Linux (Kali), OpenJDK Temurin 25 |

## 8. Rastreabilidade requisito → teste

| Requisito | Casos de teste |
|---|---|
| RF01 — Cadastrar paciente com CPF válido e único | `PacienteServicoTest`, `ApiPacienteComponenteTest`, `JornadaClinicaIT` |
| RF02 — Rejeitar dados inválidos com lista de erros | `ValidadorPacienteTest`, `ApiPacienteComponenteTest#deveRecusarNomeInvalido` |
| RF03 — Cadastrar médico com CRM no padrão `12345/UF` | `ValidadorCrmTest`, `ApiConsultaComponenteTest#deveCadastrarMedico` |
| RF04 — Agendar consulta sem conflito de agenda | `ConsultaServicoTest`, `ApiConsultaComponenteTest`, `JornadaClinicaIT` |
| RF05 — Impedir conflito de médico e de paciente | `ConsultaServicoTest`, `ApiConsultaComponenteTest#deveRecusarConflito*` |
| RF06 — Restringir atendimento ao expediente (08:00–18:00) | `ValidadorConsultaTest`, `ApiConsultaComponenteTest#deveRecusarHorarioForaDoExpediente` |
| RF07 — Atualizar prontuário/situação do paciente | `MapeadorPacienteTest`, `JornadaClinicaIT#deveExecutarJornadaCompleta` |
| RF08 — Excluir médico apenas sem consultas | `MedicoServicoTest`, `ApiConsultaComponenteTest#deveImpedirExclusaoDeMedicoComConsulta` |
| RF09 — Retornar erros padronizados (400/404/409) | `MapeadoresExcecaoTest`, testes de componente |
| RNF01 — Tempo de resposta e consistência transacional | `@Transactional` nos serviços + testes de componente |
| RNF02 — Código sem violações críticas de análise estática | PMD com `failurePriority=2` |
| RNF03 — Cobertura mínima de testes | JaCoCo `check` (80%/55%) |

## 9. Evidências geradas

| Evidência | Local |
|---|---|
| Relatório de análise estática (PMD) | [`docs/relatorios/pmd/`](docs/relatorios/pmd) |
| Relatório de cobertura (JaCoCo HTML/CSV) | `target/site/jacoco/index.html` e `target/jacoco-report/` |
| Resultados dos testes (Surefire/Failsafe) | `target/surefire-reports/` e `target/failsafe-reports/` |
| Relatório de execução consolidado | [`docs/relatorios/relatorio-execucao.md`](docs/relatorios/relatorio-execucao.md) |
| Justificativa dos apontamentos | [`docs/relatorios/README.md`](docs/relatorios/README.md) |

## 10. Como reproduzir

```bash
# Suíte completa: unitários + componentes + sistema + PMD + cobertura
mvn clean verify

# Somente testes rápidos (unitários e de componente)
mvn test

# Somente testes de sistema
mvn verify -DskipITs=false

# Execução manual da aplicação (perfil dev, H2 em memória com dados de exemplo)
mvn quarkus:dev
```
