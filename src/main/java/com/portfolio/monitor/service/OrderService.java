package com.portfolio.monitor.service;

import com.portfolio.monitor.dto.EventLog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private final List<String> nicknames = List.of(
            "다람쥐42", "토끼17", "고양이88", "판다33", "여우56",
            "햄스터21", "펭귄09", "곰돌이77", "부엉이64", "강아지15",
            "코알라30", "수달45", "기린12", "돌고래99", "앵무새73"
    );

    private final List<String> boardIds = List.of(
            "main", "design-01", "meeting-A", "brainstorm", "sketch-02",
            "team-alpha", "project-X", "review-03"
    );

    public EventLog generateRandomEvent() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String nick = nicknames.get(rng.nextInt(nicknames.size()));
        String board = boardIds.get(rng.nextInt(boardIds.size()));
        int num = rng.nextInt(10, 500);

        List<Object[]> events = List.of(
                new Object[]{"CONNECT", nick + "님이 보드 #" + board + "에 접속했습니다", "INFO"},
                new Object[]{"CONNECT", nick + "님이 화이트보드에서 퇴장했습니다", "INFO"},
                new Object[]{"CONNECT", "동시접속자 " + num + "명 돌파 - 보드 #" + board, "SUCCESS"},
                new Object[]{"DRAW", nick + "님이 보드 #" + board + "에서 그리기 시작", "INFO"},
                new Object[]{"DRAW", nick + "님이 캔버스에 도형 " + num + "개 추가", "INFO"},
                new Object[]{"DRAW", "보드 #" + board + " 전체 지우기 실행 - " + nick + "님", "WARN"},
                new Object[]{"BOARD", "새 보드 생성 - #" + board + " (" + nick + "님)", "SUCCESS"},
                new Object[]{"BOARD", "보드 #" + board + " 자동 저장 완료", "SUCCESS"},
                new Object[]{"BOARD", "보드 #" + board + " 공유 링크 생성됨", "INFO"},
                new Object[]{"SYSTEM", "WebSocket 연결 수: " + num + "개", "INFO"},
                new Object[]{"SYSTEM", "메모리 사용량 경고 - " + (num % 100) + "%", "WARN"},
                new Object[]{"SYSTEM", "메시지 브로커 지연 감지 - " + num + "ms", "WARN"},
                new Object[]{"CURSOR", nick + "님 커서 위치 동기화 - 보드 #" + board, "INFO"},
                new Object[]{"ERROR", nick + "님 연결 끊김 - 재접속 시도 중", "WARN"},
                new Object[]{"ERROR", "보드 #" + board + " 동기화 실패 - 재시도 " + (num % 5 + 1) + "회", "WARN"}
        );

        Object[] picked = events.get(rng.nextInt(events.size()));
        String type = (String) picked[0];
        String message = (String) picked[1];
        String level = (String) picked[2];

        return new EventLog(
                UUID.randomUUID().toString().substring(0, 8),
                type,
                message,
                level,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        );
    }
}
