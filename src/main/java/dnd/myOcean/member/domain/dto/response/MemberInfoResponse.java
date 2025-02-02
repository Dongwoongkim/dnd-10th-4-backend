package dnd.myOcean.member.domain.dto.response;

import dnd.myOcean.member.domain.Member;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberInfoResponse {

    private Long id;
    private String role;
    private String email;
    private String nickname;
    private String gender;
    private LocalDate birthDay;
    private Integer age;
    
    public static MemberInfoResponse of(final Member member) {
        return new MemberInfoResponse(member.getId(),
                member.getRole().name(),
                member.getEmail(),
                member.getNickName(),
                member.getGender().name(),
                member.getBirthDay(),
                member.getAge());
    }
}
