# 🔌 WebSocket 실시간 통신 학습일지

> **프로젝트**: CatchDrawing — 실시간 협업 화이트보드 + 모니터링 대시보드  
> **기술 스택**: Spring Boot 3.2.5 / STOMP / SockJS / Java 17  
> **작성일**: 2026-02-15

---

## 1. 전체 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────────┐
│                        Browser (Client)                         │
│                                                                 │
│   dashboard.html ◄──── /topic/metrics ◄─── MetricsPushScheduler │
│                  ◄──── /topic/events  ◄─── EventTrackingService │
│                                                                 │
│   whiteboard.html ──── /app/whiteboard/{id}/draw ──────►┐       │
│                   ──── /app/whiteboard/{id}/cursor ─────►│       │
│                   ──── /app/whiteboard/{id}/clear ──────►│       │
│                   ◄─── /topic/whiteboard/{id} ◄──────────┘       │
│                   ◄─── /topic/whiteboard/{id}/cursors            │
└─────────────────────────────────────────────────────────────────┘
                              │
                        SockJS + STOMP
                         Endpoint: /ws
                              │
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Boot Server                          │
│                                                                 │
│  ┌──────────────┐  ┌─────────────────────┐  ┌───────────────┐  │
│  │ WebSocket     │  │ WhiteboardController│  │ MetricsPush   │  │
│  │ Config        │  │ (@MessageMapping)   │  │ Scheduler     │  │
│  └──────────────┘  └────────┬────────────┘  └───────┬───────┘  │
│                              │                       │          │
│                    ┌─────────▼───────────┐  ┌───────▼───────┐  │
│                    │ EventTrackingService │  │ MetricsService│  │
│                    │ (SimpMessagingTemplate)│ │ (JVM Metrics) │  │
│                    └─────────────────────┘  └───────────────┘  │
│                              ▲                                  │
│                    ┌─────────┴───────────┐                      │
│                    │ WebSocketEvent       │                      │
│                    │ Listener             │                      │
│                    │ (Connect/Disconnect) │                      │
│                    └─────────────────────┘                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. WebSocket 설정 — WebSocketConfig

```java
// config/WebSocketConfig.java

@Configuration
@EnableWebSocketMessageBroker  // ① STOMP 메시지 브로커 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");              // ② 구독 경로 prefix
        config.setApplicationDestinationPrefixes("/app");  // ③ 클라이언트→서버 전송 prefix
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")           // ④ WebSocket 연결 endpoint
                .setAllowedOriginPatterns("*") // ⑤ CORS 허용
                .withSockJS();                 // ⑥ SockJS fallback 지원
    }
}
```

### 핵심 개념 정리

| 항목 | 설명 |
|------|------|
| **`/topic`** | SimpleBroker가 관리하는 구독 채널 prefix. 클라이언트가 `subscribe("/topic/xxx")`하면 서버에서 해당 topic으로 보낸 메시지를 수신 |
| **`/app`** | 클라이언트가 서버로 메시지를 **보낼 때** 사용하는 prefix. `/app/whiteboard/main/draw`로 보내면 `@MessageMapping("/whiteboard/{boardId}/draw")`에 도달 |
| **`/ws`** | 최초 WebSocket 핸드셰이크가 일어나는 HTTP endpoint |
| **SockJS** | WebSocket을 지원하지 않는 브라우저를 위한 폴백. long-polling 등으로 자동 대체 |

### 메시지 경로 흐름도

```
클라이언트 send()                서버 처리                    구독자 수신
─────────────────    ──────────────────────    ──────────────────────
/app/whiteboard      @MessageMapping에서       @SendTo로 지정된
  /{boardId}/draw    처리 후 return            /topic/whiteboard/{boardId}
                                               를 구독한 모든 클라이언트
```

**중요 포인트**: `/app` prefix는 `@MessageMapping` 어노테이션에 적힌 경로 앞에 자동으로 붙는다.  
즉, 클라이언트는 `/app/whiteboard/main/draw`로 보내고, 서버는 `@MessageMapping("/whiteboard/{boardId}/draw")`로 받는다.

