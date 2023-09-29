# Response_Object_TEST

```java
  package com.keti.aas.idthub.util.response;



import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

@Getter
@ToString
public class Response<T> implements Serializable {

    private static final long serialVersionUID = 362498820763181265L;
    private final String message;  // 결과 메시지
    private final int code;       // 결과 코드
    private final int total;          // 총 응답 데이터 수
    private final T data; // 응답 데이터
    private final HttpStatus status;

    /**
     * 생성자에서 필수적으로 넣어야하는 처리를 해야해서 커스텀
     * @param <T>
     */
    public static class ResponseBuilder<T> {

        private final String message;  // 결과 메시지
        private final int code;  // int 형 코드
        private int total;  // 총 응답 데이터 수
        private T data; // 응답 데이터
        private HttpStatus status;

        private void setStatus(int code){
            try {
                status = HttpStatus.valueOf(code);
            } catch (IllegalArgumentException ignored) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            };
        }

        private ResponseBuilder(String message, int code) {
            this.message = message;
            this.code = code;
            setStatus(code);
        }

        public ResponseBuilder<T> data(T value) {
            data = value;
            return this;
        }

        public ResponseBuilder<T> total(int value) {
            total = value;
            return this;
        }
        public Response<T> build() {
            return new Response<>(this);
        }
    }

    public static <T> ResponseBuilder<T> builder(String message, int code) {
        return new ResponseBuilder<T>(message , code);
    }

    private Response(ResponseBuilder<T> responseBuilder){
        message = responseBuilder.message;
        status = responseBuilder.status;
        total = responseBuilder.total;
        data = responseBuilder.data;
        code = responseBuilder.code;
    }

}

```

공통 Response Entity 를 생성해보았습니다.

이에 따른 Error Handling 을 위해 만든 코드들

```java
package com.keti.aas.idthub.util.exception;

import lombok.Getter;


@Getter
public enum ErrorCode {
    UNKNOWN(500)                // 알 수 없는 오류
    , REQUIRED_PARAM(501)       // 필수 파라메터 필요
    , WRONG_REQUEST(502)        // 잘못된 요청
    , MAXIMUM_FILE_SIZE(503)    // 파일 크기 오류
    , CANNOT_DELETE(504)        // 삭제 불가(외래키)
    , PRECONDITION_FAILED(412)  // 전제조건 실패


    // 토큰 관련 오류
    , INVALID_TOKEN(300)        // 유효하지 않는 토큰
    , EXPIRED_TOKEN(301)        // 만료된 토큰
    , AUTHORIZATION_SERVER(302) // 키클록 인가서버 오류

    // 인증 관련 오류
    , NOT_FOUND_USER(400)       // 사용자를 찾을 수 없음
    , UNAUTHORIZED(401)         // 미인증
    , FORBIDDEN(403)
    , COLUMN_NOTFOUND(505)           // 잘못된 요청
    , ACCESS_DENIED(404)        // 인가 실패

    // 체크 오류
    , RESULT_EXIST(402);  // 요청에 대한 결과 존재

    private final int value;
    ErrorCode(int value) {
        this.value = value;
    }

    public static ErrorCode from(int code) {
        for(ErrorCode err : values()) {
            if(err.getValue() == code) return err;
        }

        return null;
    }

}
```

```java
package com.keti.aas.idthub.util.exception;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * jwt exception 분기처리 enum
 */
@Getter
public enum JwtExceptionEnum {

    INVALID_TOKEN("유효하지 않은 토큰" ,
            ErrorCode.INVALID_TOKEN.getValue() ,
            Arrays.asList("An error occurred while attempting to decode the Jwt: Signed JWT rejected" ,
                            "An error occurred while attempting to decode the Jwt: Malformed payload" ,
                            "An error occurred while attempting to decode the Jwt: Invalid" ,
                            "Bearer token is malformed")),
    EXPIRE_TOKEN("만료된 토큰" ,
            ErrorCode.EXPIRED_TOKEN.getValue() , List.of("An error occurred while attempting to decode the Jwt: Jwt expired")),
    NO_AUTHENTICATION("인증되지 않은 사용자" ,
            ErrorCode.UNAUTHORIZED.getValue() , List.of("Full authentication")),
    ACCESS_DENIED("권한이 없습니다." , ErrorCode.ACCESS_DENIED.getValue()
    , List.of("Access is denied") ),
    DEFAULT( "알 수 없는 오류" ,
            ErrorCode.UNAUTHORIZED.getValue(), List.of("IllegalStateException" , "null"));
    private final String ERROR;
    private final int code;
    private final List<String> ERROR_REASON;

    JwtExceptionEnum(String ERROR, int code, List<String> ERROR_REASON) {
        this.ERROR = ERROR;
        this.code = code;
        this.ERROR_REASON = ERROR_REASON;
    }

    public static JwtExceptionEnum findByErrorReason(String message){
        return Arrays.stream(JwtExceptionEnum.values())
                .filter(exceptionEnum -> hasMessage(exceptionEnum , message))
                .findAny()
                .orElse(JwtExceptionEnum.DEFAULT);
    }

    private static boolean hasMessage(JwtExceptionEnum exceptionEnum , String message){
        return exceptionEnum.ERROR_REASON.stream()
                .anyMatch(message::startsWith);
    }
}
```


```java
package com.keti.aas.idthub.util.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
public class RestException extends Exception{
    private final int code;
    private final String message;
    @Builder
    public RestException(String message , int code) {
        super();
        this.code = code;
        this.message = message;
    }
}
```


```java
package com.keti.aas.idthub.util.exception;

import com.keti.aas.idthub.util.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class RestExceptionAdvisor {

    @ExceptionHandler(RestException.class) @Order(value = Ordered.HIGHEST_PRECEDENCE)
    public Response<?> handleRESTException(RestException e) {
        return Response.builder(e.getMessage() , e.getCode()).build();
    }


    @ExceptionHandler(UsernameNotFoundException.class) @Order(value = Ordered.HIGHEST_PRECEDENCE)
    public Response<?> handleUsernameNotFoundException(UsernameNotFoundException e) {
        return Response.builder(e.getMessage() , ErrorCode.NOT_FOUND_USER.getValue()).build();
    }

    @ExceptionHandler(IllegalStateException.class) @Order(value = Ordered.HIGHEST_PRECEDENCE)
    public Response<?> handleIllegalStateException(IllegalStateException e) {
        return Response.builder(e.getMessage() , ErrorCode.UNKNOWN.getValue()).build();
    }


    @ExceptionHandler(IllegalArgumentException.class) @Order(value = Ordered.HIGHEST_PRECEDENCE)
    public Response<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return Response.builder(e.getMessage() , ErrorCode.REQUIRED_PARAM.getValue()).build();
    }


    @ExceptionHandler(Exception.class) @Order(value = Ordered.HIGHEST_PRECEDENCE)
    public Response<?> handleException(Exception e) {
        log.error("error name = {} " , e.getClass().getSimpleName());
        log.error("error message = {} " , e.getMessage());
        return Response.builder(e.getMessage() , ErrorCode.UNKNOWN.getValue()).build();
    }


}
```
