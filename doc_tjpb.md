# Tribunal de Justiça da Paraíba
## Documentação Padrão de Arquitetura de Software

### 1. Introdução
Esta documentação tem como objetivo padronizar a arquitetura de software adotada pelo Tribunal de Justiça da Paraíba (TJPB) no desenvolvimento de sistemas web e mobile, garantindo consistência técnica, manutenibilidade, segurança e qualidade das entregas. Ela serve como referência principal para equipes internas de desenvolvimento e para fábricas de software terceirizadas contratadas pela instituição.

A padronização proposta contempla tanto o backend quanto o frontend das aplicações, promovendo uma separação de responsabilidades, uso de boas práticas, e o emprego de tecnologias consolidadas no mercado com suporte de longo prazo (LTS). Além de servir como guia técnico, esta documentação também define critérios mínimos de qualidade e critérios de aceite para funcionalidades entregues, visando otimizar o processo de desenvolvimento, facilitar o onboarding de novos desenvolvedores e assegurar a interoperabilidade entre os sistemas do TJPB.

O conteúdo está organizado em seções temáticas e práticas, abrangendo as tecnologias utilizadas, diretrizes para o desenvolvimento backend e frontend (web e mobile), e práticas recomendadas para garantia da qualidade do código-fonte.

---

### 2. Tecnologias Utilizadas
Esta seção apresenta o conjunto de tecnologias oficialmente adotadas pelo Tribunal de Justiça da Paraíba para o desenvolvimento de sistemas. A seleção dessas ferramentas e frameworks prioriza estabilidade, suporte de longo prazo (LTS), segurança, desempenho e alinhamento com boas práticas do mercado. As tecnologias estão organizadas por camada da arquitetura da aplicação.

#### 2.1. Backend
| Tecnologia | Finalidade |
| :--- | :--- |
| Java | Linguagem principal |
| Spring Boot | Framework de desenvolvimento |
| PostgreSQL | Banco de dados relacional |

#### 2.2. Frontend
| Tecnologia | Finalidade |
| :--- | :--- |
| HTML 5 | Marcação de páginas |
| CSS3 | Estilização |
| JavaScript (ES6+) | Linguagem de programação |
| Angular | Framework SPA |
| PrimeNG | Biblioteca de componentes UI |
| SCSS (Sassy CSS) | Formato de pré-processamento para CSS |
| TypeScript | Linguagem de programação fortemente tipada |

#### 2.3. Suporte
| Tecnologia | Finalidade |
| :--- | :--- |
| ActiveMQ Artemis | Mensageria |
| Keycloak | Autenticação e autorização |
| Maven | Gerenciamento de dependências |
| Git | Versionamento de códigos |
| Swagger/SpringDoc | Documentação de APIs |
| Liquibase | Versionamento de banco de dados |
| SonarQube | Análise de qualidade de código |
| Docker | Contêineres e ambiente de execução |
| NPM (Node Package Manager) | Gerenciador de pacotes para o ambiente de execução Node.js |
| Node.js | Interpretador JavaScript |

---

### 3. Desenvolvimento Backend
O backend dos sistemas desenvolvidos pelo Tribunal de Justiça da Paraíba é construído com foco em modularidade, manutenibilidade e segurança. Para isso, adota-se uma arquitetura em camadas bem definida, uso de frameworks consolidados e boas práticas de desenvolvimento orientadas a serviços. Todos os projetos backend seguem um padrão técnico baseado na linguagem Java, utilizando o Spring Boot como principal framework para desenvolvimento web, integração, acesso a dados e segurança.

O banco de dados relacional padrão é o PostgreSQL, e para comunicação assíncrona entre sistemas utiliza-se o ActiveMQ Artemis. A autenticação e autorização são centralizadas por meio do Keycloak, via protocolos OAuth2 e OpenID Connect. Essa padronização visa garantir consistência entre os sistemas, facilitar a integração entre diferentes soluções, permitir uma curva de aprendizado mais rápida e assegurar qualidade técnica nas entregas realizadas tanto pela equipe interna quanto por fábricas de software terceirizadas.

Nas subseções a seguir, serão apresentadas diretrizes de organização de código, camadas da aplicação, boas práticas de codificação, controle de acesso, mensageria, versionamento de banco de dados, documentação de APIs, etc.