---

## 3. Controller 계층 — WhiteboardController

```java
// controller/WhiteboardController.java

@Controller
public class WhiteboardController {

    private final EventTrackingService eventTrackingService;

    // ── 그리기 이벤트 ──
    @MessageMapping("/whiteboard/{boardId}/draw")     // 수신 경로 (클라이언트 → 서버)
    @SendTo("/topic/whiteboard/{boardId}")             // 브로드캐스트 경로 (서버 → 모든 구독자)
    public DrawEvent handleDraw(@DestinationVariable String boardId, DrawEvent event) {
        eventTrackingService.onDraw(event.getSenderId(), event.getTool(), boardId);
        return event;  // return된 객체가 @SendTo 경로로 전송됨
    }

    // ── 커서 이동 ──
    @MessageMapping("/whiteboard/{boardId}/cursor")
    @SendTo("/topic/whiteboard/{boardId}/cursors")     // cursors 전용 토픽 (분리)
    public CursorEvent handleCursor(@DestinationVariable String boardId, CursorEvent event) {
        eventTrackingService.onCursorMove(event.getNickname(), boardId);
        return event;
    }

    // ── 전체 지우기 ──
    @MessageMapping("/whiteboard/{boardId}/clear")
    @SendTo("/topic/whiteboard/{boardId}")
    public DrawEvent handleClear(@DestinationVariable String boardId, DrawEvent event) {
        event.setAction("CLEAR");   // action 필드를 서버에서 강제 설정
        eventTrackingService.onClear(event.getSenderId(), boardId);
        return event;
    }
}
```

### 동작 과정 (그리기 예시)

```
1. 사용자 A가 캔버스에 선을 그림
2. JavaScript에서 STOMP send():
     stompClient.send("/app/whiteboard/main/draw", {}, JSON.stringify({
         senderId: "user-abc123",
         tool: "pen",
         color: "#ef4444",
         lineWidth: 3,
         fromX: 0.25, fromY: 0.30,
         toX: 0.28,   toY: 0.35,
         action: "DRAW"
     }));
3. Spring이 JSON → DrawEvent 자동 역직렬화 (Jackson)
4. handleDraw() 실행:
     - eventTrackingService.onDraw() → 이벤트 카운트 + 로그 push
     - return event → @SendTo에 의해 /topic/whiteboard/main 구독자 전원에게 브로드캐스트
5. 사용자 B, C의 브라우저가 구독 콜백에서 수신 → 캔버스에 동일한 선 렌더링
```

### @MessageMapping vs @RequestMapping 차이

| | @RequestMapping (REST) | @MessageMapping (WebSocket) |
|---|---|---|
| **프로토콜** | HTTP 요청/응답 | WebSocket STOMP 프레임 |
| **통신 방향** | 1:1 (요청한 클라이언트에게만 응답) | 1:N (@SendTo로 모든 구독자에게) |
| **연결 방식** | Stateless (매번 새 연결) | Stateful (하나의 연결 유지) |
| **데이터 형식** | JSON body via @RequestBody | STOMP SEND 프레임 body (JSON) |
| **적합한 용도** | CRUD API | 실시간 양방향 통신 |

### @DestinationVariable 이해

```java
@MessageMapping("/whiteboard/{boardId}/draw")
public DrawEvent handleDraw(@DestinationVariable String boardId, DrawEvent event) {
```

REST의 `@PathVariable`과 같은 역할이다. STOMP destination 경로에서 `{boardId}` 부분을 추출한다.  
이를 통해 **보드별로 독립적인 채널**을 운영할 수 있다. (`/topic/whiteboard/main`, `/topic/whiteboard/room2` 등)

---

## 4. Service 계층 — EventTrackingService

### 역할: 이벤트 추적 + 대시보드 실시간 로그 Push

