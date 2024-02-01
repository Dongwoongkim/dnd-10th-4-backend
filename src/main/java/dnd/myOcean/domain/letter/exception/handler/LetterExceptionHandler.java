package dnd.myOcean.domain.letter.exception.handler;


import dnd.myOcean.domain.letter.exception.AccessDeniedLetterException;
import dnd.myOcean.domain.letter.exception.AlreadyReplyExistException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LetterExceptionHandler {

    @ExceptionHandler(AccessDeniedLetterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity accessDeniedLetterException(AccessDeniedLetterException e) {
        return new ResponseEntity("해당 편지에 접근할 수 없습니다.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AlreadyReplyExistException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity alreadyReplyExistException(AlreadyReplyExistException e) {
        return new ResponseEntity("이미 답장을 완료한 편지입니다.", HttpStatus.BAD_REQUEST);
    }
}