#### 3.1. DDD, Arquitetura Hexagonal e CQRS
Esta seção apresenta três abordagens arquiteturais que auxiliam no desenvolvimento de sistemas mais robustos, de fácil manutenção e alinhados ao domínio do negócio: DDD (Domain-Driven Design), Arquitetura Hexagonal e CQRS (Command Query Responsibility Segregation). Embora possam ser utilizados de forma independente, esses conceitos se complementam e têm ganhado espaço em sistemas modernos, especialmente em domínios complexos.

##### 3.1.1. Domain-Driven Design (DDD)
Domain-Driven Design (DDD) é uma abordagem de desenvolvimento de software focada no domínio do negócio. O objetivo é refletir com fidelidade as regras e processos reais no código, promovendo uma linguagem comum entre desenvolvedores e especialistas do domínio.

**Benefícios Arquiteturais:**
* Favorece a manutenibilidade e testabilidade do código.
* Cria uma linguagem ubiqua compartilhada entre time técnico e stakeholders.
* Organiza a aplicação em módulos coesos (Bounded Contexts).
* Estimula o uso de entidades ricas e serviços de domínio com lógica centralizada.

##### 3.1.2. Arquitetura Hexagonal (Ports & Adapters)
A Arquitetura Hexagonal, também conhecida como Ports and Adapters, tem como objetivo isolar o núcleo da aplicação (domínio) de detalhes externos, como banco de dados, APIs ou interfaces gráficas. De acordo com os princípios da arquitetura hexagonal, as camadas mais internas da aplicação (mais próximas do domínio) não devem conhecer nem acessar as camadas mais externas.

Normalmente a estrutura de uma aplicação com arquitetura hexagonal segue a seguinte estrutura:
* **Domínio no centro:** regras de negócio puras, sem dependência externa.
* **Portas (Ports):** interfaces que representam as formas de entrada e saída do sistema.
* **Adaptadores (Adapters):** implementações específicas (ex: REST controllers, repositórios JPA, mensageria).

**Benefícios Arquiteturais:**
* Facilita testes automatizados do domínio sem dependências externas.
* Permite substituir tecnologias (como bancos ou mensagerias) com baixo impacto.
* Promove o desacoplamento e organização em camadas com mais clareza.

##### 3.1.3. CQRS (Command Query Responsibility Segregation)
CQRS propõe a separação entre operações de leitura (Query) e escrita (Command), ao invés de utilizar o mesmo modelo para ambos. **Commands** são operações que alteram o estado da aplicação (ex: criação, atualização, exclusão). **Queries** são operações que apenas consultam dados, sem causar efeitos colaterais.

Nas aplicações do TJPB utilizamos apenas a segregação das operações nas classes da aplicação.

**Benefícios Arquiteturais:**
* Permite otimizar separadamente consultas e atualizações.
* Favorece a escalabilidade e modularização da aplicação, se necessário, especialmente em cenários com volume de leitura muito maior que o de escrita.
* Favorece a segurança da aplicação, permitindo a implementação de forma mais simples de regras de acesso diferentes para leitura e escrita.
* Favorece a contagem de pontos de função da aplicação, identificando com mais facilidade a definição de entradas externas, consultas externas e saídas externas.


#### 3.2. Estrutura de Pacotes
Todos os sistemas backend do TJPB devem seguir uma estrutura de pacotes padronizada, com o objetivo de garantir consistência entre os projetos, clareza na organização do código-fonte e facilidade de manutenção. Cada camada descrita a seguir (com exceção da config) é subdividida em subpacotes de acordo com o aggregate root:

- src
  - main
    - java
      - br.tjpb.jus.<nome_do_sistema>
        - application
        - boundaries
        - config
        - domain
        - infra
        - shared
    - resources


##### 3.2.1. Boundaries
É a camada mais externa da aplicação e abriga os pontos de entrada e saída do sistema, implementados na forma de interfaces com o mundo externo. É por meio dessa camada que se originam commands e queries a serem executados pela aplicação. Também é responsabilidade dessa camada realizar a validação preliminar dos dados de entrada (tipos, formato de dados, tamanho de campos, valores mínimos e máximos, etc.) e repassar os comandos e queries encapsulados para a camada mais interna que irá processá-los (application ou domain).

São exemplos de fronteiras implementadas nessa camada:
* Controladores REST (APIs do sistema). Seguindo o padrão CQRS as classes desta camada são divididas entre query controllers e command controllers.
* Consumidores de mensagens recebidas via filas.
* Event Listeners: ouvintes de eventos da aplicação.
* Manipuladores de arquivos e Jobs (tarefas agendadas).