```java
// service/EventTrackingService.java

@Service
public class EventTrackingService {

    private final SimpMessagingTemplate messagingTemplate;  // ★ 핵심: 서버→클라이언트 push용
    private final AtomicLong totalDrawEvents = new AtomicLong(0);
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final Map<String, Long> lastDrawLogTime = new ConcurrentHashMap<>();
    private static final long DRAW_LOG_THROTTLE_MS = 3000;  // 3초 쓰로틀링
```

### SimpMessagingTemplate — 서버에서 능동적으로 Push

```java
public void pushEvent(String type, String message, String level) {
    totalMessages.incrementAndGet();
    EventLog log = new EventLog(/* ... */);
    messagingTemplate.convertAndSend("/topic/events", log);  // ★ 서버 → 구독자 직접 전송
}
```

**`@SendTo`와의 차이점:**

| | @SendTo | SimpMessagingTemplate |
|---|---|---|
| **사용 위치** | Controller의 @MessageMapping 메서드 | Service, Scheduler 등 어디서든 |
| **트리거** | 클라이언트 메시지 수신 시 | 서버 자체 판단 (이벤트, 타이머 등) |
| **유연성** | return값만 보낼 수 있음 | 원하는 시점에 원하는 데이터 전송 가능 |

> **배운 점**: Controller의 `@SendTo`는 "요청에 대한 응답 브로드캐스트"이고,  
> `SimpMessagingTemplate`은 "서버가 자발적으로 push"할 때 사용한다.

### 쓰로틀링 구현 (Draw 이벤트)

```java
public void onDraw(String senderId, String tool, String boardId) {
    totalDrawEvents.incrementAndGet();    // 카운트는 항상 증가
    totalMessages.incrementAndGet();
    
    long now = System.currentTimeMillis();
    Long last = lastDrawLogTime.get(senderId);
    
    // 유저별로 3초에 한 번만 로그 push
    if (last == null || now - last >= DRAW_LOG_THROTTLE_MS) {
        lastDrawLogTime.put(senderId, now);
        pushEvent("DRAW", senderId + "님이 보드 #" + boardId + "에서 " + toolName(tool), "INFO");
    }
}
```

**왜 쓰로틀링이 필요한가?**
- 펜 드래그 시 `mousemove` 이벤트가 초당 60회 이상 발생
- 매번 로그를 push하면 대시보드 이벤트 목록이 폭발적으로 쌓임
- `ConcurrentHashMap<senderId, lastTime>`으로 유저별 독립 쓰로틀링 구현
- **메트릭 카운트**는 항상 증가하되, **로그 push**만 제한하는 것이 핵심

### 동시성 처리 도구

| 클래스 | 용도 | 이유 |
|--------|------|------|
| `AtomicLong` | 총 이벤트 카운트 | 여러 WebSocket 스레드에서 동시에 increment해도 안전 |
| `ConcurrentHashMap` | 유저별 마지막 로그 시간 | 동시 접속 유저별 쓰로틀 타임스탬프 관리 |

---

## 5. WebSocket 세션 감지 — WebSocketEventListener

```java
// config/WebSocketEventListener.java

@Component
public class WebSocketEventListener {

    private final Set<String> activeSessions = ConcurrentHashMap.newKeySet();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        activeSessions.add(sessionId);
        eventTrackingService.onUserConnected(sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        activeSessions.remove(sessionId);
        eventTrackingService.onUserDisconnected(sessionId);
    }

    public int getActiveSessionCount() {
        return activeSessions.size();  // MetricsService에서 참조
    }
}
```

### Spring의 WebSocket 이벤트 체계

```
브라우저 new SockJS("/ws")
    │
    ├─ TCP 핸드셰이크
    ├─ STOMP CONNECT 프레임
    │
    ▼
SessionConnectedEvent  ← @EventListener로 감지
    │
    │  ... 통신 중 ...
    │
SessionDisconnectEvent ← @EventListener로 감지
    │
    ▼
TCP 연결 종료
```

