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

/**
 * Azure Function to handle translation requests using Microsoft Translator API.
 */
public class TranslateFunction {
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

        // Handle CORS preflight request
        if (request.getHttpMethod() == HttpMethod.OPTIONS) {
            return request.createResponseBuilder(HttpStatus.OK)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type")
                .build();
        }

        // Extract query parameters
        String text = request.getQueryParameters().get("text");
        String to = request.getQueryParameters().get("to");

        // Validate input
        if (text == null || to == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .header("Access-Control-Allow-Origin", "*")
                .body("Please provide 'text' and 'to' query parameters.")
                .build();
        }

        try {
            // Construct request to Microsoft Translator API
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

            // Parse API response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode apiResponseJson = mapper.readTree(response.body());
            String translatedText = apiResponseJson.get(0).get("translations").get(0).get("text").asText();

            // Build JSON result
            ObjectNode translation = mapper.createObjectNode();
            translation.put("text", translatedText);
            ArrayNode translations = mapper.createArrayNode();
            translations.add(translation);
            ObjectNode result = mapper.createObjectNode();
            result.set("translations", translations);

            String resultJson = mapper.writeValueAsString(result);

            return request.createResponseBuilder(HttpStatus.OK)
                .header("Access-Control-Allow-Origin", "*")
                .header("Content-Type", "application/json")
                .body(resultJson)
                .build();

        } catch (Exception e) {
            context.getLogger().severe("Translation error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Access-Control-Allow-Origin", "*")
                .body("Error during translation: " + e.getMessage())
                .build();
        }
    }
}