##### 3.2.2. Application
É a camada intermediária entre as fronteiras da aplicação, responsável principalmente por receber commands, enriquecer os dados recebidos com os dados necessários para processamento na camada de domínio e repassar esses dados para a camada mais interna. Esta camada também tem a responsabilidade de realizar o controle de acesso básico, avaliando-se os roles do usuário que originou o comando. Na camada de aplicação é definido também o escopo transacional para o processamento dos comandos e é realizada a auditoria dos comandos, interceptados pelo CommandLogger.

##### 3.2.3. Domain
Representa o coração da aplicação, contendo entidades, repositórios (interfaces), serviços de domínio e objetos de valor. Toda regra de negócio da aplicação deve estar concentrada nesta camada que deve ser independente de frameworks externos e deve conter apenas regras de negócio puras e as interfaces ports. Por simplicidade, as entidades JPA são também as entidades DDD, anotações do ORM são permitidas e as interfaces de repositório são as próprias interfaces do JpaRepository. Recomenda-se que os repositórios sejam mantidos sob visibilidade de pacote (package-private). A comunicação entre aggregate roots deve ser realizada por meio de eventos de sistema e referenciada idealmente pelos respectivos identificadores (ids).

##### 3.2.4. Infra
Implementações técnicas de repositórios, serviços externos, clientes HTTP, envio de e-mails, acesso a banco de dados e integrações diversas. Esta camada implementa em adapters as ports definidas no domínio, mantendo a inversão de dependência.

##### 3.2.5. Config
Contém todas as classes de configuração do Spring, como beans personalizados, filtros, segurança, configuração de CORS, etc.

##### 3.2.6. Shared
Código comum reutilizável, como classes utilitárias, constantes, mapeadores genéricos, validadores e exceções customizadas.

---

### 3.3. Padronizações de código
Nesta seção serão apresentadas diversas definições de código que devem ser aplicadas nas aplicações que se propõem a aderir à arquitetura definida neste documento.

#### 3.3.1. Persistência de dados
Nesta seção serão detalhadas tecnicamente classes e padrões arquiteturais relacionados à persistência dos dados, Hibernate e Spring Data Jpa.

##### 3.3.1.1. Classe DomainEntity
A classe DomainEntity representa uma superclasse base para entidades persistentes do domínio do sistema. Ela centraliza a lógica relacionada à identificação e à comparação de entidades. A anotação MappedSuperclass indica que suas propriedades devem ser herdadas pelas entidades JPA concretas.

No construtor padrão, o id é automaticamente gerado usando a classe TsidGenerator (Time-Sortable Identifier). O método equals() utiliza a classe real do Hibernate para garantir que a comparação seja feita apenas entre instâncias da mesma entidade concreta, evitando problemas com proxies. Possui implementações que incluem auditoria de dados: DomainEntityAuditableCreate e DomainEntityAuditableUpdate.


##### 3.3.1.2. Interface DomainEntityRepository
A interface DomainRepository é uma generalização da camada de persistência do sistema. Ela estende a interface JpaRepository do Spring Data JPA, fornecendo um repositório base que pode ser reutilizado por todas as entidades que estendem DomainEntity. Seu objetivo é centralizar a configuração genérica de repositórios JPA, evitando duplicação de código e promovendo a padronização de acesso a dados em todo o domínio da aplicação.

**Benefícios Arquiteturais:**
* **Reutilização:** Evita que cada entidade tenha que declarar sua própria interface repetidamente com a mesma assinatura.
* **Consistência:** Garante que todos os repositórios utilizem Long como tipo de ID e sigam o padrão definido na arquitetura.
* **Extensibilidade:** Pode ser estendida para adicionar comportamentos comuns a todos os repositórios, como auditoria, busca paginada padronizada, soft delete, etc.

##### 3.3.1.3. TsidGenerator
A classe TsidGenerator é uma utilitária responsável por gerar identificadores únicos com base no padrão TSID (Time-Sortable Identifier). Ela encapsula a lógica de configuração e utilização da biblioteca io.hypersistence.tsid, promovendo uma estratégia de identificação distribuída, escalável e ordenada temporalmente. É possível a geração de Tsids tanto do tipo Long quanto String.

**Benefícios Arquiteturais:**
* **IDs ordenáveis por tempo:** Os TSIDs preservam a ordenação cronológica, o que facilita consultas ordenadas, indexação e análise de dados.
* **Geração distribuída:** A estratégia evita conflitos mesmo em cenários com múltiplas instâncias do sistema gerando IDs simultaneamente.
* **Desacoplamento da persistência:** Evita dependência direta de auto-incremento de banco de dados, facilitando testes, replicação e escalabilidade horizontal.
* **Reutilização e centralização:** Toda a lógica de geração de IDs está encapsulada, reduzindo erros e promovendo consistência no uso.

