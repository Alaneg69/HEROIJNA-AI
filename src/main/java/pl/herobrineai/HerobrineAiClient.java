package pl.herobrineai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class HerobrineAiClient {
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build();

    public boolean enabled() {
        return key().isPresent();
    }

    public CompletableFuture<String> ask(String instructions, String input) {
        Optional<String> key = key();
        if (key.isEmpty()) return CompletableFuture.completedFuture("");

        String model = System.getenv().getOrDefault("HEROBRINE_OPENAI_MODEL", "gpt-5");
        String url = System.getenv().getOrDefault("HEROBRINE_OPENAI_URL", "https://api.openai.com/v1/responses");

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("instructions", instructions);
        body.addProperty("input", input);

        JsonArray modalities = new JsonArray();
        modalities.add("text");
        body.add("output_modalities", modalities);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + key.get())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
            .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(response -> {
                if (response.statusCode() / 100 != 2) {
                    throw new IllegalStateException("AI HTTP " + response.statusCode() + ": " + response.body());
                }
                return extractText(response.body());
            });
    }

    private Optional<String> key() {
        String key = System.getenv("HEROBRINE_OPENAI_API_KEY");
        return key == null || key.isBlank() ? Optional.empty() : Optional.of(key.trim());
    }

    private String extractText(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root.has("output_text") && root.get("output_text").isJsonPrimitive()) {
            return root.get("output_text").getAsString();
        }
        if (root.has("output") && root.get("output").isJsonArray()) {
            String found = findOutputText(root.get("output"));
            if (!found.isBlank()) return found;
        }
        return "";
    }

    private String findOutputText(com.google.gson.JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("type") && "output_text".equals(object.get("type").getAsString()) && object.has("text")) {
                return object.get("text").getAsString();
            }
            for (var entry : object.entrySet()) {
                String value = findOutputText(entry.getValue());
                if (!value.isBlank()) return value;
            }
        } else if (element.isJsonArray()) {
            for (var child : element.getAsJsonArray()) {
                String value = findOutputText(child);
                if (!value.isBlank()) return value;
            }
        }
        return "";
    }
}
