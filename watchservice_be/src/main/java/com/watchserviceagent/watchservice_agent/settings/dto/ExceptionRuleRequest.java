package com.watchserviceagent.watchservice_agent.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 예외(화이트리스트) 규칙 생성 요청 DTO.
 *
 * 프론트에서:
 *  POST /settings/exceptions
 *  {
 *    "type": "PATH",
 *    "pattern": "C:\\Users\\user\\SafeFolder",
 *    "memo": "백업 폴더"
 *  }
 */
@Getter
@Setter
public class ExceptionRuleRequest {

    @NotBlank(message = "규칙 타입은 필수입니다.")
    @Pattern(regexp = "PATH|EXT|PROCESS", message = "타입은 PATH, EXT, PROCESS 중 하나여야 합니다.")
    private String type;    // PATH / EXT / PROCESS

    @NotBlank(message = "패턴은 필수입니다.")
    @Size(max = 4096, message = "패턴은 4096자 이하여야 합니다.")
    private String pattern;

    @Size(max = 500, message = "메모는 500자 이하여야 합니다.")
    private String memo;
}