##### 3.3.1.4. SpecificationBuilder e QueryRepository
Interface funcional responsável pela construção de specifications com base em filtros de busca. Essa classe é genérica tendo como parâmetros: T, a entidade sobre a qual a specification atua e F o objeto de filtro que contém os critérios de busca. O método build precisa ser implementado e é ele quem retorna uma specification representando os critérios definidos pelo filtro. O uso dessas classes SpecificationBuilder pode ser feito através das interfaces que estendem de QueryRepository, que adiciona suporte à construção de queries dinâmicas com base em filtros de alto nível.

**Benefícios Arquiteturais:**
* Facilita a composição de filters com Optional, booleans e listas.
* Mantém o código de construção de specifications coeso e testável.
* Reduz a repetição de código na implementação de repositórios com queries dinâmicas.
* Promove forte tipagem e uso de objetos de filtro reutilizáveis.

#### 3.3.2. Autenticação e autorização (segurança)
A camada de segurança da aplicação é responsável por garantir a autenticação, autorização e o controle de acesso às APIs e recursos internos, com integração ao Keycloak como provedor de identidade. A segurança é baseada em OAuth2 e JWT, com suporte para diferentes fluxos de autenticação (Login com OAuth2 e JWT Resource Server).



As classes AppUser, AppUserRoles e AppUserResolver desempenham papéis centrais:
* **AppUser:** Representa o usuário autenticado no contexto da aplicação, encapsulando informações como o identificador do usuário, nome e as roles (permissões) atribuídas.
* **AppUserRoles:** Enumeração que define de forma centralizada os diferentes papéis (roles) reconhecidos pelo sistema, como ADMIN, USER, entre outros.
* **AppUserResolver:** Componente utilitário responsável por recuperar, a partir do contexto de segurança atual, o objeto AppUser correspondente ao usuário autenticado, permitindo que camadas de negócio tenham fácil acesso aos dados do usuário logado sem precisar interagir diretamente com a infraestrutura de segurança.

#### 3.3.3. Tratamento de erros
A classe AppControllerAdvice é o mecanismo central de tratamento de erros (Exception Handling) da aplicação. Ela intercepta exceções lançadas durante o ciclo de vida de uma requisição REST e converte essas exceções em respostas HTTP padronizadas e amigáveis ao consumidor da API. Essa abordagem segue o padrão do Spring Boot usando @ControllerAdvice e ResponseEntityExceptionHandler.

**Principais responsabilidades:**
* Capturar exceções comuns durante a execução de endpoints REST.
* Retornar respostas JSON com informações úteis de erro.
* Garantir consistência no formato de resposta de erro em toda a aplicação (record ErrorResponse).

#### 3.3.4. Cache
Aplicações do TJPB utilizam o mecanismo de cache fornecido pelo Spring Boot Starter Cache, sem customizações adicionais de configuração de backend. O cache é usado para armazenar dados frequentemente acessados, melhorando a performance e reduzindo o tempo de resposta de operações repetitivas.

**Boas práticas:**
* Foque em dados caros de consultar/calcular, acessados com alta frequência e que não mudam constantemente.
* Defina prazos de validade (TTL) para que os dados não fiquem obsoletos.
* Evite colocar objetos muito grandes (ex: listas gigantes, arquivos binários) no cache.
* Quando atualizar um dado, lembre-se de invalidar/evictar o cache relacionado.

#### 3.3.5. Auditoria de comandos (pacote config.command)
A auditoria de comandos é um mecanismo transversal implementado para registrar a execução de operações sensíveis de negócio, garantindo rastreabilidade, transparência e apoio à governança da aplicação. Cada operação de negócio que altera o estado do sistema (ex: criação, edição ou exclusão de dados) é encapsulada em um comando.

Esses comandos estendem a classe **AbstractCommand**, que captura metadados contextuais, como o usuário autenticado e o instante de criação do comando. Um aspecto (**CommandsLogger**) intercepta automaticamente a execução desses comandos e registra os dados relevantes em log estruturado antes da execução efetiva da lógica de negócio. Esses dados são utilizado para rastrear quem executou o quê e quando.


##### 3.3.6. Serialização de objetos em JSON
A serialização e desserialização de objetos em JSON utiliza a principal biblioteca disponível no mercado para esta finalidade (Jackson) e é centralizada na aplicação por meio de uma instância customizada de `ObjectMapper`, a classe `AppObjectMapper`. Esta configuração garante padronização, interoperabilidade e suporte a tipos específicos utilizados no domínio da aplicação.

