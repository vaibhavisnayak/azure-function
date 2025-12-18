package com.vaibhavi.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import java.net.http.*;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class TranslateFunction {

    // 🔴 CRITICAL: This variable is set ONLY when the Azure server starts (Cold Start)
    private static final long CONTAINER_START_TIME = System.currentTimeMillis();

    // Securely read environment variables
    private static final String SUBSCRIPTION_KEY = System.getenv("TRANSLATOR_KEY");
    private static final String ENDPOINT = System.getenv("TRANSLATOR_ENDPOINT");
    private static final String REGION = System.getenv("TRANSLATOR_REGION");

    @FunctionName("TranslateFunction")
    public HttpResponseMessage run(
        @HttpTrigger(
            name = "req",
            route = "TranslateFunction",
            methods = {HttpMethod.GET, HttpMethod.OPTIONS},
            authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context) {

        // ⏱️ START TIMER
        long executionStartTime = System.currentTimeMillis();

        // 📊 METRIC 1: Calculate how old the container is
        // If age < 1000ms, it is a COLD START.
        long containerAge = executionStartTime - CONTAINER_START_TIME;
        context.getLogger().info("METRIC_ContainerAge=" + containerAge);

        // Handle CORS (Standard code)
        if (request.getHttpMethod() == HttpMethod.OPTIONS) {
            return request.createResponseBuilder(HttpStatus.OK)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type")
                .build();
        }

        String text = request.getQueryParameters().get("text");
        String to = request.getQueryParameters().get("to");

        if (text == null || to == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Please provide 'text' and 'to' query parameters.")
                .build();
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            URI uri = URI.create(ENDPOINT + "/translate?api-version=3.0&to=" + to);
            String body = "[{\"Text\":\"" + text + "\"}]";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header("Ocp-Apim-Subscription-Key", SUBSCRIPTION_KEY)
                .header("Ocp-Apim-Subscription-Region", REGION)
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = client.send(httpRequest, BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("Translation API error: " + response.body());
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode apiResponseJson = mapper.readTree(response.body());
            String translatedText = apiResponseJson.get(0).get("translations").get(0).get("text").asText();

            ObjectNode translation = mapper.createObjectNode();
            translation.put("text", translatedText);
            ArrayNode translations = mapper.createArrayNode();
            translations.add(translation);
            ObjectNode result = mapper.createObjectNode();
            result.set("translations", translations);

            // ⏱️ STOP TIMER
            long executionEndTime = System.currentTimeMillis();
            long duration = executionEndTime - executionStartTime;

            // 📊 METRIC 2: Log how long execution took
            context.getLogger().info("METRIC_ExecTime=" + duration);

            return request.createResponseBuilder(HttpStatus.OK)
                .header("Access-Control-Allow-Origin", "*")
                .header("Content-Type", "application/json")
                .body(mapper.writeValueAsString(result))
                .build();

        } catch (Exception e) {
            context.getLogger().severe("Translation error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error during translation: " + e.getMessage())
                .build();
        }
    }
}
