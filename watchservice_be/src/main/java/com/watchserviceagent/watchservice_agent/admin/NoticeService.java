package com.watchserviceagent.watchservice_agent.admin;

import com.watchserviceagent.watchservice_agent.admin.domain.Notice;
import com.watchserviceagent.watchservice_agent.admin.dto.NoticeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 클래스 이름 : NoticeService
 * 기능 : 공지사항 생성/조회/삭제 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    /*
     * 함수 이름 : create
     * 기능 : 새 공지사항을 저장한다.
     * 매개변수 : title - 공지 제목, content - 공지 내용
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public void create(String title, String content) {
        noticeRepository.insert(title, content);
    }

    /*
     * 함수 이름 : getAll
     * 기능 : 모든 공지사항을 최신순으로 조회하여 DTO 목록으로 반환한다.
     * 매개변수 : 없음
     * 반환값 : List<NoticeDto> - 공지사항 DTO 목록
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public List<NoticeDto> getAll() {
        List<Notice> list = noticeRepository.findAllDesc();
        return list.stream().map(NoticeDto::from).toList();
    }

    /*
     * 함수 이름 : deleteById
     * 기능 : 특정 ID의 공지사항을 삭제한다.
     * 매개변수 : id - 삭제할 공지사항 ID
     * 반환값 : int - 삭제된 행 수
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public int deleteById(long id) {
        return noticeRepository.deleteById(id);
    }
}

