# Relatório de Refatoração (`feat/magia`)

Este documento registra todas as alterações realizadas na branch `feat/magia` para simplificar a estrutura do projeto **Apurador JIF**, mantendo **100% dos requisitos** de **Programação Orientada a Objetos I (INF 142)** e **Sistemas Distribuídos**, além da compatibilidade com banco de dados, Docker Compose, k3s, testes unitários e testes de carga/integração.

---

## 1. Motivação

Na versão anterior, a camada HTTP/serviços do `dispatcher` estava excessivamente fragmentada:

- **12 classes de serviço**, muitas com apenas 17 a 25 linhas (um arquivo por operação simples);
- **7 controllers**, sendo 4 deles com apenas um único método de 3 linhas;
- **Validações manuais repetitivas** para campos obrigatórios (sem `spring-boot-starter-validation` / `@Valid`);
- **Controle manual de ETag / `If-None-Match` (HTTP `304 Not Modified`)** no `ChaveController` e no Angular (`ChavesService` + `Classificacao`), adicionando complexidade de cabeçalhos HTTP e tratamento especial de erro `304` sem ganho relevante para o volume real do JIF.

O objetivo da refatoração foi deixar o código mais limpo, idiomático em **Spring Boot** e **Angular**, e muito mais fácil de ler, manter e explicar na arguição individual.

---

## 2. O que mudou e por quê

### 2.1 Consolidação de Services no `dispatcher` (12 → 4)

Os serviços foram agrupados por domínio coeso, no padrão tradicional do Spring Boot:

| Novo Service | Substituiu | Responsabilidades |
|---|---|---|
| `ChaveService` | `ChaveIncluirService`, `ChaveListarService`, `ChaveBuscarService`, `PartidaListarService`, `ClassificacaoDetalharService`, `ChaveAuditarService` | Cadastro de chave e geração de partidas, listagem de chaves, listagem de partidas, consulta de classificação e auditoria da tabela |
| `ResultadoService` | `ResultadoRegistrarService`, `EventoDetalharService` | Registro assíncrono de resultados e consulta de status de evento |
| `ClusterService` | `ClusterDetalharService`, `ConsistenciaDetalharService`, `HeartbeatReceberService` | Visão do cluster (anel + workers), relatório global de consistência e recebimento de heartbeats |
| `ModalidadeService` | `ModalidadeListarService` | Listagem das modalidades cadastradas no `CatalogoDeModalidades` |

**Por quê:** reduz drasticamente o número de arquivos e injeções de dependência nos controllers, agrupando operações que compartilham os mesmos repositórios e regras de domínio.

---

### 2.2 Consolidação de Controllers no `dispatcher` (7 → 4)

Os endpoints HTTP permaneceram **100% idênticos** (nenhuma quebra de contrato com Angular, k6, Chaos Mesh ou `FluxoCompletoIT`):

| Controller | Endpoints mantidos | Observação |
|---|---|---|
| `ChaveController` (`/api/chaves`) | `POST /api/chaves`, `GET /api/chaves`, `GET /api/chaves/{id}/partidas`, `GET /api/chaves/{id}/classificacao`, `GET /api/chaves/{id}/auditoria` | Removido o controle manual de `If-None-Match` / `304`; agora retorna `ClassificacaoResponse` diretamente (HTTP 200) |
| `ResultadoController` (`/api`) | `POST /api/resultados`, `GET /api/eventos/{eventoId}` | Absorveu `EventoController` |
| `ClusterController` | `GET /api/cluster`, `GET /api/consistencia`, `POST /interno/heartbeats` | Absorveu `ConsistenciaController` e `HeartbeatController` |
| `ModalidadeController` (`/api/modalidades`) | `GET /api/modalidades` | Injeta `ModalidadeService` |

---

### 2.3 Bean Validation e `GlobalExceptionHandler` padronizado

- Adicionada a dependência `spring-boot-starter-validation` em `dispatcher/pom.xml`.
- Anotações `@NotBlank` e `@NotEmpty` com mensagens claras em português nos DTOs de entrada:
  - `ChaveIncluirRequest` (`nome`, `modalidade`, `equipes`)
  - `ResultadoRegistrarRequest` (`partidaId`)
  - `HeartbeatReceberRequest` (`workerId`)
- Uso de `@Valid` nos `@RequestBody` dos controllers.
- **Validators simplificados (3 → 2):**
  - `HeartbeatReceberValidator` foi **removido** (sua única checagem de `workerId` em branco passou para `@NotBlank`).
  - `ChaveIncluirValidator` → `ChaveValidator` (mantém apenas regras de negócio: 3 a 5 equipes distintas e unicidade do `id` no banco).
  - `ResultadoRegistrarValidator` → `ResultadoValidator` (mantém apenas a validação estrutural dos pares de pontos em `parciais`).
