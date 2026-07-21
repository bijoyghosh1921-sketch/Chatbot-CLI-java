import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.Scanner;

public class chatbot {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME = "llama3.2"; // Change to mistral, phi3, etc.
    private static final Random random = new Random();

    /**
     * Sends a prompt to local Ollama API and retrieves the generated response text.
     */
    public static String queryOllama(String prompt) {
        try {
            // Escaping quotes inside the prompt string for valid JSON formatting
            String cleanPrompt = prompt.replace("\"", "\\\"").replace("\n", " ");
            String jsonPayload = String.format(
                "{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false}",
                MODEL_NAME, cleanPrompt
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Extract response value from JSON payload
                String body = response.body();
                int startIndex = body.indexOf("\"response\":\"") + 12;
                int endIndex = body.indexOf("\",\"done\":");
                if (startIndex > 11 && endIndex > startIndex) {
                    return body.substring(startIndex, endIndex)
                               .replace("\\n", "\n")
                               .replace("\\\"", "\"");
                }
            }
        } catch (Exception e) {
            return "[Error connecting to Ollama: " + e.getMessage() + "]";
        }
        return "[System Error: Unable to parse Ollama response]";
    }

    /**
     * Streams text out letter-by-letter with dynamic pauses to mirror a chat interface.
     */
    public static void streamTyping(String text, int baseDelay) {
        char[] characters = text.toCharArray();

        for (char c : characters) {
            System.out.print(c);
            System.out.flush();

            int delay = baseDelay + random.nextInt(20);

            if (c == '.' || c == '!' || c == '?' || c==':') {
                delay += 300;
            } else if (c == ',' || c == ';') {
                delay += 120;
            }

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println();
    }

    /**
     * Executes native PowerShell commands to speak text out loud asynchronously.
     */
    public static void speakAsync(String text) {
        new Thread(() -> {
            try {
                // Clean input text for PowerShell compatibility
                String cleanText = text.replaceAll("[^a-zA-Z0-9 .?,!]", "")
                                      .replace("'", "''");
                
                String command = "Add-Type -AssemblyName System.Speech; " +
                                 "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                                 "$synth.Speak('" + cleanText + "');";
                
                new ProcessBuilder("powershell", "-Command", command).start().waitFor();
            } catch (IOException | InterruptedException e) {
                // Ignore background TTS errors to prevent program interrupt
            }
        }).start();
    }

    public static void main(String[] args) {
        Scanner kg = new Scanner(System.in);

        System.out.println("=================================================");
        System.out.println("   JAVA OLLAMA TERMINAL - [Local Core Active]    ");
        System.out.println("=================================================");
        System.out.println("Type your prompt below. Type 'exit' to quit.\n");

        // Main Continuous Execution Loop
        while (true) {
            System.out.print("\nYOU > ");
            String userInput = kg.nextLine().trim();

            if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
                System.out.println("\n[Terminating Core Session... Goodbye!]");
                break;
            }

            if (userInput.isEmpty()) {
                continue;
            }

            System.out.print("AI > ");
            
            // 1. Fetch AI response from local Ollama process
            String aiResponse = queryOllama(userInput);

            // 2. Trigger asynchronous audio output via PowerShell
            speakAsync(aiResponse);

            // 3. Render real-time typing animation to console interface
            streamTyping(aiResponse, 25);
        }

        kg.close();
    }
}
