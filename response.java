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
