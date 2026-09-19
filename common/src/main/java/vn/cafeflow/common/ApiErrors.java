package vn.cafeflow.common;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("message", ex.getReason() == null ? "Yêu cầu không hợp lệ" : ex.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList().toString()));
    }

    @ExceptionHandler({ IllegalArgumentException.class, DataIntegrityViolationException.class })
    ResponseEntity<?> conflict(Exception ex) {
        return ResponseEntity.status(409).body(Map.of("message", "Dữ liệu không hợp lệ hoặc đang được sử dụng."));
    }

    public static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu");
    }
}
