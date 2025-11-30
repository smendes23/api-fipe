# FIPE Vehicle Management System

## Visão Geral

Este projeto é um sistema distribuído para gerenciamento de dados de veículos da FIPE (Fundação Instituto de Pesquisas Econômicas). Ele é composto por duas APIs Spring Boot 3 com WebFlux e Java 21, que se comunicam de forma assíncrona via Apache Kafka. A arquitetura segue princípios de Clean Architecture, Domain-Driven Design (DDD), SOLID e outras boas práticas de desenvolvimento.

O sistema permite:
- Realizar uma carga inicial de dados de marcas de veículos da API da FIPE.
- Processar as marcas, buscar os modelos de veículos correspondentes e persistir no banco de dados.
- Consultar as marcas e veículos armazenados.
- Atualizar informações dos veículos.
- Proteger os endpoints com autenticação baseada em JWT.
- Utilizar cache com Redis para otimizar as consultas.

## Arquitetura

O sistema é dividido em dois serviços principais:

### (gateway)
- **Responsabilidades**: Atua como o gateway de entrada para os clientes. 
- É responsável por orquestrar as requisições ao serviços que atuaram como processadores disparando  requisições para carga de dados, consultar informações, atualizar dados de veículos, gerenciar o cache e a autenticação.
- **Tecnologias**: Spring WebFlux, Spring Data R2DBC, Reactor Kafka, Spring Security, Redis, PostgreSQL.

### API-1 (processor-brand)
- **Responsabilidades**: É o responsável pela carga inicial dos dados e também os fluxos de consulta.
- **Tecnologias**: Spring WebFlux, Spring Data R2DBC, Reactor Kafka, PostgreSQL.
- 
### API-2 (processor-vehicles)
- **Responsabilidades**: Consome as mensagens de marcas enviadas pela API-1 via Kafka. Para cada marca, busca os modelos de veículos na API da FIPE e os persiste no banco de dados PostgreSQL.
- **Tecnologias**: Spring WebFlux, Spring Data R2DBC, Reactor Kafka, PostgreSQL.

### Fluxo de Dados (Carga Inicial)

1.  O cliente envia uma requisição `POST /api/v1/vehicles/load` para a **API-1**.
2.  A **API-1** busca as marcas de veículos na API externa da FIPE.
3.  Para cada marca encontrada, a **API-1** a salva no PostgreSQL e publica uma mensagem no tópico Kafka `fipe.brands`.
4.  A **API-2** consome as mensagens do tópico `fipe.brands`.
5.  Para cada marca recebida, a **API-2** busca os modelos de veículos na API da FIPE.
6.  A **API-2** salva os dados completos dos veículos (código, marca, modelo) no banco de dados PostgreSQL.

## Tecnologias Utilizadas

| Categoria       | Tecnologia                               |
| --------------- | ---------------------------------------- |
| **Linguagem**   | Java 21                                  |
| **Framework**   | Spring Boot 3.2.1 (com WebFlux)          |
| **Banco de Dados**| PostgreSQL 15                            |
| **Cache**       | Redis 7                                  |
| **Mensageria**  | Apache Kafka 3.6                         |
| **Reatividade** | Project Reactor                          |
| **Segurança**   | Spring Security (JWT)                    |
| **Documentação**| SpringDoc OpenAPI 3 (Swagger)            |
| **Build**       | Maven 3.9                                |
| **Container**   | Docker & Docker Compose                  |
| **Testes**      | JUnit 5, Mockito, Testcontainers         |

## Pré-requisitos

