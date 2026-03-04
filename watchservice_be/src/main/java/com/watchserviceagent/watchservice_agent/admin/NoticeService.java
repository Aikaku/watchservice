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

    public void create(String title, String content) {
        noticeRepository.insert(title, content);
    }

    public List<NoticeDto> getAll() {
        List<Notice> list = noticeRepository.findAllDesc();
        return list.stream().map(NoticeDto::from).toList();
    }

    public int deleteById(long id) {
        return noticeRepository.deleteById(id);
    }
}

