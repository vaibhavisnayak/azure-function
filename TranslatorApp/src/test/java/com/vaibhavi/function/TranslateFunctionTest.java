package com.vaibhavi.function;

import com.microsoft.azure.functions.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TranslateFunctionTest {

    @Test
    public void testTranslateFunction() throws Exception {
        // 1️⃣ Mock the request
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("text", "Hello");
        queryParams.put("to", "fr");
        doReturn(queryParams).when(req).getQueryParameters();

        doReturn(Optional.empty()).when(req).getBody();

        // ✅ 2️⃣ 👉 YOUR doAnswer goes here:
        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));
        // 👆 Put it just like this.

        // 3️⃣ Mock ExecutionContext
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // 4️⃣ Run your function
        final HttpResponseMessage ret = new TranslateFunction().run(req, context);

        // 5️⃣ Assert results
        assertNotNull(ret);
       assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ret.getStatus());

    }
}