- [Docker](https://www.docker.com/get-started)
- [Docker Compose](https://docs.docker.com/compose/install/)
- Git
- Conexão com a internet para baixar as imagens Docker e dependências Maven.

## Como Executar

Siga os passos abaixo para clonar, construir e executar a aplicação.

### 1. Clone o Repositório

```bash
git clone <URL_DO_REPOSITORIO>
cd fipe-vehicle-system
```

### 2. Construa e Execute com Docker Compose

O `docker-compose.yml` na raiz do projeto orquestra todos os serviços necessários (PostgreSQL, Redis, Zookeeper, Kafka e as duas APIs). Os Dockerfiles das APIs são multi-stage, compilando o código e criando uma imagem final otimizada.

Para iniciar todo o ambiente, execute o comando:

```bash
docker-compose up --build
```

O argumento `--build` força a reconstrução das imagens das APIs, garantindo que quaisquer alterações no código sejam aplicadas. Na primeira execução, o download das imagens pode levar alguns minutos.

Após a inicialização, os seguintes serviços estarão disponíveis:

- **API-1 Gateway**: `http://localhost:8080`
- **API-2 Processor**: `http://localhost:8081`
- **PostgreSQL**: `localhost:5432`
- **Redis**: `localhost:6379`
- **Kafka**: `localhost:9092`
- **Swagger UI (API-1)**: `http://localhost:8080/swagger-ui.html`

### 3. Verificando o Status dos Serviços

Você pode verificar o status dos containers com o comando:

```bash
docker-compose ps
```

Para visualizar os logs de um serviço específico (por exemplo, a `gateway`):

```bash
docker-compose logs -f gateway
```

## Como Testar

Os testes unitários e de integração foram desenvolvidos seguindo a prática de TDD. Para executar os testes, você pode usar o Maven. Não é necessário ter o ambiente Docker em execução, pois os testes de integração utilizam **Testcontainers** para provisionar instâncias temporárias de PostgreSQL e Kafka.

### Executando os Testes da API-1

Navegue até o diretório da API-1 e execute o comando Maven:

```bash
cd api-1-gateway
mvn test
```

### Executando os Testes da API-2

Navegue até o diretório da API-2 e execute o comando Maven:

```bash
cd ../api-2-processor
mvn test
```

## Documentação dos Endpoints (API-1)

A seguir, uma descrição detalhada dos endpoints disponíveis na `api-1-gateway`.

**URL Base**: `http://localhost:8080`

### Autenticação

#### 1. Login de Usuário

Autentica um usuário e retorna um token JWT para ser usado nas requisições subsequentes.

- **Endpoint**: `POST /api/v1/auth/login`
- **Descrição**: Realiza o login com `username` e `password`.
- **Credenciais Padrão**:
  - Admin: `username: admin`, `password: admin123`
  - User: `username: user`, `password: user123`

**Exemplo de Requisição (cURL):**

```bash
curl -X POST "http://localhost:8080/api/v1/auth/login" \
-H "Content-Type: application/json" \
-d '{
  "username": "admin",
  "password": "admin123"
}'
```

**Exemplo de Resposta (200 OK):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiQURNSU4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTcwNTg2NzU4NSwiZXhwIjoxNzA1OTUzOTg1fQ.abcdef...",
  "type": "Bearer",
  "expiresIn": 86400000
}
```

### Gerenciamento de Veículos

**Header de Autenticação Obrigatório**: `Authorization: Bearer <SEU_TOKEN_JWT>`

#### 2. Carga Inicial de Dados

Dispara o processo de busca de marcas na API da FIPE e publicação no Kafka.

- **Endpoint**: `POST /api/v1/vehicles/load`

**Exemplo de Requisição (cURL):**

```bash
curl -X POST "http://localhost:8080/api/v1/vehicles/load" \
-H "Authorization: Bearer <SEU_TOKEN_JWT>"
```

**Exemplo de Resposta (202 ACCEPTED):**

```json
"Data loading initiated. Processed 59 brands."
```

#### 3. Buscar Todas as Marcas

Retorna a lista de todas as marcas de veículos armazenadas no banco de dados.

- **Endpoint**: `GET /api/v1/brands`

**Exemplo de Requisição (cURL):**

```bash
curl -X GET "http://localhost:8080/api/v1/brands" \
-H "Authorization: Bearer <SEU_TOKEN_JWT>"
```

**Exemplo de Resposta (200 OK):**

```json
[
  {
    "code": "1",
    "name": "Acura"
  },
  {
    "code": "2",
    "name": "Agrale"
  }
]
```

#### 4. Buscar Veículos por Marca

Retorna a lista de veículos de uma marca específica.

- **Endpoint**: `GET /api/v1/vehicles?brandCode={brand}`

**Exemplo de Requisição (cURL):**

```bash
curl -X GET "http://localhost:8080/api/v1/vehicles?brand=BMW" \
-H "Authorization: Bearer <SEU_TOKEN_JWT>"
```

**Exemplo de Resposta (200 OK):**

```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    "code": "004001-3",
    "brandCode": "21",
    "model": "147 C/CL",
    "observations": null
  },
  {
    "id": "b2c3d4e5-f6a7-8901-2345-67890abcdef1",
    "code": "004168-0",
    "brandCode": "21",
    "model": "500 ABARTH 1.4 16V TB 167cv",
    "observations": null
  }
]
```

#### 5. Atualizar um Veículo

Permite atualizar o modelo e as observações de um veículo existente.

- **Endpoint**: `PUT /api/v1/vehicles/{id}`

**Exemplo de Requisição (cURL):**

```bash
curl -X PUT "http://localhost:8080/api/v1/vehicles/a1b2c3d4-e5f6-7890-1234-567890abcdef" \
-H "Authorization: Bearer <SEU_TOKEN_JWT>" \
-H "Content-Type: application/json" \
-d '{
  "model": "147 C/CL Special Edition",
  "observations": "Veículo de colecionador."
}'
```

**Exemplo de Resposta (200 OK):**

```json
{
  "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "code": "004001-3",
  "brandCode": "21",
  "model": "147 C/CL Special Edition",
  "observations": "Veículo de colecionador."
}
```

### Tratamento de Erros

Em caso de erro (ex: validação, não encontrado, não autorizado), a API retornará uma resposta com o status HTTP apropriado e um corpo JSON detalhando o erro.

**Exemplo de Resposta de Erro (400 Bad Request):**

```json
{
  "timestamp": "2025-11-19T14:30:00.123456",
  "status": 400,
  "error": "Bad Request",
  "message": "Vehicle not found with id: a1b2c3d4-e5f6-7890-1234-000000000000"
}
```


## Boas Práticas Implementadas

Este projeto foi desenvolvido seguindo as melhores práticas de engenharia de software. A seguir, detalhamos como cada princípio e padrão foi aplicado.

### SOLID

Os princípios SOLID foram aplicados em toda a arquitetura do sistema, garantindo código limpo, manutenível e extensível.

**Single Responsibility Principle (SRP)**: Cada classe tem uma única responsabilidade bem definida. Por exemplo, os casos de uso (`LoadFipeDataUseCase`, `UpdateVehicleUseCase`) focam apenas em uma operação de negócio específica. Os repositórios (`BrandRepository`, `VehicleRepository`) lidam exclusivamente com persistência de dados.

**Open/Closed Principle (OCP)**: O sistema é extensível sem modificar o código existente. A utilização de interfaces (ports) permite adicionar novos adaptadores (por exemplo, trocar Redis por Memcached) sem alterar a lógica de negócio. Os casos de uso dependem de abstrações, não de implementações concretas.

**Liskov Substitution Principle (LSP)**: As implementações podem substituir suas abstrações sem quebrar o comportamento esperado. Por exemplo, qualquer implementação de `FipeServicePort` pode ser usada no lugar da outra, mantendo o contrato definido pela interface.

**Interface Segregation Principle (ISP)**: As interfaces são específicas e coesas. `CacheServicePort` define apenas operações de cache, sem misturar responsabilidades. `MessagePublisherPort` foca apenas em publicação de mensagens.

**Dependency Inversion Principle (DIP)**: As dependências apontam para abstrações, não para implementações concretas. Os casos de uso dependem de `FipeServicePort` e `MessagePublisherPort`, não das implementações específicas (`FipeApiAdapter`, `KafkaMessagePublisher`). Isso facilita testes e substituição de componentes.

### Domain-Driven Design (DDD)

A arquitetura segue os princípios de DDD, organizando o código em torno do domínio de negócio.

**Entidades e Value Objects**: As entidades (`Brand`, `Vehicle`, `User`) representam conceitos do domínio com identidade própria. Elas encapsulam comportamento e validação de negócio através de métodos como `isValid()` e `update()`.

**Agregados**: `Vehicle` atua como um agregado raiz, mantendo consistência e integridade dos dados relacionados a veículos. As operações que modificam o veículo passam pela entidade, garantindo que as regras de negócio sejam aplicadas.

**Repositórios**: Os repositórios (`BrandRepository`, `VehicleRepository`) fornecem uma abstração sobre a persistência de dados, isolando o domínio dos detalhes de infraestrutura. Eles seguem o padrão Repository do DDD.

**Casos de Uso (Application Services)**: Os casos de uso (`LoadFipeDataUseCase`, `ProcessBrandUseCase`) orquestram as operações de negócio, coordenando entidades, repositórios e serviços externos. Eles representam as operações que o sistema pode realizar.

**Camadas Bem Definidas**: O projeto segue uma estrutura em camadas clara: Domain (entidades, repositórios), Application (casos de uso, ports), Infrastructure (adaptadores, configurações) e Presentation (controllers, DTOs). Cada camada tem responsabilidades específicas e depende apenas das camadas internas.

### Clean Architecture

A arquitetura hexagonal (Ports and Adapters) foi aplicada para isolar a lógica de negócio das preocupações de infraestrutura.

**Ports**: Interfaces como `FipeServicePort`, `MessagePublisherPort` e `CacheServicePort` definem contratos que a lógica de negócio espera. Elas são definidas na camada de aplicação, não na infraestrutura.

**Adapters**: Classes como `FipeApiAdapter`, `KafkaMessagePublisher` e `RedisCacheService` implementam os ports, adaptando tecnologias externas ao domínio. Eles traduzem entre o mundo externo e o domínio interno.

**Independência de Frameworks**: A lógica de negócio não depende de frameworks específicos. Os casos de uso podem ser testados sem Spring, Kafka ou Redis. Isso facilita a manutenção e evolução do sistema.

**Testabilidade**: A separação clara entre domínio, aplicação e infraestrutura torna o código altamente testável. Os testes unitários usam mocks para isolar a lógica de negócio, enquanto os testes de integração verificam a interação entre componentes.

### Design Patterns

Diversos padrões de projeto foram aplicados para resolver problemas comuns de forma elegante.

**Factory Pattern**: Métodos de fábrica como `Brand.create()` e `Vehicle.create()` encapsulam a lógica de criação de objetos, garantindo que sejam criados em um estado válido.

**Repository Pattern**: Os repositórios fornecem uma interface de coleção para acesso a dados, abstraindo os detalhes de persistência. Isso facilita testes e mudanças no mecanismo de armazenamento.

**Adapter Pattern**: Adaptadores como `FipeApiAdapter` e `KafkaMessagePublisher` traduzem entre interfaces externas e o domínio interno, permitindo que o sistema se integre com diferentes tecnologias sem acoplar a lógica de negócio.

**Strategy Pattern**: Diferentes estratégias de cache podem ser implementadas através da interface `CacheServicePort`. Atualmente, usamos Redis, mas poderíamos adicionar outras implementações (Memcached, Caffeine) sem alterar os casos de uso.

**Observer Pattern**: O sistema usa eventos de domínio implicitamente através do Kafka. Quando uma marca é descoberta, um evento é publicado, e a API-2 reage a esse evento processando os modelos de veículos.

**Template Method Pattern**: Os casos de uso seguem um fluxo de processamento consistente: validação, execução, logging. Isso cria uma estrutura previsível e facilita a manutenção.

**Chain of Responsibility**: O filtro de autenticação JWT (`JwtAuthenticationFilter`) implementa este padrão, processando a requisição e passando para o próximo filtro na cadeia.

### Test-Driven Development (TDD)

Os testes foram desenvolvidos seguindo a prática de TDD, garantindo cobertura e qualidade do código.

**Testes Unitários**: Cada caso de uso e serviço possui testes unitários que verificam o comportamento esperado em diferentes cenários (sucesso, erro, casos extremos). Os testes usam mocks para isolar a unidade sob teste.

**Testes de Integração**: Testcontainers é usado para provisionar instâncias temporárias de PostgreSQL e Kafka, permitindo testes de integração realistas sem depender de ambientes externos.

**Cobertura de Código**: Os testes cobrem os principais fluxos de negócio, incluindo casos de sucesso e falha. Isso garante que mudanças futuras não quebrem funcionalidades existentes.

**Testes Reativos**: Os testes usam `StepVerifier` do Reactor Test para verificar o comportamento de fluxos reativos, garantindo que as operações assíncronas funcionem corretamente.

### Contract First (Swagger/OpenAPI)

A API é documentada usando SpringDoc OpenAPI 3, seguindo a abordagem Contract First.

**Documentação Automática**: As anotações `@Schema`, `@Operation` e `@Tag` geram documentação interativa automaticamente. Isso garante que a documentação esteja sempre sincronizada com o código.

**Swagger UI**: A interface Swagger UI (`http://localhost:8080/swagger-ui.html`) permite explorar e testar os endpoints de forma interativa, facilitando o desenvolvimento e integração.

