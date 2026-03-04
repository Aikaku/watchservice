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

    public void saveUserFeedback(String email, String content) {
        feedbackRepository.insert(email, content);
    }

    public List<FeedbackDto> getAllForAdmin() {
        List<Feedback> list = feedbackRepository.findAllDesc();
        return list.stream().map(FeedbackDto::from).toList();
    }

    public int deleteById(long id) {
        return feedbackRepository.deleteById(id);
    }
}

