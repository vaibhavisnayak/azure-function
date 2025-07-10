package com.vaibhavi.function;

import com.microsoft.azure.functions.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of HttpResponseMessage and Builder for testing purposes.
 */
public class HttpResponseMessageMock implements HttpResponseMessage {

    private final HttpStatusType status;
    private final Object body;
    private final Map<String, String> headers;

    public HttpResponseMessageMock(HttpStatusType status, Object body, Map<String, String> headers) {
        this.status = status;
        this.body = body;
        this.headers = headers != null ? headers : new HashMap<>();
    }

    public HttpResponseMessageMock(HttpStatusType status, Object body) {
        this(status, body, new HashMap<>());
    }

    public HttpResponseMessageMock(HttpStatusType status) {
        this(status, null, new HashMap<>());
    }

    @Override
    public HttpStatusType getStatus() {
        return status;
    }

    @Override
    public String getHeader(String key) {
        return headers.get(key);
    }

    @Override
    public Object getBody() {
        return body;
    }

    /**
     * Builder class for HttpResponseMessageMock.
     */
    public static class HttpResponseMessageBuilderMock implements HttpResponseMessage.Builder {

        private HttpStatusType status;
        private Object body;
        private final Map<String, String> headers = new HashMap<>();

        @Override
        public HttpResponseMessage.Builder status(HttpStatusType status) {
            this.status = status;
            return this;
        }

        @Override
        public HttpResponseMessage.Builder header(String key, String value) {
            headers.put(key, value);
            return this;
        }

        @Override
        public HttpResponseMessage.Builder body(Object body) {
            this.body = body;
            return this;
        }

        @Override
        public HttpResponseMessage build() {
            return new HttpResponseMessageMock(status, body, headers);
        }
    }
}
