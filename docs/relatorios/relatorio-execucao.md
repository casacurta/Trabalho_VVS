# Relatório de Execução dos Testes — Trabalho_VVS

**Data da execução:** 27/11/2026
**Comando:** `mvn clean verify`
**Resultado do build:** ✅ **SUCCESS**

---

## 1. Resumo geral

| Suíte | Ferramenta | Casos | Falhas | Erros | Ignorados | Situação |
|---|---|---|---|---|---|---|
| Teste unitário + componente | Surefire 3.5.6 | 240 | 0 | 0 | 0 | ✅ |
| Teste de sistema | Failsafe 3.5.6 | 7 | 0 | 0 | 0 | ✅ |
| **Total** | | **247** | **0** | **0** | **0** | ✅ |

## 2. Cobertura de código (JaCoCo 0.8.13)

| Métrica | Coberto | Total | Percentual | Meta | Situação |
|---|---|---|---|---|---|
| Linhas | 606 | 715 | **84,76%** | ≥ 80% | ✅ |
| Ramos (branches) | 159 | 268 | **59,33%** | ≥ 55% | ✅ |

Relatórios: `target/site/jacoco/index.html` (global), `target/jacoco-report/index.html` (extensão Quarkus) e `target/site/jacoco/jacoco.csv` (dados brutos).

### 2.1 Cobertura por classe (linhas / ramos)

| Pacote | Classe | Linhas | Ramos |
|---|---|---|---|
| servico | MapeadorPaciente | 62/62 | 31/44 |
| servico | ValidadorPaciente | 46/59 | 37/54 |
| servico | ConsultaServico | 45/46 | 4/4 |
| servico | PacienteServico | 34/35 | 3/4 |
| servico | MedicoServico | 34/38 | 5/6 |
| servico | ValidadorCpf | 22/26 | 16/24 |
| servico | ValidadorConsulta | 21/27 | 14/22 |
| servico | ValidadorMedico | 12/17 | 10/16 |
| servico | MapeadorConsulta | 16/16 | 4/8 |
| servico | ValidadorCrm | 4/4 | 4/6 |
| repositorio | ConsultaRepositorio | 35/35 | 5/6 |
| repositorio | MedicoRepositorio | 20/20 | 1/2 |
| repositorio | PacienteRepositorio | 20/20 | 1/2 |
| repositorio | ProntuarioRepositorio | 11/11 | 1/2 |
| rest | PacienteRecurso | 14/14 | 4/4 |
| rest | ConsultaRecurso | 15/16 | 4/4 |
| rest | MedicoRecurso | 13/14 | 2/4 |
| rest | SaudeRecurso | 3/3 | 0/0 |
| excecao | MapeadorRecursoNaoEncontrado | 10/10 | 0/0 |
| excecao | MapeadorRegraDeNegocio | 10/10 | 0/0 |
| excecao | MapeadorDadosInvalidos | 9/9 | 0/0 |
| excecao | MapeadorErroGenerico | 4/13 | 1/2 |
| excecao | DadosInvalidosException | 4/6 | 0/0 |
| excecao | RecursoNaoEncontradoException | 3/3 | 0/0 |
| excecao | RegraDeNegocioException | 2/2 | 0/0 |
| dominio | Paciente | 34/52 | 3/22 |
| dominio | Consulta | 22/28 | 0/6 |
| dominio | Pessoa | 20/34 | 3/14 |
| dominio | Medico | 14/22 | 0/2 |
| dominio | Prontuario | 14/18 | 0/2 |
| dominio | Situacao | 14/16 | 6/8 |
| dominio | Telefone | 8/16 | 0/0 |
| config | ProdutorRelogio | 2/4 | 0/0 |
| dto | Todos os records (9) | 9/9 | — |

> As classes de domínio concentram os acessores simples (`get`/`set`) e construtores exigidos pela especificação Jakarta Persistence — que são, em sua maioria, executados pelo Hibernate em tempo de execução e não por chamada direta nos testes. O mapeador e o validador de paciente, onde reside a lógica real, estão acima de 78% e 62% de cobertura de ramos respectivamente.

## 3. Análise estática (PMD 7.17)

| Item | Resultado |
|---|---|
| Escopo | `src/main/java` (código de produção) |
| Regras | `errorprone` + `bestpractices` ([`config/pmd/regras-pmd.xml`](../../config/pmd/regras-pmd.xml)) |
| Gate de qualidade | Falha do build para prioridade 1 e 2 |
| **Violações encontradas** | **0** |
| Relatório publicado | [`docs/relatorios/pmd/pmd.html`](pmd/pmd.html) e `pmd.xml` |

