# 🎫 ticket-fast

> **대규모 트래픽 대응을 위한 고성능 티켓 예매 플랫폼**
>
> 한정된 인프라 환경 내에서 자원 효율성을 극대화하기 위해 **Spring WebFlux 기반의 비동기 논블로킹 아키텍처**를 적용하였으며, **Redis**, **Kafka**, **Qdrant**를 결합하여 대규모 티켓팅 상황을 고려한 MSA 플랫폼을 구현했습니다.

---

## 🏗️ System Architecture

```text
                     [ Client (Frontend) ]
                               │
                               ▼
─────────────────────── [ ticket-gateway ] ───────────────────────
   (WebFlux / 최전방 JWT 인증 필터 및 내부 보안 헤더 인젝션)
                               │
        ┌──────────────────────┼──────────────────────┐
        ▼                      ▼                      ▼
[ member-service ]     [ order-service ]      [ ai-service ]
 (Spring MVC / Tomcat) (WebFlux / Netty)     (WebFlux / Netty)
        │                      │                      │
        ▼                      ├──────────────────┐   ▼
 ┌──────────────┐              ▼                  ▼  ┌──────────────┐
 │   MySQL DB   │       ┌──────────────┐   ┌────────┐│ Qdrant DB    │
 │ (User, ACID) │       │  Redis Cache │   │ Kafka  ││(Vector Store)│
 └──────────────┘       │(Seat Layout/ │   │ (Batch │└──────────────┘
                        │ Gatekeeper)  │   │ Insert)│
                        └──────────────┘   └────────┘
```

---

## 🛠️ Tech Stack

### Backend

* Spring Boot
* Spring WebFlux
* Spring MVC
* Spring Cloud Gateway
* Spring AI
* R2DBC
* Spring Data JPA

### Database & Middleware

* MySQL
* Redis
* Apache Kafka
* Qdrant Vector DB

### Infrastructure

* Docker
* Docker Compose
* AWS EC2

---

## 📦 Microservices

| Service                 | Runtime             | Responsibility                   |
| ----------------------- | ------------------- | -------------------------------- |
| `ticket-gateway`        | WebFlux (Netty)     | API Gateway, JWT 검증, 내부 보안 헤더 전파 |
| `ticket-member-service` | Spring MVC (Tomcat) | 회원 가입/로그인, 사용자 정보 관리             |
| `ticket-order-service`  | WebFlux (Netty)     | 티켓 예매, 좌석 선점, 이벤트 발행             |
| `ticket-ai-service`     | WebFlux (Netty)     | RAG 기반 공연 추천 및 예매 도우미            |
| `ticket-common`         | Shared Module       | 공통 응답, 예외 처리, 유틸리티               |

---

# 💡 Architectural Decisions

## 1. MVC와 WebFlux의 전략적 하이브리드 채택

* `Gateway`와 `Order` 서비스는 **Spring WebFlux(Netty)** 를 적용하여 적은 스레드 수로 높은 동시성을 처리할 수 있도록 설계했습니다.
* Redis, Kafka 등 I/O 중심 작업에 논블로킹 방식의 장점을 활용하여 자원 효율성을 높였습니다.
* `Member` 서비스는 트랜잭션의 일관성과 복잡한 비즈니스 로직이 중요하다고 판단하여 **Spring MVC + JPA** 기반으로 구현했습니다.

---

## 2. Redis 기반 좌석 선점 및 캐싱

* Redis Set을 활용하여 좌석 선점 여부를 원자적으로 처리하고, 이미 선점된 좌석 요청을 DB 이전 단계에서 차단했습니다.
* Redis Hash에 좌석 상세 정보를 저장하여 조회성 트래픽 역시 RDB로 전달되지 않도록 구성했습니다.
* 이를 통해 티켓팅 순간 집중되는 읽기 요청으로부터 영속성 계층을 보호할 수 있도록 설계했습니다.

---

## 3. Kafka 기반 Write-Behind 패턴

* 예약 이벤트를 Kafka에 비동기적으로 적재하고, Consumer에서 Batch Insert를 수행하여 사용자 응답 경로에서 DB 쓰기를 분리했습니다.
* `At-Least-Once` 전달 전략을 채택하였으며, DB Unique Constraint와 Bulk Insert 기반의 멱등성을 통해 중복 처리 상황에서도 데이터 정합성을 유지하도록 설계했습니다.
* Producer에는 `acks=all` 설정을 적용하여 메시지 유실 가능성을 낮추고, 성능과 안정성 사이의 Trade-off를 고려했습니다.

---

## 4. Qdrant 기반 RAG AI 도우미

* 공연 정보를 벡터화하여 Qdrant에 저장하고, 코사인 유사도 기반의 시맨틱 검색을 수행했습니다.
* 검색 결과를 Gemini LLM과 결합한 RAG 구조를 통해 공연 추천 및 예매 가이드 기능을 제공했습니다.

---

# 📊 Performance Test

> k6를 활용하여 Redis, Kafka, ACK 설정 도입 전후의 성능 변화를 측정하였습니다.

| Scenario                             | TPS   | Avg Response Time | P95   |
| ------------------------------------ | ----- | ----------------- | ----- |
| WebFlux                              | 추후 추가 | 추후 추가             | 추후 추가 |
| WebFlux + Redis                      | 추후 추가 | 추후 추가             | 추후 추가 |
| WebFlux + Redis + Kafka              | 추후 추가 | 추후 추가             | 추후 추가 |
| WebFlux + Redis + Kafka (`acks=all`) | 추후 추가 | 추후 추가             | 추후 추가 |

---

# 🛠️ Technical Challenges & Troubleshooting

> 구현 과정에서 발생한 주요 문제와 해결 과정을 정리한 문서입니다.

* Redis 기반 좌석 선점 구조 설계
* Kafka Consumer 멱등성 보장
* ACK 설정에 따른 성능/안정성 Trade-off 분석
* k6를 활용한 부하 테스트 및 병목 분석
* RAG 챗봇 컨텍스트 최적화

---

# 🚀 How to Run

## Prerequisites

* Docker
* Docker Compose
* Gemini API Key

---

## Execution

```bash
# Repository Clone
git clone https://github.com/your-username/ticket-fast.git

cd ticket-fast

# Run Services
docker compose up -d --build
```

---