**`SessionConnectEvent` vs `SessionConnectedEvent`:**
- `SessionConnectEvent` — CONNECT 프레임을 받은 즉시 (아직 처리 전)
- `SessionConnectedEvent` — CONNECTED 응답까지 완료된 후 (안전하게 사용 가능) ← **이걸 사용**

---

## 6. Scheduler → Service → Client Push 흐름

### MetricsPushScheduler (1초마다 메트릭 Push)

```java
@Component
public class MetricsPushScheduler {

    @Scheduled(fixedRate = 1000)  // 1초마다 실행
    public void pushMetrics() {
        MetricsData metrics = metricsService.generateMetrics();
        messagingTemplate.convertAndSend("/topic/metrics", metrics);
    }
}
```

### MetricsService (실제 JVM 데이터 수집)

```java
public MetricsData generateMetrics() {
    // CPU: OperatingSystemMXBean에서 로드 평균
    // Memory: MemoryMXBean에서 힙 사용량
    // TPS: EventTrackingService의 totalMessages 차이값 (이전 스냅샷 대비)
    // Active Users: WebSocketEventListener.getActiveSessionCount()
    // Draw Events: EventTrackingService.getTotalDrawEvents()
}
```

### 데이터 흐름 다이어그램

```
[JVM ManagementFactory] ──► MetricsService.generateMetrics()
[WebSocketEventListener] ──►        │
[EventTrackingService] ──►          │
                                    ▼
                            MetricsPushScheduler
                            (@Scheduled 1초마다)
                                    │
                     messagingTemplate.convertAndSend()
                                    │
                                    ▼
                          /topic/metrics (STOMP 브로커)
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
              Dashboard A     Dashboard B     Dashboard C
              (구독 중)        (구독 중)        (구독 중)
```

---

## 7. 클라이언트(JS) 측 STOMP 연결 구조

```javascript
// ── WebSocket 연결 ──
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // ── 구독 (Subscribe) ──
    
    // 1) 화이트보드 그리기 이벤트 수신
    stompClient.subscribe(`/topic/whiteboard/${boardId}`, function(msg) {
        const data = JSON.parse(msg.body);
        if (data.action === 'CLEAR') {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
        } else {
            drawLine(data);  // 다른 사용자가 그린 선 렌더링
        }
    });
    
    // 2) 커서 위치 수신
    stompClient.subscribe(`/topic/whiteboard/${boardId}/cursors`, function(msg) {
        updateRemoteCursor(JSON.parse(msg.body));
    });
    
    // 3) 대시보드 메트릭 수신
    stompClient.subscribe('/topic/metrics', function(msg) {
        updateDashboard(JSON.parse(msg.body));
    });
    
    // 4) 실시간 이벤트 로그 수신
    stompClient.subscribe('/topic/events', function(msg) {
        addEventToList(JSON.parse(msg.body));
    });
});

// ── 전송 (Send) ──
stompClient.send(`/app/whiteboard/${boardId}/draw`, {}, JSON.stringify(drawEvent));
```

---

## 8. DTO 구조 정리

### DrawEvent — 그리기/지우기 데이터

```
{
    senderId:  "user-abc123"     // 보낸 사람 고유 ID
    tool:      "pen"             // pen | eraser | rect | circle
    color:     "#ef4444"         // 색상 hex
    lineWidth: 3                 // 선 굵기 (px)
    fromX:     0.25              // 시작점 X (0~1 비율)
    fromY:     0.30              // 시작점 Y (0~1 비율)
    toX:       0.28              // 끝점 X
    toY:       0.35              // 끝점 Y
    action:    "DRAW" | "CLEAR"  // 동작 종류
}
```

> **좌표가 비율(0~1)인 이유**: 사용자마다 화면 해상도가 다르므로, 절대 px 대신 비율로 전송하고 수신 측에서 자기 캔버스 크기에 맞게 변환

### CursorEvent — 커서 위치 데이터

```
{
    senderId:  "user-abc123"
    nickname:  "다람쥐42"        // 화면에 표시할 닉네임
    x:         0.45              // 커서 X 비율
    y:         0.60              // 커서 Y 비율
    color:     "#f472b6"         // 커서 색상
}
```