**Benefícios Arquiteturais:**
* **Padronização:** Todos os subsistemas compartilham a mesma configuração JSON.
* **Extensibilidade:** Novos módulos ou tipos customizados podem ser adicionados centralizadamente.
* **Manutenção facilitada:** Mudanças no formato de serialização (como datas) exigem alteração em apenas um ponto.
* **Interoperabilidade:** O uso de formatos padrão (ex: ISO para datas) torna a API mais estável para integrações externas.

##### 3.3.7. Observabilidade: APM e Logging
A observabilidade é fundamental para garantir estabilidade e desempenho. É adotada uma abordagem padronizada baseada no **Elastic Stack**, integrando monitoramento de performance (APM) e centralização de logs.



**Benefícios Arquiteturais:**
* Rastreabilidade completa de requisições (*tracing*).
* Visualização de métricas e tempo de resposta por *endpoint*.
* Detecção proativa de erros e análise de logs estruturados.
* Correlação entre logs, exceções e transações APM.

###### 3.3.7.1. Application Performance Monitoring (APM)
Todas as aplicações backend devem integrar o agente APM da Elasticsearch para rastrear requisições e identificar gargalos em tempo real. A inicialização é realizada programaticamente via `ElasticApmAttacher`.

###### 3.3.7.2. Logging
Os logs são enviados para o Elasticsearch para análise via Kibana.
* **Formato:** Devem seguir o formato JSON para facilitar a indexação.
* **Nível:** O padrão para produção deve ser INFO.
* **Segurança:** Logs sensíveis (senhas e tokens) jamais devem ser registrados.

##### 3.3.8. OpenAPI e Swagger
As APIs REST são documentadas utilizando a especificação **OpenAPI 3.0** via biblioteca SpringDoc.
* **Swagger UI:** Acessível em `http://<host>:<port>/<path>/swagger-ui.html`.
* **OpenAPI JSON:** Disponível em `http://<host>:<port>/<path>/v3/api-docs`.

##### 3.3.9. Jobs, Processamento Assíncrono e Tarefas Agendadas
O **JobRunr** é a solução padrão para agendamento e execução de tarefas em *background*.
* **Uso:** Tarefas periódicas, geração de relatórios, envios de notificações e execuções pesadas.
* **Boas Práticas:** Nomear jobs de forma única, delegar a lógica para casos de uso no domínio e garantir que sejam idempotentes.

##### 3.3.10. Storage e Armazenamento de Arquivos
O armazenamento é centralizado e compatível com o protocolo **S3**, utilizando **MinIO** em ambientes controlados.
* **Abstração:** O acesso é mediado pela interface `StorageService`, desacoplando a implementação concreta do domínio.
* **Funcionalidades:** Criação de *buckets*, geração de URLs pré-assinadas (*presigned URLs*) para transferência segura e gestão de binários.
* **Segurança:** O conteúdo nunca deve ser armazenado no banco de dados; apenas metadados e referências são persistidos.

##### 3.3.11. Mensageria
A comunicação assíncrona utiliza o **ActiveMQ** como *broker* e o padrão **JMS**.
* **Configuração:** Suporta conversão automática de mensagens JSON.
* **Consumo:** Utiliza a anotação `@JmsListener`, delegando a lógica para o domínio.
* **Envio:** Realizado via interface `Notifier`, permitindo envios individuais ou em lote.

---

### 4. Desenvolvimento Frontend - WEB
O frontend é construído com foco em modularidade e manutenibilidade, utilizando **TypeScript** e **Angular** como framework principal. Segue as recomendações do *Angular Style Guide*.

#### 4.1. Estrutura de Pacotes
A estrutura padronizada para garantir consistência entre projetos é: 



* **app/modules:** Subdividido por funcionalidades (ex: *home*), contendo componentes e páginas próprios.
* **app/core:** Módulos de carga única como autenticação, *guards*, *interceptors* e serviços globais.
* **app/shared:** Componentes, diretivas e *pipes* reutilizáveis por diversos módulos.
* **assets/environments/scss:** Recursos estáticos, configurações de ambiente e estilos globais.

---

### 5. Desenvolvimento Frontend - Mobile
*(Seção constante no sumário do documento original, mas sem conteúdo detalhado nas páginas fornecidas)*.

---

### 6. Qualidade de Código
*(Seção constante no sumário do documento original, mas sem conteúdo detalhado nas páginas fornecidas)*.