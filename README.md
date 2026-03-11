# Polvo API

API REST do sistema de gestão de metas do programa Eficiência em Ação do TJPB.

O projeto foi estruturado com Spring Boot 3, JPA, Liquibase, Keycloak e JaVers, com separação de responsabilidades entre camada de entrada, casos de uso, domínio e configurações transversais. Este README descreve o funcionamento real do backend a partir do código-fonte atual.

## Sumário

- [Visão geral](#visão-geral)
- [Stack e dependências](#stack-e-dependências)
- [Arquitetura](#arquitetura)
- [Domínio e modelo de dados](#domínio-e-modelo-de-dados)
- [Segurança e autenticação](#segurança-e-autenticação)
- [Configuração local e execução](#configuração-local-e-execução)
- [Banco de dados e migrations](#banco-de-dados-e-migrations)
- [Contratos e endpoints](#contratos-e-endpoints)
- [Fluxos de negócio](#fluxos-de-negócio)
- [Auditoria e histórico](#auditoria-e-histórico)
- [Observabilidade e documentação da API](#observabilidade-e-documentação-da-api)
- [Testes](#testes)
- [Pontos de atenção](#pontos-de-atenção)
- [Licença e uso](#licença-e-uso)

## Visão geral

O backend expõe operações para:

- consultar metas e seu histórico;
- cadastrar, atualizar e excluir metas;
- cadastrar e atualizar catálogos auxiliares de eixos e setores;
- listar coordenadores;
- consolidar indicadores globais de dashboard.

O centro do domínio é a entidade `Meta`, que se relaciona com `EixoTematico`, `Setor` e `Coordenador`. A aplicação combina:

- leitura e escrita separadas por responsabilidade de controller e handler;
- auditoria técnica via JPA Auditing;
- auditoria histórica via JaVers;
- autenticação via JWT emitido pelo Keycloak;
- versionamento de schema via Liquibase.

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
| `id` | `BIGINT` | Gerado com TSID |
| `titulo` | `String` | Obrigatório no payload |
| `descricao` | `String` | Texto livre |
| `eixo` | `ManyToOne` | FK para `efa_eixos_tematicos` |
| `setor` | `ManyToOne` | FK para `efa_setores` |
| `coordenador` | `ManyToOne` | FK para `efa_coordenadores` |
| `artigo` | `String` | Campo complementar de referência |
| `anoCiclo` | `Integer` | Obrigatório no payload |
| `deadline` | `LocalDate` | Pode ser preenchido automaticamente |
| `status` | `StatusMeta` | Estado atual da meta |
| `nivelDificuldade` | `NivelDificuldade` | Situação operacional |
| `evidenciasAuditoria` | `TEXT` | Exigido em cenários de conclusão |
| `observacoes` | `TEXT` | Observações gerais |
| `pMaximo` | `BigDecimal` | Obrigatório no payload |
| `estimativaReal` | `BigDecimal` | Usado em metas em andamento |
| `tetoEstimado` | `BigDecimal` | Usado em metas em andamento |
| `pontosAtingidos` | `BigDecimal` | Pode ser ajustado automaticamente |

### Catálogos auxiliares

| Entidade | Tabela | Papel |
| --- | --- | --- |
| `EixoTematico` | `efa_eixos_tematicos` | Classificação temática da meta |
| `Setor` | `efa_setores` | Unidade responsável ou vinculada |
| `Coordenador` | `efa_coordenadores` | Responsável funcional pela meta |

### Estados de meta

`StatusMeta` possui os valores:

- `PENDENTE`
- `EM_ANDAMENTO`
- `PARCIALMENTE_CUMPRIDA`
- `TOTALMENTE_CUMPRIDA`
- `NAO_CUMPRIDA`
- `NAO_SE_APLICA`

`NivelDificuldade` possui os valores:

- `SEM_DIFICULDADES`
- `EM_ALERTA`
- `SITUACAO_CRITICA`

### Auditoria herdada pelas entidades

As entidades auditáveis herdam os campos abaixo:

| Campo | Origem |
| --- | --- |
| `dataCriacao` | `@CreatedDate` |
| `usuarioCriacao` | `@CreatedBy` |
| `dataAtualizacao` | `@LastModifiedDate` |
| `usuarioAtualizacao` | `@LastModifiedBy` |

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

Assim, um usuário com role `COORDENADOR` só consegue alterar o acompanhamento da própria meta. A edição estrutural permanece exclusiva de `DIGOV`.

### Auditoria de autoria

Há dois mecanismos complementares:

- JPA Auditing: usa `AuditorAware<String>` e grava `user.id()` ou `system`.
- JaVers: usa `preferred_username` do JWT e grava `sistema` como fallback.

Isso significa que os campos de auditoria JPA e o autor do histórico JaVers podem ter origens ligeiramente diferentes, embora ambos dependam do contexto autenticado.

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
- a fonte de verdade para evolução estrutural é o changelog.

### Ordem das migrations

O arquivo mestre inclui as seguintes etapas:

1. `v1.0-init.sql`: criação inicial de `metas` e campos básicos de auditoria.
2. `v1.1-nova-anatomia.sql`: criação de eixos e setores, remoção do campo `concluida` e expansão do modelo de meta.
3. `v1.2-prefixos-tabelas.sql`: renomeação para tabelas com prefixo `efa_`.
4. `v1.3-campos-auditoria.sql`: inclusão de `nivel_dificuldade`, `evidencias_auditoria` e `observacoes` em `efa_metas`.
5. `v1.4-coordenador.sql`: criação de `efa_coordenadores` e associação com `efa_metas`.

### Tabelas principais do modelo atual

| Tabela | Finalidade |
| --- | --- |
| `efa_metas` | Entidade principal das metas |
| `efa_eixos_tematicos` | Catálogo de eixos |
| `efa_setores` | Catálogo de setores |
| `efa_coordenadores` | Catálogo de coordenadores |
| `jv_*` | Tabelas internas do JaVers, criadas pelo starter SQL |

## Contratos e endpoints

### Payload estrutural de meta

`MetaRequestDTO` aceita os seguintes campos:

| Campo | Obrigatório | Observação |
| --- | --- | --- |
| `titulo` | Sim | `@NotBlank` |
| `descricao` | Não | Texto livre |
| `eixoId` | Não | Alternativa a `eixoNome` |
| `setorId` | Não | Alternativa a `setorNome` |
| `eixoNome` | Não | Alternativa a `eixoId` |
| `setorNome` | Não | Alternativa a `setorId` |
| `coordenadorId` | Não | Alternativa a `coordenadorNome` |
| `coordenadorNome` | Não | Alternativa a `coordenadorId` |
| `artigo` | Não | Texto complementar |
| `anoCiclo` | Sim | `@NotNull` |
| `deadline` | Não | Se ausente, pode ser preenchido automaticamente |
| `status` | Sim | `@NotNull` |
| `nivelDificuldade` | Não | Enum |
| `evidenciasAuditoria` | Não | Pode se tornar obrigatória pela regra de negócio |
| `observacoes` | Não | Texto livre |
| `pMaximo` | Sim | `@PositiveOrZero` |
| `estimativaReal` | Não | `@PositiveOrZero` |
| `tetoEstimado` | Não | `@PositiveOrZero` |
| `pontosAtingidos` | Não | `@PositiveOrZero`, mas pode ser sobrescrito pelo handler |

Exemplo de payload estrutural:

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
  "pMaximo": 100,
  "estimativaReal": 45,
  "tetoEstimado": 60,
  "observacoes": "Meta monitorada mensalmente"
}
```

### Endpoints expostos

#### Metas

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/metas` | Lista paginada de metas, padrão 20 itens, ordenação por `titulo` |
| `GET` | `/api/metas/all` | Lista completa sem paginação, ordenada por setor e título |
| `GET` | `/api/metas/{id}` | Busca uma meta por ID |
| `GET` | `/api/metas/{id}/historico` | Retorna histórico consolidado do JaVers |
| `POST` | `/api/metas` | Cria uma meta |
| `POST` | `/api/metas/batch` | Cria metas em lote |
| `PUT` | `/api/metas/{id}` | Atualiza a estrutura completa da meta |
| `PUT` | `/api/metas/{id}/acompanhamento` | Atualiza apenas acompanhamento e auditoria da meta |
| `DELETE` | `/api/metas/{id}` | Remove uma meta |

### Payload de acompanhamento da meta

O endpoint de acompanhamento aceita apenas estes campos:

| Campo | Obrigatório | Observação |
| --- | --- | --- |
| `status` | Sim | `@NotNull` |
| `nivelDificuldade` | Não | Enum |
| `evidenciasAuditoria` | Não | Pode se tornar obrigatória pela regra de negócio |
| `observacoes` | Não | Texto livre |
| `estimativaReal` | Não | `@PositiveOrZero` |
| `tetoEstimado` | Não | `@PositiveOrZero` |
| `pontosAtingidos` | Não | `@PositiveOrZero` |

Exemplo de payload de acompanhamento:

```json
{
  "status": "EM_ANDAMENTO",
  "nivelDificuldade": "EM_ALERTA",
  "observacoes": "Acompanhamento atualizado pelo coordenador",
  "estimativaReal": 45,
  "tetoEstimado": 60,
  "pontosAtingidos": 10
}
```

#### Dashboard

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/kpis/dashboard` | Retorna total de metas, soma de pontos aplicáveis, pontos atingidos e percentual de tração |

O cálculo de KPIs considera:

- total de metas = `COUNT(m.id)`;
- pontos aplicáveis = soma de `pMaximo` para metas cujo status não é `NAO_SE_APLICA`;
- pontos atingidos = soma de `pontosAtingidos`;
- tração = `atingidos / aplicáveis * 100`, com quatro casas na divisão intermediária.

#### Catálogos

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/coordenadores` | Lista coordenadores por nome |
| `GET` | `/api/setores` | Lista setores |
| `POST` | `/api/setores` | Cria setor |
| `PUT` | `/api/setores/{id}` | Atualiza setor |
| `GET` | `/api/eixos` | Lista eixos |
| `POST` | `/api/eixos` | Cria eixo |
| `PUT` | `/api/eixos/{id}` | Atualiza eixo |

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
