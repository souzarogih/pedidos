# AGENTS.md

Fonte principal de regras para qualquer agente (humano ou IA) que trabalhe neste repositório.

## Objetivo do serviço

API de pedidos ("pedidos"). O domínio de negócio ainda será definido incrementalmente através das specs em `.specs/`.

## Stack

- Java 21
- Spring Boot (versão fixada em `pom.xml` — consulte o arquivo, não assuma)
- Build e execução exclusivamente via Maven Wrapper, nunca via `mvn` global:
  - `./mvnw test`
  - `./mvnw spring-boot:run`
  - `./mvnw clean verify`

## Arquitetura

Arquitetura hexagonal (ports & adapters):

- **Domínio** (`domain`): entidades, value objects e regras de negócio. Sem anotações Spring, sem JPA, sem qualquer dependência de framework.
- **Aplicação** (`application`): casos de uso, orquestração. Também sem Spring/JPA — depende apenas de portas (interfaces) definidas pelo próprio domínio/aplicação.
- **Infraestrutura/Adapters**: única camada onde Spring, JPA, controllers REST, repositórios etc. podem aparecer, implementando as portas definidas nas camadas internas.

## Convenções de código

- Valores monetários sempre representados com `BigDecimal` (nunca `double`/`float`).
- Lombok é proibido. Escrever construtores, getters, equals/hashCode etc. manualmente ou via `record` quando fizer sentido.
- Nenhuma dependência nova (Maven) deve ser adicionada sem antes perguntar e obter aprovação explícita.

## Fluxo de trabalho esperado

1. Ler a spec indicada em `.specs/` (a pasta só deve ser lida quando o agente for explicitamente instruído a fazer isso).
2. Planejar a implementação e apresentar o plano.
3. Aguardar aprovação ("OK") antes de editar qualquer arquivo.
4. Implementar, seguindo as regras acima.
5. Rodar os testes via Maven Wrapper.
6. Mostrar o diff das mudanças antes de considerar a tarefa concluída.

Nenhum passo acima deve ser pulado, mesmo em tarefas que pareçam pequenas.
