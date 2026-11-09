# Ecotecidos API

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.7-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Version](https://img.shields.io/badge/version-1.0.0-blue)

API RESTful da plataforma **Ecotecidos**, desenvolvida como projeto de conclusão do curso de **Desenvolvedor Full Stack (+PraTi)**. A solução conecta doadores de resíduos têxteis a pontos de coleta parceiros, incentivando a economia circular por meio de **cashback** e **educação ambiental**.

---

## Sumário

- [Principais Funcionalidades](#principais-funcionalidades)
- [Arquitetura](#arquitetura)
- [Banco de Dados](#banco-de-dados)
- [Pré-requisitos](#pré-requisitos)
- [Instalação e Configuração](#instalação-e-configuração)
- [Carga Inicial de Usuários (Seed)](#carga-inicial-de-usuários-seed)
- [Comandos de Execução](#comandos-de-execução)
- [Documentação da API e Monitoramento](#documentação-da-api-e-monitoramento)
- [Equipe de Desenvolvimento](#equipe-de-desenvolvimento)

---

## Principais Funcionalidades

- **Autenticação, Autorização (RBAC) e Segurança**
  - Auto-cadastro público de doadores (`POST /api/v1/register`) e autenticação *stateless* via token **JWT** (`POST /api/v1/auth/login`).
  - Controle de acesso baseado em 4 perfis (`Role`):
    - `DOADOR`: registra descartes, acumula/resgata cashback e favorita conteúdos/pontos.
    - `PONTO_COLETA_OPERADOR`: valida, pesa e confirma/rejeita entregas presenciais de tecidos.
    - `PONTO_COLETA_GERENTE`: gerencia dados operacionais, equipe de operadores e acompanhamento do ponto de coleta.
    - `ADMIN`: governa usuários, pontos de coleta, catálogo de tecidos, conteúdos educativos e regras de conversão de cashback.
  - Fluxo completo de recuperação e redefinição de senha com envio assíncrono de e-mail HTML (**Spring Mail + Thymeleaf**) e tokens temporários armazenados via *hash*.
  - Validações customizadas de CPF/CNPJ (`@CpfOrCnpj`) e telefone internacional E.164 (`@InternationalPhone` via `libphonenumber`).

- **Gestão de Perfil e Armazenamento de Imagens**
  - Atualização de dados cadastrais, troca de senha e upload/remoção de foto de perfil.
  - Processamento seguro de imagens (**JPEG, PNG e WebP** via `webp-imageio` e Java AWT) para avatares, fotos de pontos de coleta e capas de publicações.

- **Pontos de Coleta, Moderação e Geolocalização**
  - Cadastro, fluxo de aprovação administrativa de status (`PENDING`, `ACTIVE`, `PAUSED`, `SUSPENDED`) e moderação de alterações solicitadas pelos pontos (`CollectionPointPendingUpdate`).
  - Definição de horários de atendimento por dia da semana, tipos de tecidos aceitos, listagem paginada, sistema de favoritos e **busca por proximidade geográfica** (`latitude` e `longitude`).

- **Fluxo de Descarte de Tecidos (`Disposals`)**
  - Abertura de solicitações de descarte pelo doador vinculadas a um ponto de coleta e aos tipos de tecido entregues.
  - Conferência no ponto de coleta com registro de peso aproximado (kg), confirmação (`CONFIRMED`) ou rejeição fundamentada (`REJECTED`), além de painéis resumidos para doadores e pontos.

- **Carteira Digital e Sistema de Cashback**
  - Carteira individual por usuário (`CashbackWallet`) protegida contra concorrência com *Optimistic Locking* (`@Version`).
  - Crédito automático (`CashbackCredit`) calculado com base no peso validado e na regra vigente de pontos por quilo (`CashbackRule`), com rotina agendada de expiração (`@Scheduled`).
  - Resgate de saldo em lojas parceiras (`CashbackDebit`) e consulta de extrato paginado de transações.

- **Conteúdo Educativo (Blog)**
  - Publicação e consulta de artigos educativos categorizados, com geração automática e busca por `slug` (`/posts/slug/{slug}`), filtros combinados por categoria (`categoryId`), termo textual (`searchTerm` em título, resumo e slug), tipo de tecido (`clothType`) e `tags`, além de contador de visualizações, favoritos e exclusão lógica (*soft delete*) auditada.

---

## Arquitetura

A aplicação segue a **Arquitetura em Camadas (Layered Architecture)** sobre o ecossistema Spring Boot, mantendo separação estrita entre contratos HTTP (DTOs), regras de negócio, mapeamento de objetos e persistência de dados.

### Stack Tecnológica

| Camada / Recurso | Tecnologia |
| :--- | :--- |
| Linguagem & Runtime | Java 21 (LTS) |
| Framework Principal | Spring Boot 4.0.7 (WebMVC, Validation, Security, Data JPA, Mail, Actuator) |
| Autenticação | Auth0 `java-jwt` 4.4 (HMAC256) |
| Persistência & Migrações | PostgreSQL 16 + Hibernate + Flyway |
| Mapeamento & Produtividade | MapStruct 1.6.2 + Lombok |
| Documentação Interativa | SpringDoc OpenAPI 3.0.2 (Swagger UI) |
| Templates & Utilitários | Thymeleaf (e-mails HTML), Google `libphonenumber`, Sejda `webp-imageio` |
| Containerização | Docker (Multi-stage build) & Docker Compose |

### Estrutura de Pacotes (`br.com.maisprati.ecotecidos`)

```text
src/main/
├── java/br/com/maisprati/ecotecidos/
│   ├── config/
│   │   ├── security/  # Configuração do Spring Security, CORS e filtro JWT
│   │   ├── seed/      # Carga inicial idempotente de usuários (DataSeeder)
│   │   └── OpenApiConfig.java
│   ├── controller/    # Endpoints REST, paginação e contratos Swagger
│   ├── dto/           # Projeções, requisições (request) e respostas (response)
│   ├── exception/     # Tratamento global padronizado de exceções (GlobalExceptionHandler)
│   ├── mapper/        # Interfaces MapStruct para conversão entre Entidades e DTOs
│   ├── model/
│   │   ├── entity/    # Entidades JPA, relacionamentos e objetos embarcados (@Embeddable)
│   │   └── enums/     # Enumerações de domínio (Role, Status, UF, etc.)
│   ├── repository/    # Repositórios Spring Data JPA e consultas customizadas
│   ├── service/       # Regras de negócio, transações, expiração de cashback, arquivos e e-mails
│   ├── util/          # Utilitários auxiliares (normalização textual e telefones E.164)
│   └── validation/    # Anotações e validadores customizados (@CpfOrCnpj, @InternationalPhone)
└── resources/
    ├── db/migration/  # Scripts SQL versionados do Flyway
    ├── templates/     # Templates HTML Thymeleaf para envio de e-mails
    ├── application.properties       # Regras globais da aplicação
    ├── application-dev.properties   # Configurações do perfil de desenvolvimento
    └── application-prod.properties  # Configurações otimizadas para produção
```

**Padronização de Erros:** todas as exceções da API são interceptadas pelo `GlobalExceptionHandler` e retornam respostas padronizadas no formato `ErrorResponseDTO` (`timestamp`, `status`, `error`, `message`, `path`), incluindo lista detalhada de `fieldErrors` (`ValidationErrorResponseDTO`) em falhas de validação HTTP `400`.

---

## Banco de Dados

O projeto utiliza **PostgreSQL 16**. A evolução do esquema é controlada exclusivamente pelo **Flyway** (`spring.flyway.enabled=true`), enquanto o Hibernate opera em modo de validação estrita (`spring.jpa.hibernate.ddl-auto=validate`), garantindo integridade entre o código Java e o banco relacional em todos os ambientes.

### Visão Geral das Tabelas

| Tabela | Descrição |
| :--- | :--- |
| `users` / `users_roles` | Cadastro de usuários, credenciais (BCrypt), status ativo, avatar e perfis de acesso (`Role`). |
| `password_reset_tokens` | Tokens temporários (em *hash*) com data de expiração para recuperação de senha. |
| `collection_points` | Pontos de coleta com endereço embarcado (CEP, UF, `latitude`, `longitude`), foto e status operacional. |
| `collection_point_pending_updates` | Payload JSON de atualizações cadastrais pendentes de aprovação pelo administrador. |
| `collection_point_operating_hours` | Dias da semana e horários de abertura/fechamento de cada ponto de coleta. |
| `collection_point_managers` / `_operators` | Vínculo `N:N` de gerentes e operadores responsáveis por cada ponto de coleta. |
| `cloth_types` | Catálogo de tipos de tecidos aceitos na plataforma (ex.: algodão, jeans, poliéster). |
| `disposals` / `disposal_cloth_types` | Registros de descartes de tecidos, peso (kg), operador validador, status e justificativa. |
| `cashback_wallet` | Saldo consolidado de pontos/cashback do usuário com controle de versão (`version`). |
| `cashback_transaction` | Histórico unificado (*Single Table Inheritance* via `transaction_type`: `CREDIT` e `DEBIT`), indexado por carteira e data. |
| `cashback_rules` | Parâmetros vigentes de conversão de pontos gerados por quilo de tecido descartado. |
| `categories` / `posts` / `post_tags` / `post_cloth_types` | Artigos educativos, categorias, tags, tecidos abordados, visualizações e metadados de *soft delete*. |
| `users_favorite_posts` / `_collection_points` | Tabelas associativas de conteúdos e pontos favoritados pelos usuários. |

---

## Pré-requisitos

### Opção 1 — Execução com Docker (Recomendado)
- **Docker Engine** `24.0+`
- **Docker Compose** `v2.20+`

### Opção 2 — Execução Local (Desenvolvimento)
- **Java JDK 21**
- **Maven** `3.9+` *(opcional — o projeto inclui o Maven Wrapper `mvnw` / `mvnw.cmd`)*
- **PostgreSQL** `16+` rodando localmente

---

## Instalação e Configuração

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/lucastnsoares/ecotecidos-api.git
   cd ecotecidos-api
   ```

2. **Crie o arquivo `.env` na raiz do projeto** copiando o modelo `.env.example`:
   ```bash
   # Linux / macOS
   cp .env.example .env

   # Windows (PowerShell)
   Copy-Item .env.example .env
   ```

3. **Configure as variáveis de ambiente no `.env`:**

   > [!WARNING]
   > **Atenção às credenciais e parâmetros padrão (`dev` e `prod`):** para permitir que o projeto suba rapidamente em testes e homologação via Docker, a aplicação fornece valores padrão de inicialização. No entanto, é **altamente recomendável** sobrescrever no `.env`:
   > - **`DB_PASSWORD` e `DB_USER`**: em produção, **substitua obrigatoriamente** a senha padrão (`postgres`) por uma senha forte e utilize um usuário de banco dedicado com permissões restritas. Em desenvolvimento local (`dev`), defina `DB_PASSWORD` caso a senha da sua instalação local do PostgreSQL seja diferente de `postgres`.
   > - **`TOKEN_JWT_SECRET`**: substitua o fallback por uma chave aleatória longa e exclusiva (mínimo de 32 caracteres) para evitar falsificação de tokens JWT.
   > - **`FRONTEND_URL` e `API_SECURITY_CORS_ALLOWED_ORIGINS`**: informe o domínio HTTPS oficial do frontend; caso contrário, os navegadores serão bloqueados por CORS e os links de redefinição de senha nos e-mails apontarão para `localhost`.
   > - **`MAIL_USERNAME`, `MAIL_PASSWORD` e `MAIL_FROM`**: como `app.mail.enabled=true` é fixo em `prod`, sem credenciais SMTP válidas o envio de e-mails de recuperação de senha e convite administrativo falhará. Em desenvolvimento local (`dev`), caso não queira configurar SMTP real, basta definir `MAIL_ENABLED=false` para simular os e-mails no log do console.

   ### Variáveis Principais (Servidor, Banco, Segurança e E-mail)

   | Variável | Status em `prod` | Padrão (`dev` / `prod`) | Descrição |
   | :--- | :---: | :--- | :--- |
   | `SPRING_PROFILES_ACTIVE` | Opcional | `dev` (local) / `prod` (Docker) | Perfil ativo do Spring (`dev` ou `prod`). |
   | `SERVER_PORT` | Opcional | `8080` | Porta HTTP exposta pela API. |
   | `MANAGEMENT_PORT` | Opcional | `9090` | Porta dedicada ao Spring Actuator. |
   | `DB_HOST` | Opcional | `localhost` / `postgres` (Docker) | Host do banco PostgreSQL. |
   | `DB_PORT` | Opcional | `5432` | Porta do PostgreSQL. |
   | `DB_NAME` | Opcional | `projeto_final_dev` (`dev`) / `ecotecidos_db` (`prod`) | Nome do banco de dados. |
   | `DB_USER` | **Altamente recomendável** | `postgres` | Usuário do banco de dados. |
   | `DB_PASSWORD` | **Altamente recomendável** | `postgres` | Senha do banco de dados (altere em `prod` por segurança e em `dev` se seu banco local usar outra senha). |
   | `TOKEN_JWT_SECRET` | **Altamente recomendável** | Fallback temporário | Chave secreta forte para assinatura HMAC256 dos tokens JWT. |
   | `TOKEN_ISSUER` | Opcional | `ecotecidos-api` | Identificador emissor (`iss`) do JWT. |
   | `TOKEN_EXPIRATION_SECONDS` | Opcional | `3600` | Tempo de expiração do token JWT em segundos. |
   | `FRONTEND_URL` | **Altamente recomendável** | `http://localhost:5173` | URL base do frontend (usada nos links de e-mail). |
   | `API_SECURITY_CORS_ALLOWED_ORIGINS` | **Altamente recomendável** | `http://localhost:3000,http://localhost:5173` | Lista de origens permitidas no CORS (separadas por vírgula). |
   | `MAIL_ENABLED` | Fixo `true` em `prod` | `true` (`dev` e `prod`) | Em `prod` o envio real é sempre ativo; em `dev` pode ser `false` para simular no log. |
   | `MAIL_SMTP_HOST` | Opcional | `smtp.gmail.com` | Servidor SMTP para envio de e-mails. |
   | `MAIL_SMTP_PORT` | Opcional | `587` | Porta SMTP (StartTLS). |
   | `MAIL_FROM` | **Altamente recomendável** | `noreply@ecotecidos.com.br` (`prod`) | Endereço remetente exibido nos e-mails enviados. |
   | `MAIL_USERNAME` | **Altamente recomendável** | Vazio | Conta de autenticação no servidor SMTP. |
   | `MAIL_PASSWORD` | **Altamente recomendável** | Vazio | Senha de aplicativo (*App Password*) do provedor de e-mail. |
   | `FILE_UPLOAD_DIR` | Opcional | `uploads` (`/app/uploads` no Docker) | Diretório de persistência de imagens enviadas. |
   | `SWAGGER_ENABLED` | Opcional | `false` (`prod`) | Permite habilitar o Swagger UI no perfil `prod` quando `true`. |

   ### Variáveis de Seed dos Usuários Iniciais (`DataSeeder`)

   | Variável | Padrão em `dev` | Padrão em `prod` | Descrição |
   | :--- | :--- | :---: | :--- |
   | `SEED_ENABLED` | `true` | `true` | Ativa (`true`) ou desativa (`false`) a execução do `DataSeeder` na inicialização. |
   | `SEED_ADMIN_NAME` | `ADMINISTRADOR DO SISTEMA` | — *(Altamente recomendável)* | Nome completo do usuário `ADMIN` inicial. |
   | `SEED_ADMIN_DOCUMENT` | `52998224725` | — *(Altamente recomendável)* | CPF/CNPJ válido do `ADMIN` inicial. |
   | `SEED_ADMIN_EMAIL` | `admin@projeto.com.br` | — *(Altamente recomendável)* | E-mail de login do `ADMIN` inicial. |
   | `SEED_ADMIN_PHONE` | `+5531987654321` | — *(Opcional)* | Telefone do `ADMIN` inicial (formato E.164 ou nacional). |
   | `SEED_ADMIN_PASSWORD` | `Admin@123456` | — *(Altamente recomendável)* | Senha inicial do `ADMIN` (mínimo de 6 caracteres). |
   | `SEED_DOADOR_NAME` | `USUARIO DOADOR TESTE` | — *(Opcional)* | Nome completo do usuário `DOADOR` inicial. |
   | `SEED_DOADOR_DOCUMENT` | `11144477735` | — *(Opcional)* | CPF/CNPJ válido do `DOADOR` inicial. |
   | `SEED_DOADOR_EMAIL` | `doador@projeto.com.br` | — *(Opcional)* | E-mail de login do `DOADOR` inicial. |
   | `SEED_DOADOR_PHONE` | `+5511912345678` | — *(Opcional)* | Telefone do `DOADOR` inicial. |
   | `SEED_DOADOR_PASSWORD` | `Doador@123456` | — *(Opcional)* | Senha inicial do `DOADOR` (mínimo de 6 caracteres). |

---

## Carga Inicial de Usuários (Seed)

Na inicialização da aplicação, o componente `DataSeeder` verifica de forma **idempotente** (por e-mail e documento) se os usuários iniciais já existem no banco antes de criá-los:

- **No perfil `dev`:** caso nenhuma variável `SEED_*` seja sobrescrita, são criadas automaticamente duas contas de teste para facilitar o desenvolvimento:
  - **Administrador (`ADMIN`):** `admin@projeto.com.br` / senha `Admin@123456`
  - **Doador (`DOADOR`):** `doador@projeto.com.br` / senha `Doador@123456`
- **No perfil `prod`:** **não existem credenciais fixas no código**. Para provisionar o primeiro Administrador (e, opcionalmente, um Doador inicial), preencha as variáveis `SEED_ADMIN_*` (e `SEED_DOADOR_*`, se desejar) no `.env`. Caso um dos blocos fique em branco em `prod`, esse usuário é simplesmente ignorado sem gerar erro.

> [!IMPORTANT]
> **Recomendação para o primeiro deploy em `prod`:** como o endpoint público `POST /api/v1/register` cadastra exclusivamente contas com perfil `DOADOR`, é **altamente recomendável** preencher as variáveis `SEED_ADMIN_*` antes da primeira inicialização para garantir a criação da conta administrativa inicial com credenciais seguras escolhidas por você.

---

## Comandos de Execução

### Via Docker Compose (Produção / Ambiente Completo)

Sobe automaticamente o banco **PostgreSQL 16** (com *healthcheck* e volume persistente), executa o build *multi-stage* da API em **Java 21**, aplica as migrações do **Flyway**, executa o `DataSeeder` (se configurado) e inicia o servidor:

```bash
# Construir a imagem e iniciar os containers em segundo plano
docker compose up -d --build

# Acompanhar os logs da API em tempo real
docker compose logs -f api

# Verificar o status dos containers
docker compose ps

# Acessar o banco de dados diretamente no container (já que a porta 5432 fica isolada na rede interna)
docker exec -it ecotecidos-db psql -U postgres -d ecotecidos_db

# Parar os containers (mantendo os dados do banco e uploads)
docker compose down

# Parar os containers e APAGAR os volumes persistentes (reset completo)
docker compose down -v
```

### Execução Local (Desenvolvimento)

Certifique-se de que o PostgreSQL está ativo, que o banco configurado em `DB_NAME` existe (ex.: `CREATE DATABASE projeto_final_dev;`) e que `DB_PASSWORD` no `.env`/IDE corresponda à senha do seu banco local (caso não seja a padrão `postgres`):

```bash
# Linux / macOS — Executar em modo de desenvolvimento
./mvnw spring-boot:run

# Windows (PowerShell) — Executar em modo de desenvolvimento
.\mvnw.cmd spring-boot:run
```

### Testes e Build do Artefato (`.jar`)

```bash
# Executar a suíte de testes automatizados
./mvnw test

# Gerar o pacote executável de produção em target/ecotecidos-1.0.0.jar
./mvnw clean package -DskipTests

# Executar o JAR gerado diretamente no perfil de produção
java -jar target/ecotecidos-1.0.0.jar --spring.profiles.active=prod
```

---

## Documentação da API e Monitoramento

Com a aplicação em execução (porta padrão `8080` e Actuator na porta `9090`):

- **URL Base da API:** `http://localhost:8080/api/v1`
- **Swagger UI (Interativo):** `http://localhost:8080/api/v1/swagger-ui.html` *(ativo por padrão em `dev`; em `prod`, defina `SWAGGER_ENABLED=true`)*
- **Especificação OpenAPI (JSON):** `http://localhost:8080/api/v1/api-docs`
- **Healthcheck Geral (Actuator):** `http://localhost:9090/actuator/health`
- **Probes de Container:**
  - Liveness: `http://localhost:9090/actuator/health/liveness`
  - Readiness: `http://localhost:9090/actuator/health/readiness`

> **Como autenticar no Swagger UI:** execute `POST /auth/login` com e-mail e senha válidos, copie o `token` retornado e clique no botão **Authorize** no topo da página informando o token no esquema `bearerAuth`.

---

## Equipe de Desenvolvimento

| Integrante | GitHub |
| :--- | :--- |
| **Alan Silva** | [@AlanMatheus18](https://github.com/AlanMatheus18) |
| **Bruna Santiago** | [@brunasdr9412](https://github.com/brunasdr9412) |
| **Eduardo Podgaiskis** | [@eduardopodgaiskis0afk](https://github.com/eduardopodgaiskis0afk) |
| **Lucas Soares** | [@lucastnsoares](https://github.com/lucastnsoares) |
| **Luis Carlos Paulon** | [@carlospaulon](https://github.com/carlospaulon) |
| **Rafael Kuchla Flores** | [@rafael-kuchla-flores](https://github.com/rafael-kuchla-flores) |