### 3.1 Apontamentos iniciais e correções aplicadas

Na primeira execução o PMD apontou 5 problemas reais no código de produção, todos **corrigidos**:

| # | Arquivo | Regra | Correção |
|---|---|---|---|
| 1 | [`MedicoRepositorio`](../../src/main/java/br/edu/ifrs/vvs/repositorio/MedicoRepositorio.java:77) | `UseLocaleWithCaseConversions` | `toUpperCase(Locale.ROOT)` na busca por CRM |
| 2 | [`MedicoRepositorio`](../../src/main/java/br/edu/ifrs/vvs/repositorio/MedicoRepositorio.java:93) | `UseLocaleWithCaseConversions` | `toLowerCase(Locale.ROOT)` na busca por especialidade |
| 3 | [`PacienteRepositorio`](../../src/main/java/br/edu/ifrs/vvs/repositorio/PacienteRepositorio.java:96) | `UseLocaleWithCaseConversions` | `toLowerCase(Locale.ROOT)` na busca por nome |
| 4 | [`ValidadorCrm`](../../src/main/java/br/edu/ifrs/vvs/servico/ValidadorCrm.java:24) | `UseLocaleWithCaseConversions` | `toUpperCase(Locale.ROOT)` na normalização do CRM |
| 5 | [`ValidadorCpf`](../../src/main/java/br/edu/ifrs/vvs/servico/ValidadorCpf.java:70) | `UseVarargs` | Parâmetro alterado para `int... pesos` |

> As três ocorrências de `UseLocaleWithCaseConversions` tinham impacto funcional real: em locales como o turco (*tr-TR*), `"i".toUpperCase()` produz `"İ"`, quebrando buscas e validações. A correção com `Locale.ROOT` elimina esse defeito de internacionalização.

Detalhes e justificativas adicionais: [`docs/relatorios/README.md`](README.md).

## 4. Testes de sistema (evidência)

Arquivo: `target/failsafe-reports/failsafe-summary.xml`

```xml
<completed>7</completed>
<errors>0</errors>
<failures>0</failures>
<skipped>0</skipped>
```

| Caso | Fluxo validado | Situação |
|---|---|---|
| `deveExecutarJornadaCompleta` | cadastro → agendamento → agenda → prontuário → cancelamento → exclusão | ✅ |
| `deveRecusarAgendaOcupada` | segundo agendamento no mesmo horário do médico (409) | ✅ |
| `deveRecusarAgendaDuplicadaDoPaciente` | mesmo paciente em dois médicos no mesmo horário (409) | ✅ |
| `deveRejeitarCadastroInvalidoComDetalhes` | 4 inconsistências acumuladas em um único 400 | ✅ |
| `deveRecusarAgendamentoNoPassado` | data retroativa (400) | ✅ |
| `deveRetornar404ParaPacienteInexistente` | GET/PUT/DELETE inexistente (404) | ✅ |
| `deveImpedirExclusaoDeMedicoEmUso` | exclusão de médico com consultas (409) | ✅ |

## 5. Defeitos encontrados pelos testes (e corrigidos)

Os testes revelaram dois defeitos reais durante o desenvolvimento — evidência do valor do processo de V&V:

| # | Defeito | Detectado por | Correção |
|---|---|---|---|
| D1 | `HibernateException: A collection with orphan deletion was no longer referenced...` ao atualizar paciente com telefones | `ApiPacienteComponenteTest#deveAtualizarPaciente` | Setters de coleção passaram a mutar a instância gerenciada (`clear`+`addAll`) em [`Pessoa`](../../src/main/java/br/edu/ifrs/vvs/dominio/Pessoa.java:79) e [`Paciente`](../../src/main/java/br/edu/ifrs/vvs/dominio/Paciente.java:117) |
| D2 | Vazamento de dados entre testes de API (falso 409 por CPF já existente) | Suíte de componente | Limpeza determinística em [`BaseComponenteTest`](../../src/test/java/br/edu/ifrs/vvs/componente/BaseComponenteTest.java:36) + `@TestTransaction` |

## 6. Reprodução

```bash
mvn clean verify        # suíte completa + PMD + cobertura
mvn test                # apenas unitários e de componente
open target/site/jacoco/index.html
open docs/relatorios/pmd/pmd.html
```
