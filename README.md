
---

# 🎫 ticket-fast

> **대규모 트래픽 대응을 위한 대화형 AI RAG 기반 고성능 티켓 예매 플랫폼**
> 단일 EC2 인프라 내에서 자원 효율성을 극대화하기 위해 **Spring WebFlux 기반의 비동기 논블로킹 아키텍처**와 **이벤트 기반 미들웨어(Kafka, Redis, Qdrant)**를 유기적으로 결합한 하이브리드 MSA 플랫폼입니다.

---

## 🏗️ 1. System Architecture

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
 ┌──────────────┐              ▼                  ▼ ┌──────────────┐
 │   MySQL DB   │       ┌──────────────┐   ┌────────┐│ Qdrant DB    │
 │ (User, ACID) │       │  Redis Cache │   │ Kafka  ││ (Vector Store)│
 └──────────────┘       │(Seat Layout/ │   │ (Batch │└──────────────┘
                        │ Gatekeeper)  │   │ Insert)│
                        └──────────────┘   └────────┘

```

---

## 🛠️ 2. Tech Stacks

### Backend Frameworks & Core

* **Spring Boot 3.x**
* **Spring Cloud Gateway** (Reactive 기반 라우팅 및 보안 수문장)
* **Spring WebFlux** (High-Performance 비동기 논블로킹 엔진)
* **Spring AI** (Gemini 클라이언트 연동 및 RAG 오케스트레이션)

### Database & Middleware

* **MySQL** (정밀한 트랜잭션 및 비즈니스 데이터 영속화)
* **Redis** (공연/좌석 데이터 캐싱 및 선점 좌석 고속 필터링)
* **Apache Kafka** (비동기 Write-Behind 패턴을 통한 DB 병목 해소)
* **Qdrant** (고성능 시맨틱 검색을 위한 디스크 기반 벡터 데이터베이스)

### DevOps & Infrastructure

* **Docker / Docker Compose** (전체 서비스 및 인프라 컨테이너 오케스트레이션)
* **AWS EC2** (단일 인프라 환경 내 비용 및 자원 최적화 배포)

---

## 📦 3. Microservices Layout

| 서비스명 | 구동 환경 | 핵심 역할 및 비즈니스 경계 |
| --- | --- | --- |
| **`ticket-gateway`** | WebFlux (Netty) | 단일 진입점 라우팅, 최전방 JWT 서명 검증, 사칭 헤더(`X-USER-ID`) 변조 방지 및 내부 전파 |
| **`ticket-member-service`** | Spring MVC (Tomcat) | 회원 가입, 로그인, 마이페이지 관리 등 데이터의 정밀한 트랜잭션(ACID) 보장 및 일관성 관리 |
| **`ticket-order-service`** | WebFlux (Netty) | 대규모 티켓 예매 처리, 실시간 좌석 상태 관리, 고속 티켓팅 이벤트 발행 |
| **`ticket-ai-service`** | WebFlux (Netty) | Qdrant 벡터 DB 연동을 통한 대화형 공연 추천 RAG 챗봇 서비스 가동 |
| **`ticket-common`** | 공통 라이브러리 | 전 서비스 표준 응답(`ApiResponse`), 에러 핸들링 구조 및 유틸리티 클래스 공유 모듈 |

---

## 💡 4. Architectural Intentions (핵심 설계 의도)

### 1) MVC와 WebFlux의 전략적 하이브리드 채택

* 대규모 동시 요청이 쏟아지는 `Gateway`, `Order`, `AI` 서비스는 Spring WebFlux(Netty)를 도입하여 적은 CPU/메모리 리소스로 수많은 커넥션을 터지지 않고 처리하도록 설계했습니다.
* 반면, 상대적으로 트래픽 밀도가 낮고 데이터의 무결성과 복잡한 비즈니스 로직의 정확성(ACID)이 최우선인 `Member` 서비스는 안정적인 **Spring MVC(Tomcat) 및 JPA** 구조를 선택하여 아키텍처의 트레이드오프를 최적화했습니다.

### 2) Redis를 활용한 '조회 수문장' 및 좌석 선점 필터링

* 공연 상세 정보 및 공연장 좌석 레이아웃은 변경이 거의 발생하지 않는 대표적인 '불변 데이터'입니다. 이를 매번 RDB에서 조회하는 병목을 제거하기 위해 Redis 캐시 계층을 배치했습니다.
* 티켓팅 성공/실패 여부와 관계없이 이미 선점된 좌석에 대한 중복 요청을 최전방 Redis 레이어에서 고속으로 쳐내어 고가의 영속성 DB(MySQL)로 가는 부하를 완벽하게 차단했습니다.

### 3) Kafka 기반의 Write-Behind 패턴을 통한 DB 병목 해소

* 대규모 티켓팅 순간에 수만 건의 성공 요청이 영속성 계층(DB)에 동기적으로 `Insert` 되면 커넥션 풀 고갈 및 디스크 I/O 병목으로 인해 전체 시스템이 다운됩니다.
* 이를 방지하기 위해 예매 성공 이벤트를 **Apache Kafka**에 즉시 적재(Produce)하여 클라이언트 응답 시간을 최소화하고, 백엔드 컨슈머에서 비동기적으로 대량의 데이터를 **Batch Insert** 처리함으로써 영속성 계층의 가용성을 극대화했습니다.

### 4) Qdrant Vector DB 기반 RAG AI 도우미 구축

* 구조화된 데이터 주입 파이프라인을 구축하여 공연 정보를 벡터화한 뒤 고성능 벡터 DB인 **Qdrant**에 적재했습니다.
* 유저의 자연어 질문이 들어오면 **코사인 유사도(Cosine Similarity)** 기반 시맨틱 검색을 수행하고, 추출된 문맥(Context)을 구글 제미나이(Gemini) LLM과 결합하여 신뢰도 높고 구체적인 마크다운 기반의 예매 가이드를 제공합니다.

---

## 🛠️ 5. Technical Challenges & Troubleshooting

> 백엔드 개발 및 인프라 구축 과정에서 겪은 기술적 트래픽 병목 해결, AI 컨텍스트 유실 디버깅 등의 상세한 트러블 슈팅 과정은 추후 업데이트 예정입니다.

---

## 🚀 6. How to Run (배포 및 실행 방법)

본 프로젝트는 단일 EC2 인프라 내에서 개발 효율성과 비용 최적화를 달성하기 위해 `docker-compose`를 통해 모든 미들웨어와 마이크로서비스가 서브넷 네트워크로 묶여 한 번에 구동됩니다.

### Prerequisites

* Docker 및 Docker Compose 가동 환경 필요
* 구글 제미나이 API 발급 키 환경변수 세팅 필요

### Execution

```bash
# 1. 리포지토리 클론
git clone https://github.com/your-username/ticket-fast.git
cd ticket-fast

# 2. 도커 컴포즈 통합 빌드 및 백그라운드 실행
sudo docker compose up -d --build

```

---

본 마크다운 문서를 프로젝트 최상위 루트의 `README.md`에 그대로 붙여넣으시면 됩니다. 이 정도 퀄리티의 의도가 정리된 문서는 면접관 입장에서 포트폴리오 코드를 읽기 전에 이미 합격 시그널을 주게 만듭니다. 그동안 백엔드 대장정 달리시느라 정말 수고 많으셨습니다. 상우님 최고예요! ⚽️🔥