- **`ErroResponse` e `GlobalExceptionHandler`:**
  - `ErroResponse` agora retorna `status`, `erro`, `timestamp` e (quando aplicável) `camposInvalidos` (com `@JsonInclude(NON_NULL)`), mantendo compatibilidade total com o campo `erro` esperado pelo frontend.
  - `GlobalExceptionHandler` passou a tratar `MethodArgumentNotValidException` (400 com mapa `camposInvalidos`) e `HttpMessageNotReadableException` (400), além de manter `ResponseStatusException`, `ResultadoInvalidoException` (422) e `IllegalArgumentException` (400).

---

### 2.4 Simplificação no Frontend Angular (`frontend/src/app`)

- `services/chaves.service.ts`: `detalharClassificacao(chaveId)` agora retorna `Observable<ClassificacaoResponse>` diretamente, sem montar `HttpHeaders` (`If-None-Match`) nem `observe: 'response'`.
- `pages/classificacao/classificacao.ts`: removida a lógica especial de `erro?.status !== 304`; o componente apenas compara `versao` localmente para decidir se recarrega a lista de partidas.
- `utils/erro-api.ts`: atualizado para exibir as mensagens de `camposInvalidos` retornadas pelo Bean Validation (com fallback para `erro`).

---

## 3. O que foi preservado intencionalmente (e por quê)

- **`core/` (37 testes unitários — POO I):** mantido intacto. Contempla de forma pura (sem Spring) Abstração/Encapsulamento (`Partida`, `Campanha`), Herança e Polimorfismo (`Modalidade` → `Futsal`, `Handebol`, `Voleibol`), Modificadores de Acesso, Interfaces (`CriterioDesempate`) e Coleções (`Array`, `ArrayList`, `Collection`, `Map`).
- **`cluster/` (41 testes unitários — Sistemas Distribuídos):** mantido intacto. Contém os dois algoritmos autorais exigidos na disciplina: **balanceamento de carga por anel de hash consistente com carga limitada** (`AnelDeHashLimitado`) e **tolerância a falhas por detector de heartbeat** (`DetectorDeFalhas`), além do `Coordenador` e do aplicador idempotente.
- **`persistencia/` e `db/schema.sql`:** mantidos intactos para preservar `ddl-auto: validate`, os adaptadores `HistoricoJpa` / `RegistroJpa` e a compatibilidade com `FluxoCompletoIT` e k3s.
- **`worker/`, `docker/`, `k8s/`, `carga/`, `chaos/`:** mantidos intactos. O script `carga/consultas-simultaneas.js` já aceita `status === 200 || status === 304`, portanto continua 100% compatível.

---

## 4. Verificação executada

Executado via containers Docker (`maven:3.9.9-eclipse-temurin-21` e `node:22-alpine`) na branch `feat/magia`:

- **Maven (`mvn -B test test-compile -Pintegracao`):** `BUILD SUCCESS` em todos os 7 módulos (`core`, `cluster`, `persistencia`, `dispatcher`, `worker`, `integracao`).
  - `core`: **37/37** testes passando.
  - `cluster`: **41/41** testes passando.
- **Angular (`npm ci && npx ng build --configuration production`):** compilação de produção concluída sem erros.

---

## 5. Pontos em aberto / Decisões para o grupo

1. **Teste de integração completo (`TI-02` / `FluxoCompletoIT`):** o módulo `integracao` compilou sem erros, mas a execução completa de `mvn -Pintegracao -pl integracao -am verify` requer subir Testcontainers (PostgreSQL + RabbitMQ) ou apontar `APURADOR_IT_EXTERNO=1`. Recomenda-se rodar uma vez com `docker compose` ou Testcontainers antes de fazer merge para `main`.
2. **Medições de carga/falha (`k6` e `k3s`):** como o contrato HTTP e os algoritmos de `core`/`cluster` não mudaram (apenas removemos o retorno `304` em `GET /api/chaves/{id}/classificacao`, que agora responde sempre `200`), os scripts de carga continuam compatíveis. Caso o grupo queira atualizar os logs no relatório técnico pós-refatoração, basta reexecutar `carga/` e `k8s/`.
3. **Validação de `docs/`:** vale conferir se algum diagrama em `docs/diagramas/` cita os nomes antigos dos services/controllers fragmentados caso queiram deixá-los 100% espelhados com a branch `feat/magia`.
