package dnd.myOcean.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportSendRequest {

    @Null
    private Long memberId;

    @NotBlank(message = "신고할 편지 번호를 입력해주세요.")
    private Long letterId;

    @NotBlank(message = "신고유형을 입력해주세요.")
    private String reportType;

    @NotBlank(message = "신고 내용을 입력해주세요.")
    private String reportContent;
}
