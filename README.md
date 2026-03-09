# 🐙 Polvo API — Eficiência em Ação

**Sistema de Gestão de Metas** do TJPB (Tribunal de Justiça da Paraíba).

API RESTful desenvolvida com **Spring Boot 3**, protegida com **OAuth2/Keycloak** e persistência em **PostgreSQL** com versionamento de schema via **Liquibase**. 
Recém-refatorada para adotar as melhores práticas arquiteturais estabelecidas pelo TJPB, utilizando **Domain-Driven Design (DDD)**, **Arquitetura Hexagonal** e **CQRS**.

---

## 📋 Índice

- [🐙 Polvo API — Eficiência em Ação](#-polvo-api--eficiência-em-ação)
  - [📋 Índice](#-índice)
  - [🛠 Stack Tecnológica](#-stack-tecnológica)
  - [🏗 Arquitetura](#-arquitetura)
  - [🗃 Modelo de Dados](#-modelo-de-dados)
    - [Tabela `efa_metas`](#tabela-efa_metas)
  - [🔌 Endpoints da API](#-endpoints-da-api)
    - [Leitura (Queries) - `MetaQueryController`](#leitura-queries---metaquerycontroller)
    - [Escrita (Commands) - `MetaCommandController`](#escrita-commands---metacommandcontroller)
    - [Dashboards e KPIs - `KpiQueryController`](#dashboards-e-kpis---kpiquerycontroller)
  - [🔐 Segurança e Autenticação](#-segurança-e-autenticação)
    - [Regras de Acesso](#regras-de-acesso)
  - [⚖️ Regras de Negócio e Auditoria](#️-regras-de-negócio-e-auditoria)
  - [📜 Histórico de Alterações](#-histórico-de-alterações)
  - [⚙ Configuração e Execução](#-configuração-e-execução)
    - [Pré-requisitos](#pré-requisitos)
    - [Executando a API](#executando-a-api)
  - [📄 Licença](#-licença)

---

## 🛠 Stack Tecnológica

| Tecnologia          | Versão / Detalhes                          |
| ------------------- | ------------------------------------------ |
| Java                | 21 (LTS)                                   |
| Spring Boot         | 3.2.x                                      |
| Spring Security     | OAuth2 Resource Server (JWT)               |
| Spring Data JPA     | Hibernate (PostgreSQL)                     |
| Migração de Schema  | Liquibase                                  |
| Autenticação        | Keycloak (realm `tjpb-polvo`)              |
| Auditoria de Dados  | JaVers                                     |
| Mapeamento DTOs     | MapStruct                                  |
| Utilitários         | Lombok, TSID (Identificadores Ordenáveis)  |
| Build               | Maven (mvnw)                               |

---

## 🏗 Arquitetura

O projeto segue os padrões de excelência técnica do TJPB:
- **DDD (Domain-Driven Design):** Foco total no domínio e regras de negócio.
- **Arquitetura Hexagonal:** Isolamento do core de dependências externas.
- **CQRS (Command Query Responsibility Segregation):** Separação clara entre leitura e escrita.

### Estrutura de Pacotes:
```
br.jus.tjpb.polvo_api
├── application/             # Casos de Uso e Comandos (Handlers)
├── boundaries/              # Adaptadores de Entrada (Controllers, DTOs, Mappers)
├── config/                  # Configurações do Framework (Security, JPA, JaVers)
├── domain/                  # Entidades, Enums e Repositórios (Core)
├── infra/                   # Implementações Técnicas e Adaptadores de Saída
└── shared/                  # Código compartilhado entre camadas
```

---

## 🗃 Modelo de Dados

### Tabela `efa_metas`

As metas possuem campos ricos para gestão, auditoria e governança:

| Coluna | Tipo | Descrição |
| :--- | :--- | :--- |
| `id` | `BIGINT` | PK (Time-sortable ID - TSID) |
| `titulo` | `VARCHAR` | Nome da meta |
| `descricao` | `TEXT` | Detalhamento da meta |
| `status` | `VARCHAR` | Status atual (PENDENTE, EM_ANDAMENTO, etc) |
| `nivel_dificuldade`| `VARCHAR` | Nível de execução (SEM_DIFICULDADES, EM_ALERTA, CRITICA) |
| `evidencias_auditoria`| `TEXT` | Evidências para comprovação do cumprimento |
| `observacoes` | `TEXT` | Notas adicionais de governança |
| `p_maximo` | `DECIMAL` | Percentual/Pontuação máxima aplicável |
| `pontos_atingidos` | `DECIMAL` | Pontuação efetivamente alcançada |

---

## 🔌 Endpoints da API

A API roda por padrão na porta **8081**.

### Leitura (Queries) - `MetaQueryController`

| Método | Rota | Função | Acesso |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/metas` | Listagem paginada de metas | 🌐 Público |
| `GET` | `/api/metas/{id}` | Detalhes de uma meta específica | 🌐 Público |
| `GET` | `/api/metas/{id}/historico`| Linha do tempo de alterações (JaVers) | 🌐 Público |

### Escrita (Commands) - `MetaCommandController`

| Método | Rota | Função | Acesso |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/metas` | Criar nova meta | 🔒 COORDENADOR |
| `POST` | `/api/metas/batch` | Criar metas em lote | 🔒 COORDENADOR |
| `PUT` | `/api/metas/{id}` | Atualizar dados da meta | 🔒 COORDENADOR |
| `DELETE`| `/api/metas/{id}` | Remover meta do sistema | 🔒 COORDENADOR |

### Dashboards e KPIs - `KpiQueryController`

| Método | Rota | Função | Acesso |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/kpis/dashboard` | Resumo estatístico (Total, Pontos, Tração) | 🔒 Autenticado |

---

## 🔐 Segurança e Autenticação

Integração nativa com **Keycloak**. 
- **Público:** Visualização de metas e histórico.
- **Autenticado:** Acesso a dashboards.
- **Role `COORDENADOR`:** Permissão total para CRUD de metas.

---

## ⚖️ Regras de Negócio e Auditoria

### Validação de Conclusão:
Para garantir a integridade dos dados, metas marcadas como **TOTALMENTE_CUMPRIDA**, **PARCIALMENTE_CUMPRIDA** ou **NAO_CUMPRIDA** possuem validação obrigatória:
- O campo `evidencias_auditoria` deve conter no mínimo **20 caracteres**.
- Caso contrário, a operação é bloqueada com erro de negócio.

### Sanitização Matemática:
O sistema limpa automaticamente campos de estimativa quando a meta sai do estado "EM_ANDAMENTO" e calcula os pontos atingidos automaticamente em casos de 100% de cumprimento.

### Robustez na Importação:
Para facilitar a carga via planilhas legadas, o sistema adota as seguintes regras silentes:
- **Resolução de Nomes:** Caso não seja enviado um ID, o sistema busca e cria automaticamente **Eixos Temáticos** e **Setores** com base nos nomes fornecidos.
- **Deadline Padrão:** Se o prazo (`deadline`) for nulo ou inválido (ex: `-`), o sistema define automaticamente como **31/12** do ano do ciclo informado.

---

## 📜 Histórico de Alterações

Utilizamos o **JaVers** para manter um log completo de auditoria. Todas as mudanças de valores, campos alterados, quem alterou e quando alterou ficam registradas e acessíveis via endpoint de histórico, garantindo 100% de transparência na governança.

---

## ⚙ Configuração e Execução

### Pré-requisitos
- **Java 21 (LTS)**
- **PostgreSQL 15+** (Base: `db_polvo`)
- **Keycloak** (Realm: `tjpb-polvo`)

### Executando a API
```bash
# Iniciar via Maven
./mvnw spring-boot:run
```

---

## 📄 Licença

Uso exclusivo do **Tribunal de Justiça da Paraíba (TJPB)**.
Diretoria de Tecnologia da Informação.
