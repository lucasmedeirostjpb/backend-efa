# Polvo API

API REST do sistema de gestão de metas do programa Eficiência em Ação do TJPB.

O projeto foi estruturado com Spring Boot 3, JPA, Liquibase, Keycloak e JaVers, com separação de responsabilidades entre camada de entrada, casos de uso, domínio e configurações transversais. Este README descreve o funcionamento real do backend a partir do código-fonte atual.

## Sumário

- [Visão geral](#visão-geral)
- [Stack e dependências](#stack-e-dependências)
- [Arquitetura](#arquitetura)
- [Domínio e modelo de dados](#domínio-e-modelo-de-dados)
- [Segurança e autenticação](#segurança-e-autenticação)
- [Sistema de delegação](#sistema-de-delegação)
- [Configuração local e execução](#configuração-local-e-execução)
- [Banco de dados e migrations](#banco-de-dados-e-migrations)
- [Contratos e endpoints](#contratos-e-endpoints)
- [Fluxos de negócio](#fluxos-de-negócio)
- [Regras de negócio e validações](#regras-de-negócio-e-validações)
- [Auditoria e histórico](#auditoria-e-histórico)
- [KPIs e Dashboard](#kpis-e-dashboard)
- [Observabilidade e documentação da API](#observabilidade-e-documentação-da-api)
- [Testes](#testes)
- [Pontos de atenção](#pontos-de-atenção)
- [Licença e uso](#licença-e-uso)

## Visão geral

O backend expõe operações para:

- consultar metas e seu histórico completo de alterações;
- cadastrar, atualizar e excluir metas (individual ou em lote);
- atualizar acompanhamento de metas por coordenadores e delegados;
- cadastrar e atualizar catálogos auxiliares de eixos temáticos, setores e coordenadores;
- gerenciar delegações (permitir que outros usuários atualizem metas de um coordenador);
- consolidar indicadores globais de dashboard (KPIs);
- expor histórico completo de auditoria de cada meta.

O centro do domínio é a entidade `Meta`, que se relaciona com `EixoTematico`, `Setor` e `Coordenador`. A aplicação combina:

- separação de comandos e consultas (CQRS leve);
- auditoria técnica via JPA Auditing (data/usuário de criação e atualização);
- auditoria histórica completa via JaVers (histórico de cada mudança);
- autenticação e autorização via JWT emitido pelo Keycloak;
- versionamento e evolução de schema via Liquibase;
- segurança granular baseada em roles e ownership de metas;
- sistema de delegação para permitir colaboração entre coordenadores.

## Stack e dependências

| Item | Versão / uso atual |
| --- | --- |
| Java | 21 |
| Spring Boot | 3.2.5 |
| Spring Web | API REST |
| Spring Data JPA | Persistência |
| Spring Security | Resource Server JWT + method security |
| PostgreSQL | Banco principal local |
| Liquibase | Evolução de schema |
| JaVers | Histórico de alterações |
| MapStruct | Mapeamento DTO ↔ entidade |
| Lombok | Redução de boilerplate |
| SpringDoc OpenAPI | `/v3/api-docs` e `/swagger-ui.html` |
| Hypersistence TSID | Geração de IDs ordenáveis |
| ActiveMQ Artemis | Dependência declarada no build |
| JobRunr | Dependência declarada no build |
| Elastic APM | Attach no bootstrap da aplicação |
| Logstash Logback Encoder | Logs em JSON fora do profile `dev` |

## Arquitetura

O código está organizado com separação próxima a DDD, arquitetura hexagonal e CQRS leve.

```text
br.jus.tjpb.polvo_api
├── application        Casos de uso, commands e handlers
├── boundaries         Controllers REST, DTOs e mappers
├── config             Segurança, JPA Auditing, JaVers e demais configurações
├── domain             Entidades, enums e contratos de repositório
└── shared             DTOs e utilitários compartilhados
```

### Papéis das camadas

| Camada | Responsabilidade |
| --- | --- |
| `boundaries` | Receber requisições HTTP, validar payload e devolver DTOs |
| `application` | Executar regras de caso de uso e orquestrar persistência |
| `domain` | Modelar entidades, enums e interfaces de repositório |
| `config` | Definir segurança, auditoria, logging e bootstrap técnico |
| `shared` | Estruturas reaproveitadas entre módulos |

### Fluxo técnico padrão

1. O controller recebe a requisição e valida o DTO.
2. O controller cria um command e delega para um handler.
3. O handler resolve referências de domínio, aplica regras de negócio e salva dados.
4. O repositório JPA persiste a entidade.
5. O JPA Auditing preenche autoria técnica e o JaVers registra o histórico.
6. O mapper converte a entidade persistida para DTO de saída.

## Domínio e modelo de dados

### Entidade principal: `Meta`

A entidade `Meta` é persistida em `efa_metas` e herda auditoria de criação e atualização. Campos principais:

| Campo | Tipo | Observação |
| --- | --- | --- |
| `id` | `BIGINT` | Gerado com TSID (ordenável e distribuído) |
| `titulo` | `String` | Obrigatório no payload |
| `descricao` | `String` | Texto livre |
| `eixo` | `ManyToOne` | FK para `efa_eixos_tematicos` |
| `setor` | `ManyToOne` | FK para `efa_setores` |
| `coordenador` | `ManyToOne` | FK para `efa_coordenadores` |
| `artigo` | `String` | Campo complementar de referência |
| `anoCiclo` | `Integer` | Obrigatório no payload |
| `deadline` | `LocalDate` | Preenchido automaticamente como 31/12 do anoCiclo se omitido |
| `status` | `StatusMeta` | Estado atual da meta (ENUM) |
| `nivelDificuldade` | `NivelDificuldade` | Situação operacional (ENUM) |
| `evidenciasAuditoria` | `TEXT` | Obrigatório (min 20 caracteres) em cenários de conclusão |
| `observacoes` | `TEXT` | Observações gerais |
| `pMaximo` | `BigDecimal` | Obrigatório no payload - pontos máximos possíveis |
| `estimativaReal` | `BigDecimal` | Usado em metas em andamento |
| `tetoEstimado` | `BigDecimal` | Usado em metas em andamento |
| `pontosAtingidos` | `BigDecimal` | Ajustado automaticamente conforme status |

### Catálogos auxiliares

| Entidade | Tabela | Papel |
| --- | --- | --- |
| `EixoTematico` | `efa_eixos_tematicos` | Classificação temática da meta (ex: "Eficiência Operacional") |
| `Setor` | `efa_setores` | Unidade responsável ou vinculada à meta |
| `Coordenador` | `efa_coordenadores` | Responsável funcional pela meta, com integração Keycloak |
| `Delegacao` | `efa_delegacoes` | Delegados autorizados a editar o acompanhamento das metas do coordenador |

### Hierarquia de entidades

```
DomainEntity (abstrata - apenas id)
├── Delegacao
└── DomainEntityAuditableCreate (abstrata - adiciona criação)
    └── DomainEntityAuditableUpdate (abstrata - adiciona atualização)
        ├── Meta
        ├── Coordenador
        ├── EixoTematico
        └── Setor
```

### Estados de meta

**`StatusMeta`** possui os valores:

- `PENDENTE` - Meta ainda não iniciada
- `EM_ANDAMENTO` - Meta em execução
- `PARCIALMENTE_CUMPRIDA` - Meta cumprida parcialmente
- `TOTALMENTE_CUMPRIDA` - Meta 100% cumprida
- `NAO_CUMPRIDA` - Meta não foi cumprida
- `NAO_SE_APLICA` - Meta não é aplicável (excluída do cálculo de KPIs)

**`NivelDificuldade`** possui os valores:

- `SEM_DIFICULDADES` - Execução normal
- `EM_ALERTA` - Requer atenção
- `SITUACAO_CRITICA` - Situação crítica, risco alto

### Entidade `Coordenador`

Representa um coordenador responsável por metas. Armazenada em `efa_coordenadores`:

| Campo | Tipo | Observação |
| --- | --- | --- |
| `id` | `BIGINT` | TSID |
| `nome` | `String` | Nome completo do coordenador (NOT NULL) |
| `email` | `String` | Email do coordenador |
| `loginKeycloak` | `String` | Username no Keycloak (UNIQUE) - usado para vincular ao JWT |
| `delegacoes` | `List<Delegacao>` | Lista de delegados autorizados (OneToMany) |

### Entidade `Delegacao`

Permite que um coordenador delegue responsabilidades a outro usuário. Armazenada em `efa_delegacoes`:

| Campo | Tipo | Observação |
| --- | --- | --- |
| `id` | `BIGINT` | TSID |
| `coordenador` | `ManyToOne` | Coordenador que está delegando (NOT NULL) |
| `delegadoEmail` | `String` | Email do usuário delegado |
| `delegadoNome` | `String` | Nome do usuário delegado |

**Constraint:** UNIQUE(coordenador_id, delegado_email) - Um mesmo email não pode ser delegado duas vezes para o mesmo coordenador.

**Mudança recente (v1.6):** O campo foi migrado de `delegado_login_keycloak` para `delegado_email` para facilitar o vínculo.

### Relacionamentos do domínio

```
Meta ──────> Coordenador (ManyToOne)
     └────→ EixoTematico (ManyToOne)
     └────→ Setor (ManyToOne)

Coordenador ──> Delegacao (OneToMany)
                └── delegadoEmail

JaVers ────> Meta (histórico completo de alterações)
```

### Auditoria herdada pelas entidades

As entidades auditáveis herdam os campos abaixo (via `DomainEntityAuditableUpdate`):

| Campo | Origem | Descrição |
| --- | --- | --- |
| `dataCriacao` | `@CreatedDate` | Data/hora de criação (imutável) |
| `usuarioCriacao` | `@CreatedBy` | Usuário que criou (imutável) |
| `dataAtualizacao` | `@LastModifiedDate` | Data/hora da última atualização |
| `usuarioAtualizacao` | `@LastModifiedBy` | Usuário que fez a última atualização |

## Segurança e autenticação

### Modelo adotado

A aplicação usa Spring Security como OAuth2 Resource Server, validando JWTs emitidos por Keycloak.

Configuração atual em `application.yaml`:

- `issuer-uri`: `http://localhost:8080/realms/tjpb-polvo`
- `jwk-set-uri`: `http://localhost:8080/realms/tjpb-polvo/protocol/openid-connect/certs`

O decoder usa RS256 e a API opera em modo stateless.

### Como o usuário é identificado

- O principal do token é obtido preferencialmente do claim `preferred_username`.
- Se esse claim não existir, o sistema usa `sub`.
- O `JwtAuthConverter` extrai roles de `resource_access["polvo-app"].roles` e converte para authorities com prefixo `ROLE_`.

Exemplo esperado no token:

```json
{
  "preferred_username": "joao.silva",
  "resource_access": {
    "polvo-app": {
      "roles": ["DIGOV", "COORDENADOR"]
    }
  }
}
```

### Segurança HTTP x segurança por método

A `SecurityFilterChain` libera explicitamente `/api/metas/**` para todos os métodos HTTP. A proteção efetiva dos comandos de meta acontece com `@PreAuthorize` nos métodos do controller, porque o projeto também habilita `@EnableMethodSecurity`.

Na prática:

- leituras de metas são públicas;
- comandos de meta dependem das anotações de método;
- dashboards e catálogos protegidos dependem de autenticação/role;
- Swagger e OpenAPI são públicos.

### Matriz de acesso atual

| Método | Rota | Acesso efetivo |
| --- | --- | --- |
| `GET` | `/api/metas` | Público |
| `GET` | `/api/metas/all` | Público |
| `GET` | `/api/metas/{id}` | Público |
| `GET` | `/api/metas/{id}/historico` | Público |
| `POST` | `/api/metas` | `DIGOV` |
| `POST` | `/api/metas/batch` | `DIGOV` |
| `PUT` | `/api/metas/{id}` | `DIGOV` |
| `PUT` | `/api/metas/{id}/acompanhamento` | `COORDENADOR` dono da meta |
| `DELETE` | `/api/metas/{id}` | `DIGOV` |
| `GET` | `/api/kpis/dashboard` | `COORDENADOR` ou `DIGOV` |
| `GET` | `/api/coordenadores` | Público |
| `GET` | `/api/coordenadores/me/delegacoes` | Autenticado e associado a um coordenador |
| `POST` | `/api/coordenadores/me/delegacoes` | Autenticado e associado a um coordenador |
| `DELETE` | `/api/coordenadores/me/delegacoes/{id}` | Autenticado e associado a um coordenador |
| `GET` | `/api/setores` | Público |
| `POST` | `/api/setores` | `COORDENADOR` ou `DIGOV` |
| `PUT` | `/api/setores/{id}` | `COORDENADOR` ou `DIGOV` |
| `GET` | `/api/eixos` | Público |
| `POST` | `/api/eixos` | `COORDENADOR` ou `DIGOV` |
| `PUT` | `/api/eixos/{id}` | `COORDENADOR` ou `DIGOV` |

### Regra de ownership da meta

No update de acompanhamento, o bean `metaSecurity` compara:

- `meta.coordenador.loginKeycloak`
- com o `preferred_username` presente no JWT.

Assim, um usuário autenticado consegue alterar o acompanhamento da meta quando for o coordenador dono ou quando o seu e-mail estiver cadastrado como delegado do coordenador daquela meta. A edição estrutural permanece exclusiva de `DIGOV`.

### Auditoria de autoria

Há dois mecanismos complementares:

- **JPA Auditing**: usa `AuditorAware<String>` e grava `user.id()` ou `system`.
- **JaVers**: usa `preferred_username` do JWT e grava `sistema` como fallback.

Isso significa que os campos de auditoria JPA e o autor do histórico JaVers podem ter origens ligeiramente diferentes, embora ambos dependam do contexto autenticado.

## Sistema de delegação

O sistema de delegação permite que coordenadores autorizem outros usuários a atualizar o acompanhamento de suas metas. Isso facilita a colaboração e distribui responsabilidades.

### Como funciona

1. **Coordenador autenticado** acessa suas delegações via `/api/coordenadores/me/delegacoes`
2. **Criar delegação**: O coordenador registra o email e nome de um delegado
3. **Permissão automática**: O delegado passa a ter permissão para atualizar o acompanhamento de todas as metas daquele coordenador
4. **Validação**: O sistema verifica automaticamente se o usuário autenticado é o coordenador ou um delegado autorizado

### Endpoints de delegação

| Método | Rota | Autorização | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/coordenadores/me/delegacoes` | Autenticado | Lista delegados do coordenador logado |
| `POST` | `/api/coordenadores/me/delegacoes` | Autenticado | Cria nova delegação |
| `DELETE` | `/api/coordenadores/me/delegacoes/{id}` | Autenticado | Remove delegação |

### Payload de criação de delegação

```json
{
  "delegadoEmail": "usuario@tjpb.jus.br",
  "delegadoNome": "Maria Silva"
}
```

### Validações

- Email é obrigatório e deve ser válido
- Nome é obrigatório
- Não permite duplicatas (constraint UNIQUE na base)
- Apenas o próprio coordenador pode gerenciar suas delegações

### Uso prático

Quando um usuário delegado tenta atualizar o acompanhamento de uma meta:

```
PUT /api/metas/{id}/acompanhamento
Authorization: Bearer {jwt-token}
```

O sistema valida via `@metaSecurity.isDonoDaMeta(#id, #jwt)`:
1. Verifica se o email do JWT corresponde ao `loginKeycloak` do coordenador da meta
2. OU verifica se o email do JWT está na lista de `delegadoEmail` do coordenador da meta
3. Se qualquer validação passar, autoriza a operação

## Configuração local e execução

### Pré-requisitos

- Java 21
- Maven Wrapper do projeto
- PostgreSQL disponível em `localhost:5432`
- Keycloak disponível em `localhost:8080`

### Configuração local atual

O projeto hoje sobe com os seguintes defaults locais definidos em `src/main/resources/application.yaml`:

| Item | Valor atual |
| --- | --- |
| Porta da API | `8081` |
| Banco | `jdbc:postgresql://localhost:5432/db_polvo` |
| Usuário do banco | `postgres` |
| Senha do banco | `postgres` |
| Realm Keycloak | `tjpb-polvo` |
| JPA `ddl-auto` | `validate` |
| Liquibase | habilitado |

Esses valores refletem o estado atual do código e devem ser tratados como configuração local de desenvolvimento, não como recomendação para produção.

### Subindo a aplicação

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Em shells Unix-like:

```bash
./mvnw spring-boot:run
```

### Profile de desenvolvimento

O projeto não possui um `application-dev.yaml` dedicado, mas o `logback-spring.xml` muda o formato de log quando o profile `dev` está ativo.

Exemplo:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev"
```

Com `dev` ativo, os logs saem em texto. Sem esse profile, o appender padrão usa JSON no console.

## Banco de dados e migrations

### Estratégia de schema

O banco é controlado por Liquibase e o Hibernate está configurado com `ddl-auto: validate`. Isso significa que:

- o schema deve existir e estar alinhado com as entidades;
- o Hibernate valida o schema, mas não cria nem altera tabelas;
- a fonte de verdade para evolução estrutural é o changelog do Liquibase.

### Ordem das migrations

O arquivo mestre `db/changelog/db.changelog-master.xml` inclui as seguintes etapas em ordem cronológica:

| Versão | Arquivo | Descrição |
| --- | --- | --- |
| v1.0 | `v1.0-init.sql` | Criação inicial de `metas` com campos básicos e auditoria técnica |
| v1.1 | `v1.1-nova-anatomia.sql` | Criação de `eixos_tematicos` e `setores`, refatoração do modelo de `metas`, remoção do campo `concluida` |
| v1.2 | `v1.2-prefixos-tabelas.sql` | Renomeação de todas as tabelas com prefixo `efa_` para padronização |
| v1.3 | `v1.3-campos-auditoria.sql` | Adição de `nivel_dificuldade`, `evidencias_auditoria` e `observacoes` em `efa_metas` |
| v1.4 | `v1.4-coordenador.sql` | Criação de `efa_coordenadores` e relacionamento com `efa_metas` |
| v1.5 | `v1.5-delegacoes.sql` | Criação de `efa_delegacoes` com UNIQUE(coordenador_id, delegado_login_keycloak) |
| v1.6 | `v1.6-delegacoes-email.sql` | Refatoração: renomeação de coluna de `delegado_login_keycloak` para `delegado_email` |

### Estrutura final do banco

**Principais tabelas do sistema:**

#### efa_metas
Tabela central com aproximadamente 25+ colunas, incluindo:
- Identificação: `id`, `titulo`, `descricao`
- Relacionamentos: `eixo_id`, `setor_id`, `coordenador_id`
- Status e acompanhamento: `status`, `nivel_dificuldade`, `evidencias_auditoria`, `observacoes`
- Valores numéricos: `p_maximo`, `estimativa_real`, `teto_estimado`, `pontos_atingidos`
- Ciclo: `artigo`, `ano_ciclo`, `deadline`
- Auditoria: `data_criacao`, `usuario_criacao`, `data_atualizacao`, `usuario_atualizacao`

#### efa_eixos_tematicos
- `id`, `nome` (UNIQUE), campos de auditoria

#### efa_setores
- `id`, `sigla` (UNIQUE, max 50 chars), `nome`, campos de auditoria

#### efa_coordenadores
- `id`, `nome`, `email`, `login_keycloak` (UNIQUE), campos de auditoria

#### efa_delegacoes
- `id`, `coordenador_id` (FK), `delegado_email`, `delegado_nome`
- CONSTRAINT: UNIQUE(coordenador_id, delegado_email)

#### Tabelas JaVers
- `jv_global_id`, `jv_commit`, `jv_commit_property`, `jv_snapshot`
- Criadas e gerenciadas automaticamente pelo JaVers

### Evolução do modelo de delegações

A tabela de delegações passou por uma evolução importante:

- **v1.5**: Criação inicial usando `delegado_login_keycloak` como identificador
- **v1.6**: Migração para `delegado_email` para facilitar integração e vínculo com usuários

Essa mudança reflete a necessidade de identificar delegados por email ao invés de username do Keycloak, tornando o sistema mais flexível.

## Contratos e endpoints

### Visão geral da API

A API está organizada em módulos funcionais:

- **Metas**: CRUD completo + histórico + acompanhamento
- **KPIs**: Dashboard com indicadores consolidados
- **Catálogos**: Eixos, setores e coordenadores
- **Delegações**: Gestão de delegados por coordenador

### Endpoints de Metas

#### Consultas (MetaQueryController)

| Método | Rota | Autorização | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/metas` | Pública | Lista paginada de metas (padrão 20 itens, ordenação por `titulo`) |
| `GET` | `/api/metas/all` | Pública | Lista completa sem paginação (ordenada por setor e título) |
| `GET` | `/api/metas/{id}` | Pública | Busca uma meta específica por ID |
| `GET` | `/api/metas/{id}/historico` | Pública | Retorna histórico completo de alterações (JaVers) |

#### Comandos (MetaCommandController)

| Método | Rota | Autorização | Descrição |
| --- | --- | --- | --- |
| `POST` | `/api/metas` | `DIGOV` | Cria uma nova meta |
| `POST` | `/api/metas/batch` | `DIGOV` | Cria múltiplas metas em lote |
| `PUT` | `/api/metas/{id}` | `DIGOV` | Atualiza estrutura completa da meta |
| `PUT` | `/api/metas/{id}/acompanhamento` | `DIGOV` ou coordenador dono ou delegado | Atualiza apenas acompanhamento da meta |
| `DELETE` | `/api/metas/{id}` | `DIGOV` | Remove uma meta |

### Endpoints de KPIs e Dashboard

| Método | Rota | Autorização | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/kpis/dashboard` | `COORDENADOR` ou `DIGOV` | Retorna KPIs globais consolidados |

### Endpoints de Catálogos

#### Coordenadores

| Método | Rota | Autorização | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/coordenadores` | Pública | Lista todos os coordenadores |

#### Setores

| Método | Rota | Autorização | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/setores` | Pública | Lista todos os setores |
| `POST` | `/api/setores` | `COORDENADOR` ou `DIGOV` | Cria novo setor |
| `PUT` | `/api/setores/{id}` | `COORDENADOR` ou `DIGOV` | Atualiza setor existente |

#### Eixos Temáticos

| Método | Rota | Autorização | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/eixos` | Pública | Lista todos os eixos temáticos |
| `POST` | `/api/eixos` | `COORDENADOR` ou `DIGOV` | Cria novo eixo temático |
| `PUT` | `/api/eixos/{id}` | `COORDENADOR` ou `DIGOV` | Atualiza eixo temático existente |

### Endpoints de Delegações

| Método | Rota | Autorização | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/coordenadores/me/delegacoes` | Autenticado | Lista delegações do coordenador logado |
| `POST` | `/api/coordenadores/me/delegacoes` | Autenticado | Cria nova delegação |
| `DELETE` | `/api/coordenadores/me/delegacoes/{id}` | Autenticado | Remove delegação específica |

### Payload estrutural de meta (MetaRequestDTO)

**Usado em:** `POST /api/metas`, `POST /api/metas/batch`, `PUT /api/metas/{id}`

| Campo | Tipo | Obrigatório | Validação | Observação |
| --- | --- | --- | --- | --- |
| `titulo` | `String` | Sim | `@NotBlank` | Título da meta |
| `descricao` | `String` | Não | - | Texto livre descritivo |
| `eixoId` | `Long` | Condicional | - | Alternativa a `eixoNome` |
| `eixoNome` | `String` | Condicional | - | Cria eixo se não existir |
| `setorId` | `Long` | Condicional | - | Alternativa a `setorNome` |
| `setorNome` | `String` | Condicional | - | Cria setor se não existir |
| `coordenadorId` | `Long` | Não | - | Alternativa a `coordenadorNome` |
| `coordenadorNome` | `String` | Não | - | Cria coordenador se não existir |
| `artigo` | `String` | Não | - | Referência complementar |
| `anoCiclo` | `Integer` | Sim | `@NotNull`, `@Positive` | Ano do ciclo da meta |
| `deadline` | `LocalDate` | Não | - | Preenchido como 31/12/{anoCiclo} se omitido |
| `status` | `StatusMeta` | Sim | `@NotNull` | Estado atual |
| `nivelDificuldade` | `NivelDificuldade` | Não | - | Nível de dificuldade |
| `evidenciasAuditoria` | `String` | Condicional | `@Size(min=20)` | Obrigatório para status de conclusão |
| `observacoes` | `String` | Não | - | Texto livre |
| `pMaximo` | `BigDecimal` | Sim | `@NotNull`, `@PositiveOrZero` | Pontos máximos possíveis |
| `estimativaReal` | `BigDecimal` | Não | `@PositiveOrZero` | Estimativa real (apenas EM_ANDAMENTO) |
| `tetoEstimado` | `BigDecimal` | Não | `@PositiveOrZero` | Teto estimado (apenas EM_ANDAMENTO) |
| `pontosAtingidos` | `BigDecimal` | Não | `@PositiveOrZero` | Ajustado automaticamente pelo handler |

**Exemplo de payload estrutural:**

```json
{
  "titulo": "Reduzir o tempo médio de tramitação",
  "descricao": "Meta anual vinculada ao planejamento estratégico",
  "eixoNome": "Eficiência Operacional",
  "setorNome": "Secretaria Judiciária",
  "coordenadorNome": "Maria da Silva",
  "artigo": "Art. 5",
  "anoCiclo": 2026,
  "status": "EM_ANDAMENTO",
  "nivelDificuldade": "EM_ALERTA",
  "pMaximo": 100.00,
  "estimativaReal": 45.00,
  "tetoEstimado": 60.00,
  "observacoes": "Meta monitorada mensalmente"
}
```

### Payload de acompanhamento (MetaAcompanhamentoRequestDTO)

**Usado em:** `PUT /api/metas/{id}/acompanhamento`

Este DTO contém **apenas** os campos relacionados ao acompanhamento da meta, preservando os campos estruturais:

| Campo | Tipo | Obrigatório | Validação | Observação |
| --- | --- | --- | --- | --- |
| `status` | `StatusMeta` | Sim | `@NotNull` | Estado atual da meta |
| `nivelDificuldade` | `NivelDificuldade` | Não | - | Nível de dificuldade |
| `evidenciasAuditoria` | `String` | Condicional | `@Size(min=20)` | Obrigatório para status de conclusão |
| `observacoes` | `String` | Não | - | Observações gerais |
| `estimativaReal` | `BigDecimal` | Não | `@PositiveOrZero` | Estimativa real |
| `tetoEstimado` | `BigDecimal` | Não | `@PositiveOrZero` | Teto estimado |
| `pontosAtingidos` | `BigDecimal` | Não | `@PositiveOrZero` | Pontos já atingidos |

**Exemplo de payload de acompanhamento:**

```json
{
  "status": "PARCIALMENTE_CUMPRIDA",
  "nivelDificuldade": "SEM_DIFICULDADES",
  "evidenciasAuditoria": "Relatório mensal de janeiro mostra 65% de conclusão das atividades previstas",
  "observacoes": "Meta progredindo conforme planejado",
  "estimativaReal": 65.00,
  "tetoEstimado": 80.00,
  "pontosAtingidos": 65.00
}
```

### Payload de delegação (DelegacaoRequestDTO)

**Usado em:** `POST /api/coordenadores/me/delegacoes`

| Campo | Tipo | Obrigatório | Validação | Observação |
| --- | --- | --- | --- | --- |
| `delegadoEmail` | `String` | Sim | `@NotBlank`, `@Email` | Email do usuário delegado |
| `delegadoNome` | `String` | Sim | `@NotBlank` | Nome do usuário delegado |

**Exemplo:**

```json
{
  "delegadoEmail": "joao.silva@tjpb.jus.br",
  "delegadoNome": "João Silva"
}
```

### Resposta de dashboard (DashboardKpiDTO)

**Retornado por:** `GET /api/kpis/dashboard`

```json
{
  "totalMetas": 50,
  "somaPontosAplicaveis": 2500.00,
  "somaPontosAtingidos": 1850.50,
  "percentualTracao": 74.02
}
```

| Campo | Tipo | Descrição |
| --- | --- | --- |
| `totalMetas` | `Long` | Total de metas no sistema |
| `somaPontosAplicaveis` | `BigDecimal` | Soma de `pMaximo` de metas aplicáveis (exceto NAO_SE_APLICA) |
| `somaPontosAtingidos` | `BigDecimal` | Soma de `pontosAtingidos` de todas as metas |
| `percentualTracao` | `BigDecimal` | `(atingidos / aplicáveis) * 100` |

### Resposta de histórico (HistoricoAlteracaoDTO)

**Retornado por:** `GET /api/metas/{id}/historico`

```json
[
  {
    "autor": "joao.silva",
    "dataHora": "2026-03-12T10:30:00Z",
    "tipoMudanca": "ATUALIZACAO",
    "propriedadesAlteradas": [
      {
        "propriedade": "status",
        "valorAntigo": "PENDENTE",
        "novoValor": "EM_ANDAMENTO"
      },
      {
        "propriedade": "observacoes",
        "valorAntigo": null,
        "novoValor": "Meta iniciada conforme planejamento"
      }
    ]
  }
]
```

| Campo | Tipo | Descrição |
| --- | --- | --- |
| `autor` | `String` | Username que fez a alteração |
| `dataHora` | `Instant` | Timestamp da alteração |
| `tipoMudanca` | `String` | CRIACAO, ATUALIZACAO ou EXCLUSAO |
| `propriedadesAlteradas` | `List` | Lista de propriedades alteradas |

### Matriz de acesso consolidada

| Método | Rota | Acesso efetivo |
| --- | --- | --- |
| `GET` | `/api/metas` | Público |
| `GET` | `/api/metas/all` | Público |
| `GET` | `/api/metas/{id}` | Público |
| `GET` | `/api/metas/{id}/historico` | Público |
| `POST` | `/api/metas` | `DIGOV` |
| `POST` | `/api/metas/batch` | `DIGOV` |
| `PUT` | `/api/metas/{id}` | `DIGOV` |
| `PUT` | `/api/metas/{id}/acompanhamento` | `DIGOV` OU coordenador dono OU delegado |
| `DELETE` | `/api/metas/{id}` | `DIGOV` |
| `GET` | `/api/kpis/dashboard` | `COORDENADOR` ou `DIGOV` |
| `GET` | `/api/coordenadores` | Público |
| `GET` | `/api/coordenadores/me/delegacoes` | Autenticado |
| `POST` | `/api/coordenadores/me/delegacoes` | Autenticado |
| `DELETE` | `/api/coordenadores/me/delegacoes/{id}` | Autenticado |
| `GET` | `/api/setores` | Público |
| `POST` | `/api/setores` | `COORDENADOR` ou `DIGOV` |
| `PUT` | `/api/setores/{id}` | `COORDENADOR` ou `DIGOV` |
| `GET` | `/api/eixos` | Público |
| `POST` | `/api/eixos` | `COORDENADOR` ou `DIGOV` |
| `PUT` | `/api/eixos/{id}` | `COORDENADOR` ou `DIGOV` |

## Fluxos de negócio

### Handlers de comando implementados

O projeto implementa 5 handlers principais para operações de comando:

| Handler | Comando | Transacional | Descrição |
| --- | --- | --- | --- |
| `CreateMetaCommandHandler` | `CreateMetaCommand` | Sim | Cria nova meta com resolução automática de referências |
| `CreateMetaBatchCommandHandler` | `CreateMetaBatchCommand` | Sim | Cria múltiplas metas em lote numa única transação |
| `UpdateMetaCommandHandler` | `UpdateMetaCommand` | Sim | Atualiza estrutura completa da meta |
| `UpdateMetaAcompanhamentoCommandHandler` | `UpdateMetaAcompanhamentoCommand` | Sim | Atualiza apenas acompanhamento da meta |
| `DeleteMetaCommandHandler` | `DeleteMetaCommand` | Sim | Remove meta e registra exclusão no histórico |

### Criação de meta (individual)

**Fluxo do `CreateMetaCommandHandler`:**

1. **Conversão**: Converte o DTO em entidade usando MetaMapper
2. **Resolução de Eixo**:
   - Tenta buscar por `eixoId` se fornecido
   - Se não encontrar, busca por `eixoNome`
   - Se não existir, cria novo eixo com o nome fornecido
   - Exceção se nenhuma referência for fornecida
3. **Resolução de Setor**:
   - Tenta buscar por `setorId` se fornecido
   - Se não encontrar, busca por `setorNome`
   - Se não existir, cria novo setor (sigla = nome truncado em 50 chars)
   - Exceção se nenhuma referência for fornecida
4. **Resolução de Coordenador**:
   - Tenta buscar por `coordenadorId` se fornecido
   - Se não encontrar, busca por `coordenadorNome`
   - Se não existir, cria novo coordenador com o nome fornecido
   - Campo opcional - pode ficar nulo
5. **Deadline automático**: Se `deadline` for nulo e `anoCiclo` estiver preenchido, define para `31/12/{anoCiclo}`
6. **Sanitização**: Aplica regras de sanitização de valores matemáticos conforme status (detalhes na seção de Regras de Negócio)
7. **Validação**: Valida evidências de auditoria para status de conclusão (mín 20 caracteres)
8. **Persistência**: Salva a meta e registra no histórico JaVers
9. **Resposta**: Retorna DTO da meta criada

### Criação em lote (batch)

**Fluxo do `CreateMetaBatchCommandHandler`:**

- Percorre lista de `MetaRequestDTO` em uma **única transação**
- Aplica o mesmo fluxo de `CreateMetaCommandHandler` para cada item
- Reutiliza a lógica de resolução e criação de catálogos (eixo, setor, coordenador)
- Retorna lista de `MetaResponseDTO` para todas as metas criadas
- Se houver erro em qualquer meta, toda a transação é revertida (atomicidade)

**Vantagens:**
- Reduz roundtrips de rede
- Mantém integridade transacional
- Ideal para importações em massa ou migrations

### Atualização estrutural de meta

**Fluxo do `UpdateMetaCommandHandler`:**

1. Carrega meta existente por ID (exceção se não encontrar)
2. Aplica atualização parcial via mapper (preserva campos não enviados)
3. Re-resolve eixo, setor e coordenador conforme campos enviados
4. Reaplica sanitização matemática conforme novo status
5. Reaplica validação de evidências de auditoria
6. Salva alterações e registra no JaVers
7. Retorna DTO atualizado

**Importante:** Este endpoint preserva a estrutura completa da meta e exige role `DIGOV`.

### Atualização de acompanhamento

**Fluxo do `UpdateMetaAcompanhamentoCommandHandler`:**

1. Carrega meta existente por ID
2. Atualiza **apenas** campos de acompanhamento:
   - `status`
   - `nivelDificuldade`
   - `evidenciasAuditoria`
   - `observacoes`
   - `estimativaReal`
   - `tetoEstimado`
   - `pontosAtingidos`
3. **Preserva todos os campos estruturais** (título, eixo, setor, coordenador, anoCiclo, etc)
4. Reaplica sanitização matemática conforme novo status
5. Reaplica validação de evidências de auditoria
6. Salva e retorna DTO atualizado

**Acesso permitido para:**
- `DIGOV` (acesso total)
- Coordenador dono da meta (via `loginKeycloak`)
- Delegados do coordenador (via `delegadoEmail`)

### Exclusão de meta

**Fluxo do `DeleteMetaCommandHandler`:**

1. Valida que a meta existe (exceção se não encontrar)
2. Executa `deleteById` no repositório
3. JaVers registra evento de exclusão no histórico
4. Retorna resposta vazia (HTTP 204)

**Nota:** O histórico da meta permanece no JaVers mesmo após exclusão.

## Regras de negócio e validações

### 1. Sanitização matemática por status

O sistema ajusta automaticamente valores numéricos conforme o status da meta:

| Status | Regra aplicada |
| --- | --- |
| `PENDENTE` | `pontosAtingidos` = `null`, limpa `estimativaReal` e `tetoEstimado` |
| `EM_ANDAMENTO` | Preserva `estimativaReal`, `tetoEstimado` e `pontosAtingidos` |
| `TOTALMENTE_CUMPRIDA` | `pontosAtingidos` = `pMaximo`, limpa `estimativaReal` e `tetoEstimado` |
| `PARCIALMENTE_CUMPRIDA` | Preserva `pontosAtingidos` informado, limpa `estimativaReal` e `tetoEstimado` |
| `NAO_CUMPRIDA` | `pontosAtingidos` = `0`, limpa `estimativaReal` e `tetoEstimado` |
| `NAO_SE_APLICA` | `pontosAtingidos` = `null`, limpa `estimativaReal` e `tetoEstimado` |

**Razão:** Esta regra garante consistência nos dados e evita valores incoerentes com o estado da meta.

### 2. Evidências obrigatórias em metas concluídas

Para os seguintes status, o campo `evidenciasAuditoria` é **obrigatório** e deve conter **pelo menos 20 caracteres**:

- `TOTALMENTE_CUMPRIDA`
- `PARCIALMENTE_CUMPRIDA`
- `NAO_CUMPRIDA`

**Validação:** `IllegalArgumentException` se a regra não for atendida.

**Razão:** Garantir rastreabilidade e documentação de metas finalizadas.

### 3. Resolução automática de referências (find-or-create)

O backend aceita dois modos de associação para eixo, setor e coordenador:

#### Por ID (quando o registro já existe):
```json
{
  "eixoId": 123,
  "setorId": 456,
  "coordenadorId": 789
}
```

#### Por nome (com criação automática):
```json
{
  "eixoNome": "Eficiência Operacional",
  "setorNome": "Secretaria Judiciária",
  "coordenadorNome": "Maria Silva"
}
```

**Comportamento:**
- Sistema busca primeiro por nome no banco
- Se não encontrar, cria novo registro com o nome fornecido
- Para setores criados automaticamente: `sigla` = `nome` (truncado em 50 chars)
- Para coordenadores criados automaticamente: `loginKeycloak` = `null` inicialmente

**Exceção:** Se nenhuma referência (nem ID nem nome) for fornecida para eixo ou setor, lança erro.

### 4. Deadline automático

Se `deadline` não for informado e `anoCiclo` estiver preenchido:
```
deadline = LocalDate.of(anoCiclo, 12, 31)
```

Exemplo: `anoCiclo: 2026` → `deadline: 2026-12-31`

### 5. Validação de delegações

Ao criar uma delegação:
- Email deve ser válido (`@Email`)
- Nome é obrigatório (`@NotBlank`)
- Sistema verifica duplicatas (UNIQUE constraint no banco)
- Lança `HTTP 409 Conflict` se delegação já existir para aquele email

### 6. Cálculo de KPIs

**Pontos aplicáveis:**
```sql
SUM(CASE WHEN status <> 'NAO_SE_APLICA' THEN pMaximo ELSE 0 END)
```

**Percentual de tração:**
```
percentualTracao = (somaPontosAtingidos / somaPontosAplicaveis) × 100
```

**Arredondamento:** 4 casas decimais na divisão, resultado final em BigDecimal.

### 7. Validações de input (Bean Validation)

O sistema aplica validações padrão Java Bean Validation em todos os DTOs:

| Anotação | Aplicação | Significado |
| --- | --- | --- |
| `@NotNull` | Campos obrigatórios | Campo não pode ser nulo |
| `@NotBlank` | Strings obrigatórias | String não pode ser vazia ou apenas espaços |
| `@Positive` | Números positivos | Valor deve ser maior que zero |
| `@PositiveOrZero` | Números não-negativos | Valor deve ser zero ou positivo |
| `@Email` | Emails | Formato de email válido |
| `@Size(min=X)` | Tamanho mínimo | String deve ter pelo menos X caracteres |

**Resposta em caso de validação falha:** HTTP 400 Bad Request com detalhes dos erros.

### 8. Segurança granular por ownership

A regra `@metaSecurity.isDonoDaMeta(#id, #jwt)` verifica:

1. **É o coordenador?**
   - Compara `meta.coordenador.loginKeycloak` com `jwt.claim("preferred_username")`
   
2. **É um delegado?**
   - Busca em `efa_delegacoes` onde:
     - `coordenador_id` = `meta.coordenador_id`
     - `delegado_email` = email do JWT

3. **Autoriza se:** Qualquer uma das condições acima for verdadeira

**Aplicação:** Endpoint `PUT /api/metas/{id}/acompanhamento`

## KPIs e Dashboard

### Endpoint de dashboard

**Rota:** `GET /api/kpis/dashboard`  
**Autorização:** `@PreAuthorize("hasAnyRole('COORDENADOR', 'DIGOV')")`  
**Controller:** `KpiQueryController`

### Resposta

```json
{
  "totalMetas": 50,
  "somaPontosAplicaveis": 2500.00,
  "somaPontosAtingidos": 1850.50,
  "percentualTracao": 74.02
}
```

### Cálculo dos KPIs

#### Total de Metas
```sql
COUNT(m.id)
```
Conta todas as metas no sistema, independente do status.

#### Pontos Aplicáveis
```sql
SUM(CASE WHEN m.status <> 'NAO_SE_APLICA' THEN m.pMaximo ELSE 0 END)
```
Soma o `pMaximo` de todas as metas **exceto** as com status `NAO_SE_APLICA`.

**Razão:** Metas não aplicáveis não devem influenciar o cálculo de performance.

#### Pontos Atingidos
```sql
SUM(m.pontosAtingidos)
```
Soma todos os `pontosAtingidos`, incluindo valores nulos (tratados como 0).

#### Percentual de Tração
```java
percentualTracao = (somaPontosAtingidos / somaPontosAplicaveis) × 100
```

**Precisão:** 4 casas decimais na divisão intermediária  
**Arredondamento:** `RoundingMode.HALF_UP`  
**Proteção:** Se `somaPontosAplicaveis` for zero, retorna `0.00`

### Exemplo de cálculo

**Cenário:**
- 10 metas no total
- 8 metas aplicáveis (pMaximo = 100 cada) = 800 pontos aplicáveis
- 2 metas NAO_SE_APLICA (ignoradas no cálculo)
- 550 pontos atingidos no total

**Resultado:**
```json
{
  "totalMetas": 10,
  "somaPontosAplicaveis": 800.00,
  "somaPontosAtingidos": 550.00,
  "percentualTracao": 68.75
}
```

### Implementação

A query é implementada como método nativo no `MetaRepository`:

```java
@Query(value = """
    SELECT 
        COUNT(m.id) as totalMetas,
        SUM(CASE WHEN m.status <> 'NAO_SE_APLICA' 
            THEN m.p_maximo ELSE 0 END) as somaPontosAplicaveis,
        SUM(m.pontos_atingidos) as somaPontosAtingidos
    FROM efa_metas m
    """, nativeQuery = true)
DashboardKpiDTO obterKpisGlobaisRaw();
```

O controller então calcula o `percentualTracao` a partir dos valores retornados.

### Casos especiais

| Situação | Comportamento |
| --- | --- |
| Nenhuma meta no sistema | `totalMetas = 0`, `percentualTracao = 0` |
| Todas as metas são NAO_SE_APLICA | `somaPontosAplicaveis = 0`, `percentualTracao = 0` |
| Metas com pontosAtingidos = null | Tratados como 0 na soma |
| Divisão por zero | Proteção: retorna `percentualTracao = 0` |

O sistema implementa um **modelo de auditoria em três camadas** para garantir rastreabilidade completa de todas as operações.

### Camada 1: JPA Auditing (auditoria técnica)

**Configuração:** `@EnableJpaAuditing` em `JpaConfig.java`

**Campos auditados automaticamente:**
- `dataCriacao` + `usuarioCriacao` (imutáveis, preenchidos na criação)
- `dataAtualizacao` + `usuarioAtualizacao` (atualizados em cada mudança)

**Extração do usuário:**
- Interface: `AuditorAware<String>` implementada por `AppUserResolver`
- Fonte primária: `AppUser.id()` do `SecurityContext`
- Fallback: `"system"` quando não há usuário autenticado

**Aplicação:** Todas as entidades que herdam de `DomainEntityAuditableUpdate`

### Camada 2: JaVers (histórico completo de alterações)

**Configuração:** `JaversConfig.java` com `AuthorProvider` customizado

**Funcionalidade:**
- Rastreamento automático de **todas as mudanças** em objetos anotados
- Histórico de criação, atualização e exclusão
- Comparação de valores: valor anterior vs. novo valor
- Suporte a queries temporais

**Extração do autor:**
- Fonte primária: claim `preferred_username` do JWT
- Fallback: `"sistema"` quando não há autenticação

**Ativação:** `@JaversSpringDataAuditable` no `MetaRepository`

**Tabelas no banco:**
- `jv_global_id` - Identificadores globais de objetos
- `jv_commit` - Commits de mudanças
- `jv_commit_property` - Propriedades dos commits (autor, timestamp)
- `jv_snapshot` - Snapshots dos objetos em cada versão

### Camada 3: API de Histórico

**Endpoint:** `GET /api/metas/{id}/historico`

**Retorna:** Lista de `HistoricoAlteracaoDTO` ordenada por data (mais recente primeiro)

**Estrutura da resposta:**

```json
[
  {
    "autor": "joao.silva",
    "dataHora": "2026-03-12T10:30:00Z",
    "tipoMudanca": "CRIACAO",
    "propriedadesAlteradas": []
  },
  {
    "autor": "maria.santos",
    "dataHora": "2026-03-12T14:15:00Z",
    "tipoMudanca": "ATUALIZACAO",
    "propriedadesAlteradas": [
      {
        "propriedade": "status",
        "valorAntigo": "PENDENTE",
        "novoValor": "EM_ANDAMENTO"
      },
      {
        "propriedade": "pontosAtingidos",
        "valorAntigo": null,
        "novoValor": "25.50"
      }
    ]
  },
  {
    "autor": "admin",
    "dataHora": "2026-03-12T16:00:00Z",
    "tipoMudanca": "EXCLUSAO",
    "propriedadesAlteradas": []
  }
]
```

**Tipos de mudança:**
- `CRIACAO` - Meta foi criada (evento `NewObject`)
- `ATUALIZACAO` - Meta foi modificada (eventos `ValueChange`)
- `EXCLUSAO` - Meta foi removida (evento `ObjectRemoved`)

**Processamento:**
1. Query no JaVers: `QueryBuilder.byInstanceId(id, Meta.class).build()`
2. Agrupa mudanças por commit (CommitMetadata)
3. Ordena por data decrescente
4. Extrai autor, timestamp e propriedades alteradas
5. Retorna lista formatada

**Nota:** O histórico permanece disponível mesmo após exclusão da meta.

### Diferenças entre JPA Auditing e JaVers

| Aspecto | JPA Auditing | JaVers |
| --- | --- | --- |
| **Propósito** | Registrar quem/quando criou/atualizou | Histórico completo de mudanças |
| **Granularidade** | Apenas primeira e última alteração | Todas as alterações, propriedade por propriedade |
| **Armazenamento** | Colunas na própria tabela | Tabelas separadas (jv_*) |
| **Autor** | `AppUser.id()` ou `"system"` | `preferred_username` ou `"sistema"` |
| **Acesso** | Direto nas entidades | Via query JaVers ou endpoint de histórico |
| **Exclusão** | Dados perdidos quando entidade é deletada | Histórico preservado |

### Elastic APM (Application Performance Monitoring)

**Biblioteca:** `co.elastic.apm:apm-agent-attach` (v1.43.0)

**Ativação:** `ElasticApmAttacher.attach()` na classe principal (`PolvoEficienciaEmAcaoApplication`)

**Funcionalidade:**
- Rastreamento distribuído de requisições
- Monitoramento de performance de métodos
- Captura de exceções e erros
- Métricas de JVM e aplicação

**Configuração:** Via variáveis de ambiente ou arquivo `elasticapm.properties`

**Nota:** O agente é anexado automaticamente no bootstrap da aplicação, antes da inicialização do Spring.

## Observabilidade e documentação da API

### Logging

**Configuração:** `logback-spring.xml` com suporte a profiles

O sistema possui dois modos de logging:

#### Profile `dev` (desenvolvimento)
- Formato: Texto legível no console
- Nível: INFO (padrão)
- Ideal para: Debug local e desenvolvimento

#### Demais profiles (produção)
- Formato: JSON via `LogstashEncoder`
- Integração: ELK Stack (Elasticsearch, Logstash, Kibana)
- Estruturado para análise e agregação

**Níveis de log especiais** (em `application.yaml`):

```yaml
logging:
  level:
    org.springframework.security: TRACE
    org.springframework.security.oauth2: TRACE
    com.nimbusds: TRACE
```

Esses níveis ajudam a debugar problemas de autenticação OAuth2/JWT.

### Elastic APM (Application Performance Monitoring)

**Biblioteca:** `co.elastic.apm:apm-agent-attach` (v1.43.0)

**Ativação:**
```java
ElasticApmAttacher.attach();
```

Executado na classe principal **antes** da inicialização do Spring Boot.

**Funcionalidades:**
- Rastreamento distribuído de requisições HTTP
- Métricas de performance de métodos
- Monitoramento de queries SQL
- Captura automática de exceções
- Métricas de JVM (heap, GC, threads)

**Configuração:** Via variáveis de ambiente ou arquivo `elasticapm.properties` (não incluído no repositório)

### OpenAPI / Swagger

**Biblioteca:** `springdoc-openapi-starter-webmvc-ui`

**Endpoints disponíveis:**
- **UI interativa:** `/swagger-ui.html` ou `/swagger-ui/index.html`
- **Especificação OpenAPI:** `/v3/api-docs` (JSON)
- **Especificação YAML:** `/v3/api-docs.yaml`

**Acesso:** Públicoconfigurado na `SecurityFilterChain`

**Uso:**
- Exploração interativa de todos os endpoints
- Documentação automática gerada do código
- Teste manual direto pela interface
- Geração de clientes API

### Script de teste manual

**Arquivo:** `teste_api_keycloak.bat`

**Propósito:** Script Windows para testes manuais de:
- Autenticação no Keycloak
- Obtenção de tokens JWT
- Chamadas à API com bearer token

Útil para validação rápida de integração Keycloak → API.

### DevTools

**Biblioteca:** `spring-boot-devtools` (runtime, opcional)

**Funcionalidades:**
- Restart automático em mudanças de código
- LiveReload para reload de recursos estáticos
- Cache desabilitado em desenvolvimento

## Testes

O projeto possui testes automatizados focados em regras de negócio críticas e bootstrap da aplicação.

### Cobertura de testes existente

| Tipo | Classe de teste | Foco | Tecnologia |
| --- | --- | --- | --- |
| Unitário | `CreateMetaCommandHandlerTest` | Regras de sanitização na criação | JUnit 5 + Mockito |
| Unitário | `UpdateMetaCommandHandlerTest` | Regras de sanitização na atualização | JUnit 5 + Mockito |
| Unitário | `UpdateMetaAcompanhamentoCommandHandlerTest` | Regras de sanitização no acompanhamento | JUnit 5 + Mockito |
| Unitário | `MetaMapperTest` | Mapeamento correto de DTOs | JUnit 5 + MapStruct |
| Integração | `DelegacaoControllerTest` | Endpoints de delegação | Spring Boot Test + MockMVC |
| Integração | `MetaCommandControllerSecurityTest` | Segurança dos endpoints de comando | Spring Security Test |
| Integração | `MetaCommandControllerAuthorizationTest` | Autorização granular por ownership | Spring Security Test |
| Integração | `MetaSecurityValidatorTest` | Lógica `isDonoDaMeta()` | Spring Boot Test |
| Smoke | `PolvoEficienciaEmAcaoApplicationTests` | Subida do contexto Spring | Spring Boot Test |

### Regras de negócio validadas

#### Sanitização matemática por status (handlers)
✅ `PENDENTE` → limpa `pontosAtingidos`, `estimativaReal`, `tetoEstimado`  
✅ `EM_ANDAMENTO` → preserva todos os valores  
✅ `TOTALMENTE_CUMPRIDA` → `pontosAtingidos = pMaximo`  
✅ `PARCIALMENTE_CUMPRIDA` → preserva `pontosAtingidos` informado  
✅ `NAO_CUMPRIDA` → `pontosAtingidos = 0`  
✅ `NAO_SE_APLICA` → limpa `pontosAtingidos`

#### Segurança e autorização
✅ `DIGOV` pode atualizar qualquer meta  
✅ Coordenador pode atualizar acompanhamento de suas metas  
✅ Delegado pode atualizar acompanhamento de metas do coordenador  
✅ Negação correta para usuários não autorizados

#### Delegações
✅ Criação de delegação com validações  
✅ Listagem apenas das delegações do coordenador autenticado  
✅ Exclusão apenas de delegações próprias

### Executando os testes

**Via Maven Wrapper:**
```powershell
.\mvnw.cmd test
```

**Via IDE:**
- IntelliJ IDEA: Clique direito na pasta `src/test/java` → "Run Tests"
- VSCode: Use extensão Java Test Runner

**Relatórios:**
- Console: Saída direta
- Surefire: `target/surefire-reports/`

### Áreas com cobertura limitada

As seguintes áreas ainda precisam de mais testes:

- ⚠️ Endpoint de histórico JaVers
- ⚠️ Criação em lote (batch)
- ⚠️ Resolução automática de referências (find-or-create)
- ⚠️ Cálculo de KPIs e dashboard
- ⚠️ Validação de evidências mínimas (20 caracteres)
- ⚠️ Integração completa Liquibase + PostgreSQL
- ⚠️ Queries customizadas de repositório

## Pontos de atenção

### 1. Segurança baseada em method security

⚠️ **Importante:** A `SecurityFilterChain` HTTP libera explicitamente `/api/metas/**` para todos os métodos HTTP (GET, POST, PUT, DELETE).

**Proteção real:** Anotações `@PreAuthorize` nos métodos dos controllers.

**Razão:** Permite consultas públicas de metas, mas restringe comandos via method security.

**Implicação:** Ao debugar problemas de autorização, verificar as anotações de método, não apenas a configuração HTTP.

### 2. Coordenador e delegado usam endpoint separado

✅ **Fluxo correto:**
- **DIGOV:** Usa `PUT /api/metas/{id}` (atualização estrutural completa)
- **Coordenador/Delegado:** Usa `PUT /api/metas/{id}/acompanhamento` (apenas acompanhamento)

❌ **Evitar:**
- Coordenador tentando usar `PUT /api/metas/{id}` → Negado (falta role DIGOV)

**Razão:** Separação de responsabilidades - estrutura vs. acompanhamento.

### 3. Configuração local não externalizada

⚠️ **Atenção:** `application.yaml` contém valores hardcoded:

```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/db_polvo
  username: postgres
  password: postgres
  
oauth2:
  resourceserver:
    jwt:
      issuer-uri: http://localhost:8080/realms/tjpb-polvo
```

**Recomendação para produção:**
- Externalize via variáveis de ambiente
- Use Spring Cloud Config ou similar
- Proteja credenciais com Vault ou AWS Secrets Manager

### 4. Diferença entre autores JPA Auditing e JaVers

⚠️ **Comportamento atual:**
- **JPA Auditing:** Grava `AppUser.id()` (implementação customizada)
- **JaVers:** Grava `preferred_username` do JWT

**Resultado:** Os campos `usuarioCriacao`/`usuarioAtualizacao` podem ter valor ligeiramente diferente do `autor` no histórico JaVers.

**Razão:** Fontes diferentes de extração do usuário.

### 5. Dependências declaradas mas não utilizadas

As seguintes dependências estão no `pom.xml` mas não têm uso evidente no código explorado:

- `spring-boot-starter-artemis` - Messaging com ActiveMQ Artemis
- JobRunr (se declarado) - Schedule de jobs

**Status:** Preparação para features futuras ou legado de configuração inicial.

### 6. Migrations irreversíveis

⚠️ **Atenção:** As migrations Liquibase não possuem rollback explícito.

**Implicação:** Em caso de erro, rollback manual é necessário.

**Recomendação:** Testar migrations em ambiente de homologação antes de produção.

### 7. Histórico JaVers cresce indefinidamente

⚠️ **Observação:** Não há estratégia de archive ou cleanup de histórico antigo configurada.

**Implicação:** As tabelas `jv_*` crescem continuamente.

**Recomendação futuro:** Implementar política de archive/cleanup para históricos muito antigos.

### 8. TSID como gerador de IDs

✅ **Vantagens:**
- IDs ordenáveis por tempo de criação
- Distribuídos sem conflitos
- Melhor performance que UUID em índices B-tree

⚠️ **Atenção:**
- Gerados em aplicação (não no banco)
- Expõem aproximadamente quando o registro foi criado
- Não são criptograficamente seguros

## Licença e uso

Uso interno do Tribunal de Justiça da Paraíba (TJPB).

Sistema desenvolvido para o programa **Eficiência em Ação**.

**Contato técnico:** Departamento de Tecnologia da Informação (DTI) - TJPB
