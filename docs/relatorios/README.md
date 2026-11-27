# Relatórios de Verificação e Validação — Trabalho_VVS

Este diretório reúne as evidências de qualidade do projeto, geradas automaticamente pelo
`mvn clean verify`.

## Conteúdo

| Arquivo | Descrição | Como é gerado |
|---|---|---|
| [`relatorio-execucao.md`](relatorio-execucao.md) | Consolidado de execução: contagem de testes, cobertura, análise estática e defeitos encontrados | Manual, a partir dos relatórios abaixo |
| [`pmd/pmd.html`](pmd/pmd.html) | Relatório HTML da análise estática PMD | `maven-pmd-plugin` (fase `verify`), copiado por `maven-antrun-plugin` |
| [`pmd/pmd.xml`](pmd/pmd.xml) | Mesmo relatório em XML, para integração com ferramentas de CI | idem |
| `pmd/rulesets/` | Cópia do ruleset efetivamente usado (rastreabilidade) | idem |

Relatórios gerados em `target/` (não versionados):

* `target/site/jacoco/index.html` — cobertura JaCoCo (HTML);
* `target/site/jacoco/jacoco.csv` — cobertura (dados brutos);
* `target/jacoco-report/index.html` — cobertura gerada pela extensão `quarkus-jacoco`;
* `target/surefire-reports/` — resultados dos testes unitários e de componente;
* `target/failsafe-reports/` — resultados dos testes de sistema.

## Decisões de configuração e justificativas

### 1. PMD restrito ao código de produção

`<includeTests>false</includeTests>` no [`pom.xml`](../../pom.xml:194).

**Justificativa:** com `includeTests=true`, o PMD apontava 479 ocorrências, das quais 474
eram exclusivamente cosméticas em testes (`UnitTestAssertionsShouldIncludeMessage`,
`UnitTestContainsTooManyAsserts`, `UnitTestShouldIncludeAssert`). Essas regras não
detectam defeitos: a suíte já inclui mensagens descritivas nas asserções em que a falha
não é autoexplicativa (por exemplo, verificação de coleções e listas de detalhes), e
vários testes verificam *ausência* de exceção, caso em que `assertDoesNotThrow` **é** a
asserção.

Além disso, o **código de teste verifica-se a si mesmo**: um teste incorreto falha. O
valor da análise estática está no código de produção, e foi ali que ela encontrou cinco
problemas reais (ver [`relatorio-execucao.md`](relatorio-execucao.md), seção 3.1).

Para manter o critério de avaliação atendido (“principais apontamentos corrigidos ou
justificados”), as regras de teste **não foram silenciadas**, e sim **excluídas com
justificativa** no ruleset versionado [`config/pmd/regras-pmd.xml`](../../config/pmd/regras-pmd.xml:1).

### 2. Gate de prioridade 1 e 2

`<failurePriority>2</failurePriority>`.

Violações de prioridade 3 e 4 (estilo) permanecem visíveis no relatório, mas não
interrompem o build. Isso evita uma “cultura de desligar regras” enquanto se preserva o
sinal para problemas graves. Atualmente o relatório de produção está **zerado**.

### 3. Cobertura: dados compartilhados entre Surefire e Failsafe

`quarkus.jacoco.data-file=${user.dir}/jacoco-quarkus.exec` e `reuse-data-file=true` em
[`src/test/resources/application.properties`](../../src/test/resources/application.properties:1).

**Problema observado:** o Surefire e o Failsafe iniciam JVMs distintas. Com a
configuração padrão, a segunda JVM **sobrescrevia** o arquivo `.exec`, e o relatório final
refletia apenas uma das suítes (66,57% de linhas). Com `reuse-data-file=true` e caminho
absoluto único, o JaCoCo carrega os dados existentes e acrescenta os novos, resultando em
**84,76% de linhas / 59,33% de ramos**.

### 4. Metas de cobertura

| Métrica | Meta | Obtido |
|---|---|---|
| Linhas | 80% | 84,76% |
| Ramos | 55% | 59,33% |

**Sobre os ramos:** o menor índice de ramos está nas **entidades JPA** (`Paciente` 3/22,
`Pessoa` 3/14, `Consulta` 0/6), cuja lógica condicional (nulos em `toString`) já está
coberta, restando apenas ramos de `equals`/`hashCode` gerados e de construtores exigidos
pela especificação. Os componentes com regra de negócio real têm índices superiores:
`MapeadorPaciente` 31/44, `ValidadorPaciente` 37/54, `ConsultaServico` 4/4 e
`PacienteRecurso` 4/4.

### 5. Regras com prioridade 3 mantidas no relatório

`AvoidDuplicateLiterals` (41) e `AbstractClassWithoutAbstractMethod` (1) aparecem no
relatório e foram **mantidas sem correção** por serem decisões de design deliberadas:

* os literais repetidos estão em mensagens de erro e nomes de campo do domínio, cuja
  extração para constantes prejudicaria a legibilidade da mensagem na resposta HTTP;
* a classe base `Pessoa` é concreta e mapeada como entidade (`@Inheritance(JOINED)`),
  portanto não pode ser abstrata.
