package com.watchserviceagent.watchservice_agent.scan.domain;

import lombok.Getter;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 클래스 이름 : ScanJob
 * 기능 : 즉시 검사 작업의 상태(진행률, 현재 경로, 중단 여부 등)를 관리하는 도메인 객체.
 * 작성 날짜 : 2026/03/04
 * 작성자 : 이상혁
 */
@Getter
public class ScanJob {

    public enum Status {
        RUNNING,
        PAUSED,   // 사용자가 중지(일시중지 버튼) 누른 상태(= 스캔 종료)
        DONE,
        ERROR
    }

    private final String scanId;
    private final List<String> roots;

    private final AtomicLong scanned = new AtomicLong(0);
    private final AtomicLong total = new AtomicLong(0);

    private volatile String currentPath;
    private volatile Status status = Status.RUNNING;
    private volatile String message;

    // "pause"를 실제로는 안전하게 스캔을 멈추는(stop) 용도로 처리
    private volatile boolean stopRequested = false;

    /*
     * 함수 이름 : ScanJob
     * 기능 : ScanJob 생성자. scanId와 루트 경로 목록을 초기화한다.
     * 매개변수 : String scanId - 스캔 ID, List<String> roots - 스캔 루트 경로 목록
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public ScanJob(String scanId, List<String> roots) {
        this.scanId = scanId;
        this.roots = roots;
        this.message = "RUNNING";
    }

    /*
     * 함수 이름 : setTotal
     * 기능 : 스캔할 전체 파일 수를 설정한다.
     * 매개변수 : long v - 전체 파일 수
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public void setTotal(long v) {
        total.set(Math.max(0, v));
    }

    /*
     * 함수 이름 : incScanned
     * 기능 : 스캔 완료 파일 수를 1 증가시킨다.
     * 매개변수 : 없음
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public void incScanned() {
        scanned.incrementAndGet();
    }

    /*
     * 함수 이름 : getPercent
     * 기능 : 현재 스캔 진행률을 0~100 사이의 정수로 반환한다.
     * 매개변수 : 없음
     * 반환값 : int - 진행률(%)
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public int getPercent() {
        long t = total.get();
        if (t <= 0) return 0;
        long s = scanned.get();
        long pct = (s * 100L) / t;
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        return (int) pct;
    }

    /*
     * 함수 이름 : setCurrentPath
     * 기능 : 현재 스캔 중인 파일 경로를 갱신한다.
     * 매개변수 : String p - 현재 파일 경로 (스캔 완료 후 null)
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public void setCurrentPath(String p) {
        this.currentPath = p;
    }

    /*
     * 함수 이름 : pause
     * 기능 : 스캔 중단을 요청하고 상태를 PAUSED로 변경한다.
     * 매개변수 : 없음
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public void pause() {
        this.stopRequested = true;
        this.status = Status.PAUSED;
        this.message = "PAUSED_BY_USER";
    }

    /*
     * 함수 이름 : isStopRequested
     * 기능 : 스캔 중단 요청 여부를 반환한다.
     * 매개변수 : 없음
     * 반환값 : boolean - 중단 요청이면 true
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public boolean isStopRequested() {
        return stopRequested;
    }

    /*
     * 함수 이름 : done
     * 기능 : 스캔이 정상 완료되면 상태를 DONE으로 변경한다.
     * 매개변수 : 없음
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public void done() {
        this.status = Status.DONE;
        this.message = "DONE";
    }

    /*
     * 함수 이름 : error
     * 기능 : 스캔 중 오류 발생 시 상태를 ERROR로 변경하고 오류 메시지를 저장한다.
     * 매개변수 : String msg - 오류 메시지
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public void error(String msg) {
        this.status = Status.ERROR;
        this.message = (msg == null || msg.isBlank()) ? "ERROR" : msg;
    }
}