### EventLog — 대시보드 이벤트 로그

```
{
    id:        "a1b2c3d4"        // UUID 앞 8자리
    type:      "DRAW" | "CONNECT"
    message:   "user-abc님이 보드 #main에서 펜 그리기"
    level:     "INFO" | "WARN"
    timestamp: "14:23:45"
}
```

---

## 9. 통신 채널 분리 설계

```
/topic/whiteboard/{boardId}          ← 그리기 + 지우기 (DrawEvent)
/topic/whiteboard/{boardId}/cursors  ← 커서만 (CursorEvent)
/topic/metrics                       ← 1초 주기 시스템 메트릭
/topic/events                        ← 실시간 이벤트 로그
```

**왜 분리하는가?**
- **그리기 vs 커서**: 커서는 초당 수십 회 발생하지만, 캔버스 렌더링과는 무관. 같은 토픽이면 불필요한 파싱 오버헤드
- **메트릭 vs 이벤트**: 메트릭은 정기적(1초), 이벤트는 비정기(액션 발생 시). 구독자도 다를 수 있음
- **보드별 분리({boardId})**: 향후 다중 보드 확장 시 보드 A의 그림이 보드 B에 영향을 주지 않음

---

## 10. 배운 것 & 핵심 정리

### ✅ STOMP 메시지 흐름 3줄 요약
1. 클라이언트가 `/app/xxx`로 **send** → `@MessageMapping`이 받음
2. Controller가 처리 후 **return** → `@SendTo("/topic/xxx")`로 구독자 전원에게 **broadcast**
3. Service에서 `SimpMessagingTemplate.convertAndSend()`로 **서버 주도 push** 가능

### ✅ Controller는 "라우터", Service는 "두뇌"
- **Controller**: 메시지 수신 → Service 호출 → 결과 브로드캐스트 (얇게 유지)
- **Service**: 비즈니스 로직 (카운팅, 쓰로틀링, 로그 push, 메트릭 수집)

### ✅ 동시성은 반드시 고려
- WebSocket 메시지는 멀티스레드로 처리됨
- 공유 상태에는 `AtomicLong`, `ConcurrentHashMap` 필수

### ✅ @EventListener로 세션 라이프사이클 감지
- `SessionConnectedEvent` / `SessionDisconnectEvent`로 접속/퇴장 추적
- HTTP와 달리 WebSocket은 **연결이 유지**되므로 세션 관리가 중요

### ✅ 쓰로틀링 패턴
```
카운트는 항상 증가 (정확한 메트릭용)
로그 push만 제한 (유저별 시간 기반 쓰로틀)
```
이 패턴은 고빈도 이벤트 처리에서 자주 사용되는 실무 패턴이다.

---

## 11. 프로젝트 파일 구조 참조

```
src/main/java/com/portfolio/monitor/
├── MonitorApplication.java              // Spring Boot 진입점
├── config/
│   ├── AsyncConfig.java                 // 비동기 설정
│   ├── WebSocketConfig.java             // ★ STOMP + SockJS 설정
│   └── WebSocketEventListener.java      // ★ 세션 Connect/Disconnect 감지
├── controller/
│   ├── OrderController.java             // REST API (헬스체크 등)
│   └── WhiteboardController.java        // ★ STOMP 메시지 핸들러
├── dto/
│   ├── DrawEvent.java                   // 그리기 데이터 DTO
│   ├── CursorEvent.java                 // 커서 위치 DTO
│   ├── EventLog.java                    // 이벤트 로그 DTO
│   └── MetricsData.java                 // 시스템 메트릭 DTO
├── scheduler/
│   └── MetricsPushScheduler.java        // ★ 1초 주기 메트릭 push
└── service/
    ├── EventTrackingService.java        // ★ 이벤트 추적 + 로그 push + 쓰로틀링
    ├── MetricsService.java              // ★ JVM/WebSocket 메트릭 수집
    └── OrderService.java                // 기존 주문 서비스
```
