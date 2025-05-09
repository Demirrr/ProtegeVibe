package org.protege.editor.owl.ui.view.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version; // Import Version
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LLMService {
    private static final String API_KEY = "token-tentris-upb";
    // Using the URL from your working curl command
    private static final String BASE_URL = "http://harebell.cs.upb.de:8501/v1";
    private static final String MODEL = "tentris";
    private static final String COMPLETIONS_ENDPOINT = "/chat/completions";
    private static final String SYSTEM_MESSAGE_CONTENT = "You are a helpful assistant.";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final List<String> conversationHistory;

    public LLMService() {
        // Configure the standard HttpClient to use HTTP/1.1
        this.httpClient = HttpClient.newBuilder()
                .version(Version.HTTP_1_1) // Explicitly set HTTP/1.1
                .build();
        this.objectMapper = new ObjectMapper();
        this.conversationHistory = new ArrayList<>();
    }

    public String sendMessage(String userMessage) {
        try {
            // Add the current user message to the history
            conversationHistory.add(userMessage);

            // Construct the request URL
            URI uri = URI.create(BASE_URL + COMPLETIONS_ENDPOINT);

            // Create the JSON request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", MODEL);

            // Create the messages array with full conversation history
            ArrayNode messages = objectMapper.createArrayNode();

            // Add the system message first
            ObjectNode systemMessageNode = objectMapper.createObjectNode();
            systemMessageNode.put("role", "system");
            systemMessageNode.put("content", SYSTEM_MESSAGE_CONTENT);
            messages.add(systemMessageNode);

            // Add previous conversation history (alternating user and assistant)
            // The history list contains raw strings. We need to format them into role/content objects.
            // Assuming the history alternates user, assistant, user, assistant...
            String currentRole = "user";
            // Start from index 0 to include all history
            for (int i = 0; i < conversationHistory.size(); i++) {
                String message = conversationHistory.get(i);
                ObjectNode messageNode = objectMapper.createObjectNode();
                messageNode.put("role", currentRole);
                messageNode.put("content", message);
                messages.add(messageNode);

                // Switch role for the next message in history
                currentRole = currentRole.equals("user") ? "assistant" : "user";
            }

            // Set the constructed messages array in the request body
            requestBody.set("messages", messages);

            String requestBodyString = requestBody.toString();

            // --- Debugging: Print the request body being sent ---
            System.out.println("Sending request body: " + requestBodyString);
            // ----------------------------------------------------

            // Convert JSON string to bytes using UTF-8
            byte[] requestBodyBytes = requestBodyString.getBytes(StandardCharsets.UTF_8);

            // Create the HttpRequest
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY) // Assuming Bearer token authentication
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBodyBytes)) // Use byte array publisher
                    .build();

            // --- Debugging: Print request headers ---
            System.out.println("Sending request headers:");
            request.headers().map().forEach((k, v) -> System.out.println("  " + k + ": " + v));
            // ------------------------------------------

            // Send the request and get the response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // --- Debugging: Print response status and body ---
            System.out.println("Received status code: " + response.statusCode());
            System.out.println("Received response body: " + response.body());
            // -------------------------------------------------

            // Check for successful response status
            if (response.statusCode() != 200) {
                // Do NOT add error response to conversation history
                return "Error: API returned status code " + response.statusCode() + "\nResponse Body: " + response.body();
            }

            // Parse the JSON response
            JsonNode root = objectMapper.readTree(response.body());

            // Extract the assistant's message content
            // Navigating through the JSON structure: choices -> first element -> message -> content
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                // Do NOT add error response to conversation history
                return "Error: Invalid response format - 'choices' array not found or empty.";
            }

            JsonNode firstChoice = choices.get(0);
            JsonNode messageNode = firstChoice.get("message");
            if (messageNode == null) {
                // Do NOT add error response to conversation history
                return "Error: Invalid response format - 'message' object not found in the first choice.";
            }

            JsonNode contentNode = messageNode.get("content");
            if (contentNode == null || !contentNode.isTextual()) {
                // Do NOT add error response to conversation history
                return "Error: Invalid response format - 'content' not found or not text in the message.";
            }

            String responseContent = contentNode.asText();

            // Add the assistant's response to history
            conversationHistory.add(responseContent);

            return responseContent;

        } catch (Exception e) {
            // Handle various exceptions during the process
            // Do NOT add error response to conversation history
            return "Error: " + e.getMessage();
        }
    }

    public void clearHistory() {
        conversationHistory.clear();
    }

    // Optional: Add a getter for history if needed elsewhere
    public List<String> getConversationHistory() {
        return new ArrayList<>(conversationHistory); // Return a copy to prevent external modification
    }
}
