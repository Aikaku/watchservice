package com.watchserviceagent.watchservice_agent.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 감시 폴더 생성 요청 DTO.
 *
 * 프론트에서:
 *  POST /settings/folders
 *  {
 *    "name": "문서 폴더",
 *    "path": "C:\\Users\\user\\Documents"
 *  }
 */
@Getter
@Setter
public class WatchedFolderRequest {

    @NotBlank(message = "폴더 이름은 필수입니다.")
    @Size(max = 200, message = "폴더 이름은 200자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "폴더 경로는 필수입니다.")
    @Size(max = 4096, message = "폴더 경로는 4096자 이하여야 합니다.")
    private String path;
}