**Contratos Claros**: Os DTOs definem contratos claros de entrada e saída, com validações e descrições detalhadas. Isso facilita a comunicação entre equipes e a integração com clientes.

### Clean Code

O código foi escrito seguindo os princípios de Clean Code, priorizando legibilidade e manutenibilidade.

**Nomes Significativos**: Classes, métodos e variáveis têm nomes descritivos que revelam sua intenção. Por exemplo, `LoadFipeDataUseCase`, `authenticateUser`, `brandCode`.

**Funções Pequenas**: Cada método faz uma coisa e faz bem. Métodos longos foram quebrados em funções menores e mais focadas.

**Comentários Úteis**: Comentários JavaDoc explicam o propósito de classes e métodos públicos, sem redundância. O código é autoexplicativo sempre que possível.

**Tratamento de Erros**: Erros são tratados de forma consistente usando exceções reativas e um handler global (`GlobalExceptionHandler`). Isso garante respostas de erro padronizadas e informativas.

**Formatação Consistente**: O código segue convenções de formatação consistentes, facilitando a leitura e manutenção.

## Troubleshooting

### Problemas Comuns e Soluções

**Os containers não iniciam**: Verifique se as portas 8080, 8081, 5432, 6379 e 9092 não estão sendo usadas por outros processos. Use `docker-compose down -v` para limpar volumes antigos e tente novamente.

**Erro de conexão com o banco de dados**: Aguarde alguns segundos após o `docker-compose up` para que o PostgreSQL inicialize completamente. Os health checks garantem que as APIs só iniciem após o banco estar pronto.

**Kafka não está recebendo mensagens**: Verifique os logs do Kafka e Zookeeper com `docker-compose logs kafka zookeeper`. Certifique-se de que o tópico `fipe.brands` foi criado automaticamente.

**Erro 401 Unauthorized**: Certifique-se de que você está incluindo o header `Authorization: Bearer <TOKEN>` em todas as requisições protegidas. Obtenha um token válido através do endpoint `/api/v1/auth/login`.

**Cache não está funcionando**: Verifique se o Redis está em execução com `docker-compose ps redis`. Consulte os logs da API-1 para verificar se há erros de conexão com o Redis.


## Licença

Este projeto é licenciado sob a Apache License 2.0. Consulte o arquivo LICENSE para mais detalhes.

## Contato

Para dúvidas, sugestões ou contribuições, entre em contato através do email: saulo.mendes@bsd.com.br

---
