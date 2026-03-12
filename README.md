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

### Criação de meta

Fluxo do `CreateMetaCommandHandler`:

1. Converte o DTO em entidade com o mapper.
2. Resolve `eixo` por `eixoId`; se não houver, tenta `eixoNome` e cria se necessário.
3. Resolve `setor` por `setorId`; se não houver, tenta `setorNome` e cria se necessário.
4. Resolve `coordenador` por `coordenadorId`; se não houver, tenta `coordenadorNome` e cria se necessário.
5. Se `deadline` vier nulo e `anoCiclo` estiver preenchido, define `31/12/{anoCiclo}`.
6. Sanitiza os valores matemáticos de acordo com o status.
7. Valida regras de auditoria.
8. Persiste a meta e retorna DTO de resposta.

Quando `eixoId` e `eixoNome` estão ausentes, o handler lança erro. O mesmo vale para `setorId` e `setorNome`. Para coordenador, a associação é opcional se nenhum dos dois campos for enviado.

### Criação em lote

O `CreateMetaBatchCommandHandler` aplica a mesma lógica de resolução e validação item a item sobre uma lista de `MetaRequestDTO`.

Características do fluxo:

- percorre todos os itens em um único handler transacional;
- reutiliza a lógica de criação de catálogos por nome;
- devolve uma lista de `MetaResponseDTO`;
- exige role `DIGOV` no controller.

### Atualização estrutural de meta

O `UpdateMetaCommandHandler`:

1. carrega a meta por ID;
2. aplica atualização parcial via mapper;
3. resolve novamente eixo, setor e coordenador pelos campos enviados;
4. reaplica sanitização matemática;
5. reaplica validação de auditoria;
6. salva e devolve o DTO atualizado.

### Atualização de acompanhamento

O `UpdateMetaAcompanhamentoCommandHandler`:

1. carrega a meta por ID;
2. aplica apenas `status`, `nivelDificuldade`, `evidenciasAuditoria`, `observacoes`, `estimativaReal`, `tetoEstimado` e `pontosAtingidos`;
3. preserva todos os campos estruturais da meta;
4. reaplica sanitização matemática;
5. reaplica validação de auditoria;
6. salva e devolve o DTO atualizado.

O acesso a esse endpoint é permitido para:

- `DIGOV`;
- o coordenador dono da meta;
- qualquer usuário autenticado cujo CPF esteja cadastrado em `efa_delegacoes` para o coordenador da meta.

### Exclusão de meta

O `DeleteMetaCommandHandler` valida a existência da meta e então executa `deleteById`. O histórico do JaVers registra a remoção como evento de exclusão.

### Regras de negócio mais relevantes

#### Evidências obrigatórias em metas concluídas

Para os status abaixo:

- `TOTALMENTE_CUMPRIDA`
- `PARCIALMENTE_CUMPRIDA`
- `NAO_CUMPRIDA`

o campo `evidenciasAuditoria` deve conter pelo menos 20 caracteres úteis. Caso contrário, o handler lança `IllegalArgumentException`.

#### Sanitização matemática por status

- Se a meta não estiver em `EM_ANDAMENTO`, `tetoEstimado` e `estimativaReal` são limpos.
- Em `TOTALMENTE_CUMPRIDA`, `pontosAtingidos` recebe `pMaximo`.
- Em `NAO_CUMPRIDA`, `pontosAtingidos` recebe `0`.
- Em `PENDENTE` e `NAO_SE_APLICA`, `pontosAtingidos` é limpo.
- Em `PARCIALMENTE_CUMPRIDA`, o valor já informado de `pontosAtingidos` é preservado.

#### Resolução automática de referências

O backend aceita dois modos de associação para eixo, setor e coordenador:

- por ID, quando o catálogo já existe;
- por nome, com criação automática quando o registro ainda não existe.

No caso de setor, a sigla do registro criado automaticamente usa o próprio nome truncado para no máximo 50 caracteres.

## Auditoria e histórico

### JPA Auditing

O projeto habilita `@EnableJpaAuditing` e usa um `AuditorAware<String>` para preencher campos de autoria nas entidades.

