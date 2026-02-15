# Scheduler Monitor - 실시간 모니터링 & 협업 화이트보드

Spring Boot + WebSocket(STOMP) 기반 실시간 대시보드 & 협업 화이트보드 프로젝트

## 주요 기능

### 📊 실시간 모니터링 대시보드
- 1초 간격 메트릭 Push (TPS, 응답시간, 동시접속자, CPU/메모리)
- 3~7초 간격 랜덤 이벤트 로그 Push
- Chart.js 실시간 라인차트 & 도넛차트
- 서비스 UP/DOWN 상태 모니터링

### 🎨 실시간 협업 화이트보드
- Canvas 기반 그림판 (펜, 지우개, 사각형, 원)
- 색상 선택 & 굵기 조절
- 실시간 커서 공유 (닉네임 표시)
- 전체 지우기 (서버 경유 동기화)

## 기술 스택
- **Backend**: Spring Boot 3.2, Java 17
- **WebSocket**: STOMP + SockJS
- **Frontend**: Vanilla HTML/JS + Chart.js
- **Build**: Gradle

## 실행 방법

```bash
# 로컬 실행
./gradlew bootRun

# 빌드
./gradlew build

# JAR 실행
java -jar build/libs/scheduler-monitor-0.0.1-SNAPSHOT.jar
```

접속: http://localhost:8080/dashboard.html

## 배포 (Railway)
- `server.port`는 환경변수 `PORT`로 자동 설정
- Nixpacks 자동 빌드 (Dockerfile 불필요)
- Git push 시 자동 배포

## 프로젝트 구조
```
src/main/java/com/portfolio/monitor/
├── MonitorApplication.java
├── config/
│   ├── WebSocketConfig.java
│   └── AsyncConfig.java
├── controller/
│   ├── OrderController.java
│   └── WhiteboardController.java
├── service/
│   ├── OrderService.java
│   └── MetricsService.java
├── scheduler/
│   └── MetricsPushScheduler.java
└── dto/
    ├── MetricsData.java
    ├── EventLog.java
    ├── DrawEvent.java
    ├── CursorEvent.java
    └── OrderRequest.java

src/main/resources/
├── application.yml
└── static/
    ├── dashboard.html
    └── whiteboard.html
```
