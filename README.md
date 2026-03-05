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
    - [Tabela `metas`](#tabela-metas)
  - [🔌 Endpoints da API](#-endpoints-da-api)
    - [Leitura (Queries) - `MetaQueryController`](#leitura-queries---metaquerycontroller)
    - [Escrita (Commands) - `MetaCommandController`](#escrita-commands---metacommandcontroller)
  - [🔐 Segurança e Autenticação](#-segurança-e-autenticação)
    - [Regras de Acesso](#regras-de-acesso)
  - [📜 Auditoria e Histórico](#-auditoria-e-histórico)
  - [⚙ Configuração e Execução](#-configuração-e-execução)
    - [Pré-requisitos](#pré-requisitos)
    - [Executando a API](#executando-a-api)
  - [📄 Licença](#-licença)

---

## 🛠 Stack Tecnológica

| Tecnologia          | Versão / Detalhes                          |
| ------------------- | ------------------------------------------ |
| Java                | 21                                         |
| Spring Boot         | 3.2.x                                      |
| Spring Security     | OAuth2 Resource Server                     |
| Spring Data JPA     | Hibernate (modo `validate`)                |
| Banco de Dados      | PostgreSQL                                 |
| Migração de Schema  | Liquibase                                  |
| Autenticação        | Keycloak (realm `tjpb-polvo`)              |
| Auditoria de Dados  | JaVers                                     |
| Utilitários         | Lombok, TSID (Identificadores Únicos)      |
| Build               | Maven                                      |

---

## 🏗 Arquitetura

O projeto foi reestruturado para seguir o padrão **DDD** (Domain-Driven Design) combinado com **Arquitetura Hexagonal (Ports & Adapters)** e **CQRS** (Command Query Responsibility Segregation).

```
br.jus.tjpb.polvo_api
├── application/             # Casos de uso e comandos (Handlers)
│   └── meta/command/        # Handlers de Create, Update, Delete de Metas
├── boundaries/              # Adaptadores de entrada (Controllers REST)
│   └── api/                 # Endpoints segregados (MetaQueryController e MetaCommandController)
├── config/                  # Configurações globais (Segurança, JPA, JaVers, JSON, Tratamento de Erros)
├── domain/                  # Entidades de Domínio, Repositórios e Regras de Negócio (ex: Meta, DomainEntity)
├── infra/                   # Implementações de infraestrutura e integrações
└── shared/                  # Classes utilitárias e DTOs (Data Transfer Objects) compartilhados
```

---

## 🗃 Modelo de Dados

### Tabela `metas`

A entidade de domínio agora herda de classes bases (`DomainEntityAuditableCreate`, `DomainEntityAuditableUpdate`) que garantem campos padronizados:

| Coluna                  | Tipo              | Restrições                       |
| ----------------------- | ----------------- | -------------------------------- |
| `id`                    | `BIGINT`          | **PK**, gerado via **TSID**     |
| `titulo`                | `VARCHAR(255)`    | `NOT NULL`                       |
| `descricao`             | `TEXT`            | Opcional                         |
| `concluida`             | `BOOLEAN`         | Default `FALSE`                  |
| `data_criacao`          | `TIMESTAMP`       | Default `CURRENT_TIMESTAMP`      |
| `usuario_criacao_id`    | `VARCHAR(255)`    | Identificador do criador         |
| `usuario_criacao_nome`  | `VARCHAR(255)`    | Nome do criador                  |
| `data_atualizacao`      | `TIMESTAMP`       | Data da última atualização       |
| `usuario_atualizacao_id`| `VARCHAR(255)`    | Identificador do modificador     |
| `usuario_atualizacao_nome`| `VARCHAR(255)`  | Nome do modificador              |

O schema é gerenciado pelo **Liquibase** via `db/changelog/db.changelog-master.sql`.

---

## 🔌 Endpoints da API

A API foi segregada aplicando o padrão CQRS, separando operações de leitura (*Query*) e escrita (*Command*).
A API roda por padrão na porta **8081** (`http://localhost:8081`).

### Leitura (Queries) - `MetaQueryController`

| Método | Rota                          | Descrição                          | Acesso                   |
| ------ | ----------------------------- | ---------------------------------- | ------------------------ |
| `GET`  | `/api/metas`                  | Listar todas as metas              | 🌐 **Público**          |
| `GET`  | `/api/metas/{id}`             | Buscar meta por TSID                | 🌐 **Público**          |
| `GET`  | `/api/metas/{id}/historico`   | Visualizar o histórico de mudanças| 🌐 **Público**          |

### Escrita (Commands) - `MetaCommandController`

| Método | Rota                          | Descrição                          | Acesso                   |
| ------ | ----------------------------- | ---------------------------------- | ------------------------ |
| `POST` | `/api/metas`                  | Criar uma nova meta                | 🔒 Role `COORDENADOR`   |
| `PUT`  | `/api/metas/{id}`             | Atualizar uma meta por TSID         | 🔒 Role `COORDENADOR`   |
| `DELETE`| `/api/metas/{id}`            | Excluir uma meta                   | 🔒 Role `COORDENADOR`   |

*(Payloads e respostas seguem o mesmo padrão definido anteriormente, com o bônus de detalhamentos mais complexos nos casos de erro tratáveis pelo AppControllerAdvice).*

---

## 🔐 Segurança e Autenticação

A API continua utilizando **Spring Security** (OAuth2 Resource Server) integrado ao **Keycloak**. A classe `AppUserResolver` permite em toda aplicação resgatar com facilidade os detalhes do usuário baseando-se no JWT autenticado, garantindo rastreabilidade do autor das operações de *Command*.

### Regras de Acesso

```
/api/public/**                     →  Acesso livre (permitAll)
GET /api/metas/**                  →  Acesso livre (permitAll)
POST, PUT, DELETE /api/metas/**    →  Role COORDENADOR
Demais rotas                       →  Autenticado (qualquer usuário válido)
```

---

## 📜 Auditoria e Histórico

A API faz uso intenso do **JaVers** combinado customizações de arquitetura (via `CommandsLogger` interceptors). Sempre que uma meta é criada, editada ou excluída, o sistema audita quem foi o responsável (baseado no token do Keycloak) e quais propriedades foram alteradas. O endpoint `GET /api/metas/{id}/historico` serve como consulta a esta timeline de alterações.

---

## ⚙ Configuração e Execução

### Pré-requisitos

- **Java 21**
- **PostgreSQL** rodando em `localhost:5432` com banco `db_polvo`
- **Keycloak** rodando em `localhost:8080` com realm `tjpb-polvo` configurado

### Executando a API

```bash
# Via Maven Wrapper
./mvnw spring-boot:run

# Ou via script batch (Windows)
start_project.bat
```

A API ficará disponível em `http://localhost:8081`.

---

## 📄 Licença

Projeto interno do **Tribunal de Justiça da Paraíba (TJPB)**.
