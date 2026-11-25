# Registro de execução dos testes — 25/11/2026

## 1. Ambiente

- Sistema operacional: Ubuntu 22.04.5 LTS.
- Java: OpenJDK 21.0.13.1.
- Maven: Apache Maven 3.10.2.
- Execução: local, em ambiente Linux.
- Data: 25/11/2026.

## 2. Comando executado

`mvn -pl core,cluster -am test`

O parâmetro `-pl` seleciona os módulos `core` e `cluster`.
O parâmetro `-am` inclui os módulos dos quais eles dependem.

## 3. Resultado geral

- Resultado do Maven: BUILD SUCCESS.
- Módulos de aplicação testados: `core` e `cluster`.
- Classes de teste executadas: 12.
- Total de testes: 78.
- Falhas: 0.
- Erros: 0.
- Testes ignorados: 0.

## 4. Resultados por módulo

### 4.1 Core

| Classe de teste | Testes | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|
| ClassificadorTest | 6 | 0 | 0 | 0 |
| FutsalEmpateTriploTest | 4 | 0 | 0 | 0 |
| FutsalTest | 10 | 0 | 0 | 0 |
| HandebolTest | 4 | 0 | 0 | 0 |
| ImutabilidadeTest | 4 | 0 | 0 | 0 |
| NovaModalidadeTest | 3 | 0 | 0 | 0 |
| VoleibolTest | 6 | 0 | 0 | 0 |
| **Total** | **37** | **0** | **0** | **0** |

### 4.2 Cluster

| Classe de teste | Testes | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|
| AnelDeHashLimitadoTest | 11 | 0 | 0 | 0 |
| AplicadorDeEventosTest | 10 | 0 | 0 | 0 |
| CoordenadorTest | 10 | 0 | 0 | 0 |
| DetectorDeFalhasTest | 8 | 0 | 0 | 0 |
| SimulacaoDeFalhaTest | 2 | 0 | 0 | 0 |
| **Total** | **41** | **0** | **0** | **0** |

## 5. Evidências geradas

O Maven Surefire gerou relatórios individuais em:

- `core/target/surefire-reports/`
- `cluster/target/surefire-reports/`

Esses diretórios ficam dentro de `target` e são ignorados pelo Git. Os arquivos contêm os resultados detalhados de cada classe de teste.

## 6. Escopo e limitações

Esta execução confirma a compilação e os testes automatizados dos módulos `core` e `cluster`, incluindo suas dependências no projeto Maven.

Não representa uma nova execução dos sete módulos do sistema, dos testes de integração real com RabbitMQ e PostgreSQL (TI-02), dos testes de infraestrutura, dos testes de carga ou dos cenários de falha em Kubernetes.

Os resultados de outras modalidades de teste permanecem documentados em seus respectivos registros históricos.

## 7. Resultado final

Em 25/11/2026, os módulos `core` e `cluster` foram compilados e testados localmente em Ubuntu, com JDK 21 e Maven 3.10.2.

Os 78 testes executados passaram, sem falhas, erros ou testes ignorados.
