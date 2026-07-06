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
* `Member` 서비스는 상대적으로 트래픽이 적고 CRUD 중심의 업무이므로 JPA의 ORM 기능과 개발 생산성을 활용하기 위해 Spring MVC를 선택했습니다.

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

> k6를 활용하여 아키텍처 개선 전후의 성능 변화를 측정하였습니다.

## 1. 기술 도입 효과 비교 (2,000 Seats)

### Test Environment

* **EC2:** t3.small (2 vCPU / 2GB RAM)
* **Scenario:** 100명 → 300명 → 500명 점진적 증가 (총 1분)
* **Seats:** 2,000

| Scenario                             | TPS         | Avg Response Time | P95    |
| ------------------------------------ | ----------- | ----------------- | ------ |
| WebFlux                              | 467 req/s   | 363 ms            | 716 ms |
| WebFlux + Redis                      | 1,434 req/s | 50 ms             | 130 ms |
| WebFlux + Redis + Kafka              | 1,278 req/s | 69 ms             | 146 ms |

### Summary

* Redis 도입을 통해 읽기 병목을 제거하며 TPS가 약 **3배 증가**하였습니다.
* Kafka 도입 시 안정적인 비동기 처리 구조를 확보하였으며, 소규모(2,000석) 환경에서는 Producer 호출 오버헤드로 인해 Redis 단독 구성보다 TPS가 소폭 감소하였습니다.

---

## 2. 대규모 티켓팅 확장성 검증 (20,000 Seats)

### Test Environment

* **EC2:** t3.small (2 vCPU / 2GB RAM)
* **Scenario:** 100명 → 300명 → 500명 → 1,500명 점진적 증가
* **Seats:** 20,000

| Scenario                             | TPS         | Avg Response Time | P95    |
| ------------------------------------ | ----------- | ----------------- | ------ |
| WebFlux + Redis                      | 1,362 req/s | 523 ms            | 1.21 s |
| WebFlux + Redis + Kafka              | 1,938 req/s | 338 ms            | 556 ms |
| WebFlux + Redis + Kafka (`acks=all`) | 1,604 req/s | 429 ms            | 622 ms |

### Summary

* 대규모 쓰기 부하 환경에서는 Redis만으로는 한계가 존재했습니다.
* Kafka 기반 Write-Behind 패턴 도입 후 TPS가 약 **42% 증가**하였으며, P95 응답 시간 또한 크게 개선되었습니다.
* `acks=all` 적용 시 TPS는 감소했지만, 데이터 유실 방지 및 신뢰성 확보를 위해 최종적으로 해당 설정을 채택하였습니다.


> ⚠️ 본 테스트는 AWS EC2 t3.small(2GB RAM) 단일 인스턴스 환경에서 수행되었으며,
> 더 높은 사양 및 다중 인스턴스 환경에서는 다른 결과가 나타날 수 있습니다.

---

# 🛠️ Technical Challenges & Troubleshooting

> 구현 과정에서 발생한 주요 문제와 해결 과정을 정리한 문서입니다.

* [CAS기반 좌석 선점 구현](https://grup-swoo.tistory.com/29)
* [Redis 활용하여 DB 부하 분산](https://grup-swoo.tistory.com/30)
* [Kafka로 쓰기(write) 병목 해결](https://grup-swoo.tistory.com/31)
* [kafka 안정성 확보](https://grup-swoo.tistory.com/32)
* [프로젝트 전체 회고](https://grup-swoo.tistory.com/33)

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

