# Registro de inspeção da implantação — TM-02

**Data:** 25/11/2026
**Responsável pela inspeção:** Maria Eduarda
**Ambiente:** Ubuntu Linux — máquina local
**Atividade:** TM-02 — implantação limpa conforme as instruções do README

## 1. Objetivo

Verificar as instruções e os arquivos de configuração necessários para realizar a implantação do Apurador JIF, seguindo a documentação do repositório.

A atividade contemplou a inspeção da configuração Docker Compose, dos Dockerfiles, da configuração do Angular, do Nginx e dos arquivos ignorados durante a construção das imagens.

## 2. Ambiente utilizado

- Sistema operacional: Ubuntu Linux.
- Memória RAM total: 7,6 GiB.
- Memória disponível durante a inspeção: 2,9 GiB.
- Memória swap total: 2,0 GiB.
- Memória swap utilizada: 1,5 GiB.
- Docker: não instalado.
- Docker Compose: não instalado.

A máquina não apresentou recursos disponíveis suficientes para realizar com segurança a construção e a execução simultânea dos serviços.

## 3. Arquivos inspecionados

### 3.1. docker-compose.yml

Foi identificada a configuração de sete serviços:

- PostgreSQL;
- RabbitMQ;
- Dispatcher;
- três Workers;
- Frontend.

Também foi identificado o volume persistente `dados-postgres`.

Foram conferidas as dependências entre os serviços, as variáveis de ambiente, as portas expostas e as verificações de saúde do PostgreSQL e do RabbitMQ.

### 3.2. docker/Dockerfile.java

Foi verificada a construção das imagens Java utilizando Maven 3.9.9 e Eclipse Temurin 21.

O Dockerfile recebe o argumento `MODULO`, permitindo construir as imagens do Dispatcher e dos Workers.

Também foi verificado o uso de uma imagem final baseada no Java 21 JRE e a execução com usuário sem privilégios administrativos.

### 3.3. POMs do Dispatcher e do Worker

Foi conferida a configuração do Spring Boot Maven Plugin nos arquivos:

- `dispatcher/pom.xml`;
- `worker/pom.xml`.

Ambos utilizam o classifier `exec`, compatível com o padrão de arquivo JAR esperado pelo Dockerfile Java.

### 3.4. docker/Dockerfile.frontend

Foi verificada a construção da aplicação Angular utilizando Node 22 Alpine e a disponibilização dos arquivos compilados por meio do Nginx.

### 3.5. frontend/angular.json

Foi conferido o projeto Angular denominado `frontend`, utilizando o construtor `@angular/build:application`.

O caminho de saída esperado pelo Dockerfile, `dist/frontend/browser`, é compatível com a estrutura padrão desse construtor.

### 3.6. docker/nginx.conf

Foi verificada a configuração do Nginx:

- Encaminhamento das requisições `/api/` para o Dispatcher;
- Bloqueio externo do caminho `/interno/`;
- Disponibilização da aplicação Angular e suporte às rotas da interface.

### 3.7. .dockerignore

Foi verificada a exclusão de diretórios e arquivos desnecessários ao contexto de construção, incluindo `target`, `node_modules`, `dist`, `.git`, documentação e diretórios de testes de carga e implantação Kubernetes.

## 4. Resultado da inspeção

Os arquivos examinados apresentam coerência entre si quanto à estrutura de construção e à configuração dos serviços.

Não foram identificadas incompatibilidades evidentes durante a inspeção estática.

Entretanto, essa análise não comprova a execução bem-sucedida das imagens ou dos serviços.

## 5. Limitações da execução local

A execução do comando `docker compose up --build` não foi realizada nesta máquina.

Os motivos foram:

- Docker e Docker Compose não estavam instalados;
- A memória disponível era de aproximadamente 2,9 GiB;
- A utilização de swap já alcançava aproximadamente 1,5 GiB;
- A implantação exige a construção de imagens e a execução simultânea de sete serviços.

Diante dessas condições, optou-se por não instalar e iniciar toda a infraestrutura local, evitando sobrecarregar o ambiente.

A implantação k3s também não foi executada nesta máquina.

## 6. Informações sobre as verificações do grupo

Conforme informação recebida do integrante Maycon, as verificações de construção, Docker Compose e k3s foram realizadas em sua máquina.

Essas execuções são registradas como atividades realizadas pelo integrante responsável pela infraestrutura. A responsável por este documento não realizou essas execuções nem examinou seus logs originais.

Assim, as informações são apresentadas como relato do integrante, não como resultado de validação independente desta inspeção.

## 7. Conclusão

A inspeção documental e estática dos arquivos de implantação foi concluída em 25/11/2026.

Foram identificados e examinados os principais componentes necessários à implantação, incluindo Docker Compose, construção das imagens Java e Angular, configuração do Nginx e exclusões do contexto Docker.

A execução independente do TM-02 nesta máquina permaneceu pendente devido à ausência do Docker e às limitações de recursos disponíveis.

Portanto, o resultado desta atividade deve ser interpretado como uma inspeção técnica da configuração de implantação, e não como uma validação prática de execução local.
