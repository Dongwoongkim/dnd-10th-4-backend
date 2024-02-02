package dnd.myOcean.domain.letter.application;


import dnd.myOcean.domain.letter.domain.Letter;
import dnd.myOcean.domain.letter.dto.request.LetterReplyRequest;
import dnd.myOcean.domain.letter.dto.request.LetterSendRequest;
import dnd.myOcean.domain.letter.dto.response.LetterResponse;
import dnd.myOcean.domain.letter.exception.AccessDeniedLetterException;
import dnd.myOcean.domain.letter.exception.AlreadyReplyExistException;
import dnd.myOcean.domain.letter.exception.RepliedLetterPassException;
import dnd.myOcean.domain.letter.repository.infra.jpa.LetterRepository;
import dnd.myOcean.domain.letter.repository.infra.querydsl.dto.LetterReadCondition;
import dnd.myOcean.domain.letter.repository.infra.querydsl.dto.PagedLettersResponse;
import dnd.myOcean.domain.member.domain.Member;
import dnd.myOcean.domain.member.domain.WorryType;
import dnd.myOcean.domain.member.exception.MemberNotFoundException;
import dnd.myOcean.domain.member.repository.infra.jpa.MemberRepository;
import dnd.myOcean.global.auth.aop.dto.CurrentMemberIdRequest;
import dnd.myOcean.global.exception.UnknownException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LetterService {

    private static final Integer MAX_LETTER = 5;

    private final MemberRepository memberRepository;
    private final LetterRepository letterRepository;

    // 편지 전송 -> 받은 사람들에게 이메일 알림
    @Transactional
    public void send(LetterSendRequest request) {
        Member sender = memberRepository.findById(request.getMemberId())
                .orElseThrow(MemberNotFoundException::new);

        List<Member> receivers = filterReceiver(request, memberRepository, sender);

        if (receivers.isEmpty()) {
            sendLetterWithoutFilterUpToMaxLetter(request, sender);
            return;
        }

        if (!receivers.isEmpty() && receivers.size() < MAX_LETTER) {
            sendLetterUpToReceiversCount(request, receivers, sender);
            return;
        }

        sendLetterUptoMaxCount(receivers, request, sender);
    }

    private List<Member> filterReceiver(LetterSendRequest request, MemberRepository memberRepository, Member sender) {
        if (request.isEqualGender()) {
            return memberRepository.findFilteredAndSameGenderMember(request.getAgeRangeStart(),
                    request.getAgeRangeEnd(),
                    sender.getGender(),
                    sender.getId(),
                    WorryType.from(request.getWorryType())
            );
        }

        return memberRepository.findFilteredMember(
                request.getAgeRangeStart(),
                request.getAgeRangeEnd(),
                request.getMemberId(),
                WorryType.from(request.getWorryType()));
    }

    private void sendLetterWithoutFilterUpToMaxLetter(LetterSendRequest request, Member sender) {
        List<Member> randomReceivers = memberRepository.findRandomMembers(sender.getEmail(), MAX_LETTER);
        List<Letter> letters = createLetters(request, randomReceivers, sender, randomReceivers.size());
        letterRepository.saveAll(letters);
    }

    private void sendLetterUpToReceiversCount(LetterSendRequest request, List<Member> receivers,
                                              Member sender) {
        int letterMaxCount = generateRandomReceiverCount(receivers.size());
        Collections.shuffle(receivers);
        List<Letter> letters = createLetters(request, receivers, sender, letterMaxCount);

        letterRepository.saveAll(letters);
    }

    private void sendLetterUptoMaxCount(List<Member> receivers, LetterSendRequest request,
                                        Member sender) {
        Collections.shuffle(receivers);
        List<Letter> letters = createLetters(request, receivers, sender, MAX_LETTER);

        letterRepository.saveAll(letters);
    }

    private static List<Letter> createLetters(LetterSendRequest request, List<Member> receivers, Member sender,
                                              int letterMaxCount) {
        return IntStream.range(0, letterMaxCount)
                .mapToObj(i -> Letter.createLetter(
                        sender,
                        request.getContent(),
                        receivers.get(i),
                        WorryType.from(request.getWorryType()),
                        String.valueOf(UUID.randomUUID())))
                .collect(Collectors.toList());
    }

    private int generateRandomReceiverCount(Integer maxCount) {
        return new Random().nextInt(maxCount) + 1;
    }

    // 보낸 편지
    // 1. 단건 조회 O
    // 2. 삭제 (실제 삭제 X, 프로퍼티 값 변경) O
    // 3. 전체 페이징 조회(삭제하지 않은 메시지만 페이징)
    @Transactional
    public LetterResponse readSendLetter(CurrentMemberIdRequest request, Long letterId) {
        Letter letter = letterRepository.findByIdAndSenderIdAndIsDeleteBySenderFalse(letterId, request.getMemberId())
                .orElseThrow(AccessDeniedLetterException::new);
        return LetterResponse.toDto(letter);
    }

    @Transactional
    public void deleteSendLetter(CurrentMemberIdRequest request, Long letterId) {
        Letter letter = letterRepository.findByIdAndSenderId(letterId, request.getMemberId())
                .orElseThrow(AccessDeniedLetterException::new);
        letter.deleteBySender();
    }

    public PagedLettersResponse readSendLetters(LetterReadCondition cond) {
        return PagedLettersResponse.of(letterRepository.findAllSendLetter(cond));
    }

    // 받은 편지
    // 1. 단건 조회(프로퍼티 값 변경) O
    // 2. 전체 조회
    // 3. 받은 편지 보관 (프로퍼티 값 변경)
    // 4. 받은 편지에 대한 답장 설정
    // 5. 받은 편지 다른 사람에게 전달
    @Transactional
    public LetterResponse readReceivedLetter(CurrentMemberIdRequest request, Long letterId) {
        Letter letter = letterRepository.findByIdAndReceiverIdAndIsDeleteByReceiverFalse(letterId,
                        request.getMemberId())
                .orElseThrow(AccessDeniedLetterException::new);
        letter.read();

        return LetterResponse.toDto(letter);
    }

    public List<LetterResponse> readReceivedLetters(CurrentMemberIdRequest request) {
        List<Letter> letters = letterRepository.findAllByReceiverIdAndIsDeleteByReceiverFalse(request.getMemberId());

        return letters.stream()
                .map(letter -> LetterResponse.toDto(letter))
                .collect(Collectors.toList());
    }

    @Transactional
    public void storeReceivedLetter(CurrentMemberIdRequest request, Long letterId) {
        Letter letter = letterRepository.findByIdAndReceiverIdAndIsDeleteByReceiverFalse(letterId,
                        request.getMemberId())
                .orElseThrow(AccessDeniedLetterException::new);
        letter.store();
    }

    @Transactional
    public void replyReceivedLetter(LetterReplyRequest request, Long letterId) {
        Letter letter = letterRepository.findByIdAndReceiverIdAndIsDeleteByReceiverFalse(letterId,
                        request.getMemberId())
                .orElseThrow(AccessDeniedLetterException::new);

        if (!letter.getReplyContent().isEmpty()) {
            throw new AlreadyReplyExistException();
        }

        letter.reply(request.getReplyContent());
    }

    @Transactional
    public void passReceivedLetter(CurrentMemberIdRequest request, Long letterId) {
        Letter letter = letterRepository.findByIdAndReceiverIdAndIsDeleteByReceiverFalse(
                        letterId,
                        request.getMemberId())
                .orElseThrow(AccessDeniedLetterException::new);

        if (letter.isHasReplied()) {
            throw new RepliedLetterPassException();
        }

        // 전체 회원 id를 가져온다.
        List<Long> memberIds = getAllMemberIds();

        // 해당 편지를 받은 사람의 id를 가져온다.
        List<Long> receiverIds = letterRepository.findAllByUuid(letter.getUuid())
                .stream()
                .map(l -> l.getReceiver().getId())
                .collect(Collectors.toList());

        // 전체 회원 id에서 해당 편지를 받은 사람과 해당 편지를 보낸 사람 제거
        memberIds.removeAll(receiverIds);
        memberIds.remove(letter.getReceiver().getId());

        Collections.shuffle(memberIds);

        Member newReceiver = memberRepository.findById(memberIds.get(0))
                .orElseThrow(UnknownException::new);

        letter.updateReceiver(newReceiver);
    }

    private List<Long> getAllMemberIds() {
        List<Long> memberIds = memberRepository.findAll()
                .stream()
                .map(Member::getId)
                .collect(Collectors.toList());
        return memberIds;
    }

    // 보관한 편지
    // 1. 단건 조회
    // 2. 전체 페이징 조회
    // 3. 보관한 편지 삭제

    // 답장 받은 편지
    // 1. 전체 조회
    // 2. 단건 조회
}
