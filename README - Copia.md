# 🐙 Polvo API — Eficiência em Ação

**Sistema de Gestão de Metas** do TJPB (Tribunal de Justiça da Paraíba).

API RESTful desenvolvida com **Spring Boot 4**, protegida com **OAuth2/Keycloak** e persistência em **PostgreSQL** com versionamento de schema via **Liquibase**.

---

## 📋 Índice

- [Stack Tecnológica](#-stack-tecnológica)
- [Arquitetura](#-arquitetura)
- [Modelo de Dados](#-modelo-de-dados)
- [Endpoints da API](#-endpoints-da-api)
- [Segurança e Autenticação](#-segurança-e-autenticação)
- [Configuração e Execução](#-configuração-e-execução)
- [Testando a API](#-testando-a-api)

---

## 🛠 Stack Tecnológica

| Tecnologia          | Versão / Detalhes                          |
| ------------------- | ------------------------------------------ |
| Java                | 21                                         |
| Spring Boot         | 4.0.3                                      |
| Spring Security     | OAuth2 Resource Server                     |
| Spring Data JPA     | Hibernate (modo `validate`)                |
| Banco de Dados      | PostgreSQL                                 |
| Migração de Schema  | Liquibase                                  |
| Autenticação        | Keycloak (realm `tjpb-polvo`)              |
| Utilitários         | Lombok                                     |
| Build               | Maven                                      |

---

## 🏗 Arquitetura

```
br.jus.tjpb.polvo_api
├── controller/
│   ├── MetaController.java      # CRUD de Metas
│   └── TesteController.java     # Endpoints de teste de segurança
├── model/
│   └── Meta.java                # Entidade JPA (tabela: metas)
├── repository/
│   └── MetaRepository.java      # Spring Data JPA Repository
└── security/
    ├── SecurityConfig.java      # Configuração do filtro de segurança
    └── JwtAuthConverter.java    # Extração de roles do JWT Keycloak
```

---

## 🗃 Modelo de Dados

### Tabela `metas`

| Coluna         | Tipo              | Restrições                       |
| -------------- | ----------------- | -------------------------------- |
| `id`           | `BIGSERIAL`       | **PK**, auto-incremento         |
| `titulo`       | `VARCHAR(255)`    | `NOT NULL`                       |
| `descricao`    | `TEXT`            | Opcional                         |
| `concluida`    | `BOOLEAN`         | Default `FALSE`                  |
| `data_criacao` | `TIMESTAMP`       | Default `CURRENT_TIMESTAMP`      |

O schema é gerenciado pelo **Liquibase** via `db/changelog/db.changelog-master.sql`.

---

## 🔌 Endpoints da API

A API roda por padrão na porta **8081** (`http://localhost:8081`).

### Metas (`/api/metas`)

| Método | Rota              | Descrição                | Acesso                   |
| ------ | ----------------- | ------------------------ | ------------------------ |
| `GET`  | `/api/metas`      | Listar todas as metas    | 🌐 **Público**          |
| `POST` | `/api/metas`      | Criar uma nova meta      | 🔒 Role `COORDENADOR`   |
| `PUT`  | `/api/metas/{id}` | Atualizar uma meta por ID| 🔒 Role `COORDENADOR`   |

#### `GET /api/metas` — Listar Metas

Retorna a lista completa de metas cadastradas.

**Resposta** `200 OK`:
```json
[
  {
    "id": 1,
    "titulo": "Reduzir tempo de tramitação",
    "descricao": "Diminuir o tempo médio de tramitação processual em 15%",
    "concluida": false,
    "dataCriacao": "2026-02-25T10:30:00"
  }
]
```

#### `POST /api/metas` — Criar Meta

Cria uma nova meta. Requer token JWT com role `COORDENADOR`.

**Headers**: `Authorization: Bearer <token>`, `Content-Type: application/json`

**Body**:
```json
{
  "titulo": "Reduzir tempo de tramitação",
  "descricao": "Diminuir o tempo médio de tramitação processual em 15%"
}
```

**Resposta** `200 OK`: retorna o objeto criado com `id` e `dataCriacao` preenchidos.

#### `PUT /api/metas/{id}` — Atualizar Meta

Atualiza os campos de uma meta existente. Requer token JWT com role `COORDENADOR`.

**Headers**: `Authorization: Bearer <token>`, `Content-Type: application/json`

**Body**:
```json
{
  "titulo": "Meta atualizada",
  "descricao": "Nova descrição",
  "concluida": true
}
```

**Respostas**:
- `200 OK`: meta atualizada com sucesso.
- `404 Not Found`: meta com o ID informado não existe.

---

### Teste (`/api/public`, `/api/gestao`)

| Método | Rota                 | Descrição                         | Acesso                 |
| ------ | -------------------- | --------------------------------- | ---------------------- |
| `GET`  | `/api/public/teste`  | Endpoint de teste público         | 🌐 **Público**        |
| `GET`  | `/api/gestao/teste`  | Endpoint de teste de gestão       | 🔒 Role `COORDENADOR` |

Esses endpoints retornam mensagens de texto simples para validar se a configuração de segurança está funcionando corretamente.

---

## 🔐 Segurança e Autenticação

A API utiliza **Spring Security** como **OAuth2 Resource Server**, validando tokens JWT emitidos pelo **Keycloak**.

### Regras de Acesso

```
/api/public/**       →  Acesso livre (permitAll)
GET /api/metas/**    →  Acesso livre (permitAll)
POST /api/metas/**   →  Role COORDENADOR
PUT /api/metas/**    →  Role COORDENADOR
/api/gestao/**       →  Role COORDENADOR
Demais rotas         →  Autenticado (qualquer usuário válido)
```

### Como funciona a autenticação

1. O cliente obtém um token JWT do Keycloak (realm `tjpb-polvo`, client `polvo-app`).
2. O token é enviado no header `Authorization: Bearer <token>`.
3. A API valida o token usando a chave pública do Keycloak (`jwk-set-uri`).
4. O `JwtAuthConverter` extrai as roles do claim `resource_access.polvo-app.roles` do JWT.
5. As roles são mapeadas para authorities do Spring Security com prefixo `ROLE_` (ex: `ROLE_COORDENADOR`).

### Configuração do Keycloak

| Parâmetro     | Valor                                                  |
| ------------- | ------------------------------------------------------ |
| Realm         | `tjpb-polvo`                                           |
| Client ID     | `polvo-app`                                            |
| Issuer URI    | `http://localhost:8080/realms/tjpb-polvo`              |
| JWK Set URI   | `http://localhost:8080/realms/tjpb-polvo/protocol/openid-connect/certs` |

---

## ⚙ Configuração e Execução

### Pré-requisitos

- **Java 21**
- **PostgreSQL** rodando em `localhost:5432` com banco `db_polvo`
- **Keycloak** rodando em `localhost:8080` com realm `tjpb-polvo` configurado

### Banco de Dados

O `application.yml` já vem configurado com:

```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/db_polvo
  username: postgres
  password: postgres
```

O Liquibase cria a tabela `metas` automaticamente na primeira execução.

### Executando a API

```bash
# Via Maven Wrapper
./mvnw spring-boot:run

# Ou via script batch (Windows)
start_project.bat
```

A API ficará disponível em `http://localhost:8081`.

---

## 🧪 Testando a API

### Script automatizado

O projeto inclui o script `teste_api.bat` que executa uma bateria de 7 testes:

| #   | Teste                                 | Esperado         |
| --- | ------------------------------------- | ---------------- |
| 1   | `GET /api/public/teste`               | `200 OK`         |
| 2   | `GET /api/gestao/teste` sem token     | `401 Unauthorized` |
| 3   | `GET /api/metas` (público)            | `200 OK`         |
| 4   | `POST /api/metas` sem token           | `401 Unauthorized` |
| 5   | Obter token JWT do Keycloak           | Token válido     |
| 6   | `POST /api/metas` com token           | `200 OK`         |
| 7   | `PUT /api/metas/1` com token          | `200 OK`         |

```bash
# Executar os testes (Windows)
teste_api.bat
```

### Exemplo manual com curl

```bash
# 1. Obter token do Keycloak
TOKEN=$(curl -s -X POST "http://localhost:8080/realms/tjpb-polvo/protocol/openid-connect/token" \
  -d "client_id=polvo-app" \
  -d "username=joao123" \
  -d "password=joao123" \
  -d "grant_type=password" | jq -r '.access_token')

# 2. Listar metas (público)
curl http://localhost:8081/api/metas

# 3. Criar meta (requer COORDENADOR)
curl -X POST http://localhost:8081/api/metas \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"titulo": "Nova meta", "descricao": "Descrição da meta"}'

# 4. Atualizar meta (requer COORDENADOR)
curl -X PUT http://localhost:8081/api/metas/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"titulo": "Meta atualizada", "descricao": "Descrição revisada", "concluida": true}'
```

---

## 📄 Licença

Projeto interno do **Tribunal de Justiça da Paraíba (TJPB)**.