Comportamento atual:

- se houver usuário autenticado, grava `AppUser.id()`;
- se não houver usuário autenticado, grava `system`.

### JaVers

O `MetaRepository` está anotado com `@JaversSpringDataAuditable`, então operações de persistência em `Meta` geram histórico automaticamente.

O autor do commit JaVers é:

- `preferred_username` do JWT, quando disponível;
- `sistema`, como fallback.

### Endpoint de histórico

`GET /api/metas/{id}/historico`:

- valida se a meta existe;
- consulta as mudanças do JaVers por `instanceId`;
- agrupa mudanças por commit;
- classifica eventos em `CRIACAO`, `ATUALIZACAO` e `EXCLUSAO`;
- devolve autor, data/hora e propriedades alteradas de cada commit.

## Observabilidade e documentação da API

### Logging

`logback-spring.xml` define dois formatos:

- `dev`: logs em texto no console;
- demais profiles: logs JSON com `LogstashEncoder`.

Além disso, `application.yaml` eleva para `TRACE` categorias relacionadas a segurança OAuth2:

- `org.springframework.security`
- `org.springframework.security.oauth2`
- `com.nimbusds`

### Elastic APM

A classe principal executa `ElasticApmAttacher.attach()` antes de subir o contexto Spring. Isso indica que o processo tenta anexar o agente APM em tempo de execução.

### OpenAPI / Swagger

Com `springdoc-openapi-starter-webmvc-ui`, a documentação HTTP fica disponível em:

- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`

Essas rotas estão liberadas na configuração de segurança.

### Script utilitário de teste manual

O arquivo `teste_api_keycloak.bat` pode ser usado como ponto de partida para testes manuais de autenticação e chamadas à API em ambiente Windows.

## Testes

O projeto já possui testes automatizados básicos, concentrados em handlers de comando e bootstrap da aplicação.

### Cobertura existente

| Tipo | Arquivo | Cobertura atual |
| --- | --- | --- |
| Unitário | `CreateMetaCommandHandlerTest` | Regras de sanitização por status |
| Unitário | `UpdateMetaCommandHandlerTest` | Regras de sanitização por status |
| Integração básica | `PolvoEficienciaEmAcaoApplicationTests` | Subida do contexto Spring |

### O que esses testes validam hoje

- limpeza de `tetoEstimado` e `estimativaReal` fora de `EM_ANDAMENTO`;
- preenchimento automático de `pontosAtingidos` em `TOTALMENTE_CUMPRIDA`;
- zeramento de `pontosAtingidos` em `NAO_CUMPRIDA`;
- limpeza de `pontosAtingidos` em `NAO_SE_APLICA` e `PENDENTE`;
- preservação de `pontosAtingidos` em `PARCIALMENTE_CUMPRIDA`.

### Lacunas de teste ainda relevantes

- controllers e regras de autorização;
- endpoint de histórico;
- criação em lote;
- resolução automática por nome;
- deadline padrão;
- validação mínima de evidências;
- integração entre Liquibase, PostgreSQL e segurança.

## Pontos de atenção

### 1. Segurança de `/api/metas/**` depende de method security

A chain HTTP está permissiva para todos os verbos em `/api/metas/**`. O bloqueio real acontece nos métodos anotados do controller. Isso é importante para manutenção e troubleshooting de autenticação.

### 2. Coordenador usa endpoint próprio de acompanhamento

Após a correção do backend, o coordenador não deve mais chamar o `PUT /api/metas/{id}` estrutural. O fluxo correto para esse perfil é `PUT /api/metas/{id}/acompanhamento`.

### 3. Configuração local hardcoded

O `application.yaml` atual usa `postgres/postgres` e URIs locais fixas. Para ambientes compartilhados ou produção, isso deve ser externalizado.

### 4. Dependências presentes, mas sem fluxo explícito documentado no código explorado

As dependências de Artemis e JobRunr estão declaradas no `pom.xml`, mas este backend, no estado atual explorado, não expõe fluxo funcional evidente dessas integrações no README nem nos casos de uso principais de meta.

## Licença e uso

Uso interno do Tribunal de Justiça da Paraíba.
