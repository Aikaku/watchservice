package com.watchserviceagent.watchservice_agent.admin;

import com.watchserviceagent.watchservice_agent.admin.domain.Feedback;
import com.watchserviceagent.watchservice_agent.admin.dto.FeedbackDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 클래스 이름 : FeedbackService
 * 기능 : 피드백 저장/조회/삭제 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    /*
     * 함수 이름 : saveUserFeedback
     * 기능 : 사용자 피드백을 저장한다.
     * 매개변수 : email - 제출자 이메일, content - 피드백 내용
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public void saveUserFeedback(String email, String content) {
        feedbackRepository.insert(email, content);
    }

    /*
     * 함수 이름 : getAllForAdmin
     * 기능 : 모든 피드백을 최신순으로 조회하여 DTO 목록으로 반환한다.
     * 매개변수 : 없음
     * 반환값 : List<FeedbackDto> - 피드백 DTO 목록
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public List<FeedbackDto> getAllForAdmin() {
        List<Feedback> list = feedbackRepository.findAllDesc();
        return list.stream().map(FeedbackDto::from).toList();
    }

    /*
     * 함수 이름 : deleteById
     * 기능 : 특정 ID의 피드백을 삭제한다.
     * 매개변수 : id - 삭제할 피드백 ID
     * 반환값 : int - 삭제된 행 수
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public int deleteById(long id) {
        return feedbackRepository.deleteById(id);
    }
}

