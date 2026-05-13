package com.predicted.api.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.predicted.api.common.Models.AiStatus;
import com.predicted.api.common.Models.CoursePrediction;
import com.predicted.api.common.Models.MockQuestion;
import com.predicted.api.common.Models.TopicPrediction;
import com.predicted.api.common.Models.TutorRequest;
import com.predicted.api.common.Models.TutorResponse;
import com.predicted.api.common.Models.UserProfile;
import com.predicted.api.ai.TutorNoteContextService.TutorNoteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AcademicAiService {

  private static final Logger log = LoggerFactory.getLogger(AcademicAiService.class);

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final String provider;
  private final String apiKey;
  private final String model;
  private final String baseUrl;
  private final int timeoutSeconds;

  public AcademicAiService(
      ObjectMapper objectMapper,
      @Value("${predicted.ai.provider:fallback}") String provider,
      @Value("${predicted.ai.openai.api-key:}") String apiKey,
      @Value("${predicted.ai.openai.model:gpt-4o}") String model,
      @Value("${predicted.ai.openai.base-url:https://api.openai.com/v1}") String baseUrl,
      @Value("${predicted.ai.openai.timeout-seconds:25}") int timeoutSeconds
  ) {
    this.objectMapper = objectMapper;
    this.provider = provider;
    this.apiKey = apiKey;
    this.model = model;
    this.baseUrl = trimTrailingSlash(baseUrl);
    this.timeoutSeconds = timeoutSeconds;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(Math.max(3, timeoutSeconds)))
        .build();
  }

  public AiStatus status() {
    boolean enabled = openAiEnabled();
    return new AiStatus(
        provider,
        enabled ? model : "fallback",
        enabled,
        enabled ? "OpenAI Responses API" : "Deterministic local fallback"
    );
  }

  public TutorResponse tutorReply(TutorRequest request, UserProfile student, CoursePrediction course) {
    return tutorReply(request, student, course, List.of());
  }

  public TutorResponse tutorReply(
      TutorRequest request,
      UserProfile student,
      CoursePrediction course,
      List<TutorNoteContext> notes
  ) {
    if (openAiEnabled()) {
      try {
        return openAiTutorReply(request, student, course, notes);
      } catch (RuntimeException exception) {
        log.warn("OpenAI tutor response failed; using fallback. {}", exception.getMessage());
      }
    }
    return fallbackTutorReply(request, notes);
  }

  public List<MockQuestion> generateMockQuestions(UserProfile student, CoursePrediction course) {
    if (openAiEnabled()) {
      try {
        List<MockQuestion> questions = openAiMockQuestions(student, course);
        if (!questions.isEmpty()) {
          return questions;
        }
      } catch (RuntimeException exception) {
        log.warn("OpenAI mock generation failed; using fallback. {}", exception.getMessage());
      }
    }
    return fallbackMockQuestions(course);
  }

  private TutorResponse openAiTutorReply(
      TutorRequest request,
      UserProfile student,
      CoursePrediction course,
      List<TutorNoteContext> notes
  ) {
    String output = callResponsesApi(Map.of(
        "model", model,
        "store", false,
        "max_output_tokens", 900,
        "instructions", tutorInstructions(),
        "input", tutorInput(request, student, course, notes),
        "text", Map.of("format", tutorResponseFormat())
    ));

    JsonNode root = parseJsonObject(output);
    String answer = requiredText(root, "answer");
    List<String> nextSteps = textArray(root.path("nextSteps"), 5);
    List<String> generatedFlashcards = textArray(root.path("generatedFlashcards"), 5);
    if (nextSteps.isEmpty() || generatedFlashcards.isEmpty()) {
      throw new AiServiceException("Tutor response missed required study actions.");
    }
    return new TutorResponse(answer, nextSteps, generatedFlashcards);
  }

  private List<MockQuestion> openAiMockQuestions(UserProfile student, CoursePrediction course) {
    String output = callResponsesApi(Map.of(
        "model", model,
        "store", false,
        "max_output_tokens", 1200,
        "instructions", mockInstructions(),
        "input", mockInput(student, course),
        "text", Map.of("format", mockResponseFormat())
    ));

    JsonNode questions = parseJsonObject(output).path("questions");
    if (!questions.isArray()) {
      throw new AiServiceException("Mock response did not include questions.");
    }

    List<MockQuestion> result = new ArrayList<>();
    int index = 1;
    for (JsonNode question : questions) {
      if (result.size() == 3) {
        break;
      }
      result.add(new MockQuestion(
          index,
          course.courseId(),
          optionalText(question, "topic").orElse(course.topics().get(Math.min(result.size(), course.topics().size() - 1)).topic()),
          requiredText(question, "prompt"),
          question.path("marks").isInt() ? question.path("marks").asInt() : (index == 1 ? 12 : 8),
          optionalText(question, "markingHint").orElse("Show each step clearly.")
      ));
      index++;
    }
    return result;
  }

  private String callResponsesApi(Map<String, Object> payload) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/responses"))
          .timeout(Duration.ofSeconds(Math.max(5, timeoutSeconds)))
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new AiServiceException("OpenAI returned HTTP " + response.statusCode());
      }
      return extractOutputText(response.body())
          .orElseThrow(() -> new AiServiceException("OpenAI response did not include output text."));
    } catch (IOException exception) {
      throw new AiServiceException("OpenAI request could not be serialized or parsed.", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AiServiceException("OpenAI request was interrupted.", exception);
    }
  }

  private Optional<String> extractOutputText(String responseBody) throws IOException {
    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode direct = root.path("output_text");
    if (direct.isTextual() && StringUtils.hasText(direct.asText())) {
      return Optional.of(direct.asText());
    }

    for (JsonNode output : root.path("output")) {
      for (JsonNode content : output.path("content")) {
        JsonNode text = content.path("text");
        if (text.isTextual() && StringUtils.hasText(text.asText())) {
          return Optional.of(text.asText());
        }
      }
    }
    return Optional.empty();
  }

  private TutorResponse fallbackTutorReply(TutorRequest request, List<TutorNoteContext> notes) {
    String prompt = (request.prompt() + "\n" + notes.stream().map(TutorNoteContext::excerpt).collect(Collectors.joining("\n")))
        .toLowerCase();
    String answer;
    List<String> nextSteps;
    List<String> generatedCards;

    if (prompt.contains("vector") || prompt.contains("distributed")) {
      answer = "For vector clocks, state the rule first: each process keeps one counter per process, increments on local/send events, and merges by max on receive before incrementing. In exam answers, finish by comparing two timestamps to show causality or concurrency.";
      nextSteps = List.of("Draw a 3-process event timeline", "Compare two vector timestamps", "Attempt one past-paper causality question");
      generatedCards = List.of("When are two vector-clock events concurrent?", "What happens on receive in vector clocks?");
    } else if (prompt.contains("bayes") || prompt.contains("network")) {
      answer = "Start from the graph structure, identify parents, then apply d-separation before computing probabilities. The highest-scoring answers show both the independence reasoning and the numeric factorization.";
      nextSteps = List.of("List parent nodes", "Mark observed evidence", "Factorize the joint probability");
      generatedCards = List.of("What does d-separation test?", "How is a Bayesian network factorized?");
    } else if (prompt.contains("ll") || prompt.contains("parsing") || prompt.contains("compiler")) {
      answer = "For LL(1), calculate FIRST and FOLLOW sets, then fill the parsing table. If any cell has more than one production, the grammar is not LL(1).";
      nextSteps = List.of("Compute FIRST sets", "Compute FOLLOW sets", "Check parsing-table conflicts");
      generatedCards = List.of("What causes an LL(1) conflict?", "Why does FOLLOW matter for epsilon productions?");
    } else {
      answer = "I would study that with a 20-minute loop: define the concept, solve one lecturer-style example, then turn each mistake into a flashcard.";
      nextSteps = List.of("Pick a course", "Solve one short question", "Review related flashcards");
      generatedCards = List.of("What is the key definition?", "What mistake did I make last time?");
    }

    if (!notes.isEmpty()) {
      String filenames = notes.stream()
          .map(TutorNoteContext::filename)
          .collect(Collectors.joining(", "));
      answer = "Using your uploaded notes from " + filenames + ", " + answer;
      List<String> noteAwareSteps = new ArrayList<>();
      noteAwareSteps.add("Re-read the matching section in " + notes.get(0).filename());
      noteAwareSteps.addAll(nextSteps.stream().limit(2).toList());
      nextSteps = noteAwareSteps;
    }

    return new TutorResponse(answer, nextSteps, generatedCards);
  }

  private List<MockQuestion> fallbackMockQuestions(CoursePrediction course) {
    List<TopicPrediction> topics = course.topics();
    return java.util.stream.IntStream.range(0, Math.min(3, topics.size()))
        .mapToObj(index -> {
          TopicPrediction topic = topics.get(index);
          String prompt = switch (index) {
            case 0 -> "Explain " + topic.topic() + " using a lecturer-style worked example.";
            case 1 -> "Compare two approaches related to " + topic.topic() + " and justify the stronger answer.";
            default -> "Solve a short scenario involving " + topic.topic() + " and show each step.";
          };
          return new MockQuestion(
              index + 1,
              course.courseId(),
              topic.topic(),
              prompt,
              index == 0 ? 12 : 8,
              topic.recommendedAction()
          );
        })
        .toList();
  }

  private boolean openAiEnabled() {
    return "openai".equalsIgnoreCase(provider) && StringUtils.hasText(apiKey);
  }

  private String tutorInstructions() {
    return """
        You are PredictED's academic AI tutor for Kenyan university students.
        Give concise, exam-focused help grounded only in the course context provided.
        When uploaded study notes are provided, treat them as the student's primary source material.
        Prefer explaining from those notes first, and clearly mention when the notes do not cover a requested idea.
        Do not claim certainty about lecturer behavior beyond the supplied predictive topics.
        Return valid JSON matching the provided schema.
        """;
  }

  private String mockInstructions() {
    return """
        You generate lecturer-style revision mock questions for PredictED.
        Use the provided course topics and recommended actions.
        Make questions specific, exam-ready, and answerable without outside resources.
        Return valid JSON matching the provided schema.
        """;
  }

  private String tutorInput(
      TutorRequest request,
      UserProfile student,
      CoursePrediction course,
      List<TutorNoteContext> notes
  ) {
    return """
        Student program: %s
        Academic level: %s
        Course: %s
        Lecturer: %s
        High-yield topics:
        %s

        Uploaded study notes:
        %s

        Student question:
        %s
        """.formatted(
        student.program(),
        student.academicLevel(),
        course.courseName(),
        course.lecturer(),
        topicContext(course),
        noteContext(notes),
        request.prompt()
    );
  }

  private String mockInput(UserProfile student, CoursePrediction course) {
    return """
        Student program: %s
        Academic level: %s
        Course ID: %s
        Course: %s
        Lecturer: %s
        Topics:
        %s
        """.formatted(
        student.program(),
        student.academicLevel(),
        course.courseId(),
        course.courseName(),
        course.lecturer(),
        topicContext(course)
    );
  }

  private String topicContext(CoursePrediction course) {
    return course.topics().stream()
        .map(topic -> "- %s (%s, %d%%): %s".formatted(
            topic.topic(),
            topic.weight(),
            topic.likelihood(),
            topic.recommendedAction()
        ))
        .collect(Collectors.joining("\n"));
  }

  private String noteContext(List<TutorNoteContext> notes) {
    if (notes.isEmpty()) {
      return "No uploaded study notes.";
    }
    return notes.stream()
        .map(note -> """
            File: %s%s
            %s
            """.formatted(
            note.filename(),
            note.truncated() ? " (excerpted)" : "",
            note.excerpt()
        ))
        .collect(Collectors.joining("\n\n"));
  }

  private Map<String, Object> tutorResponseFormat() {
    Map<String, Object> schema = objectSchema(Map.of(
        "answer", Map.of("type", "string"),
        "nextSteps", stringArraySchema(),
        "generatedFlashcards", stringArraySchema()
    ), List.of("answer", "nextSteps", "generatedFlashcards"));
    return jsonSchemaFormat("tutor_response", schema);
  }

  private Map<String, Object> mockResponseFormat() {
    Map<String, Object> questionSchema = objectSchema(Map.of(
        "topic", Map.of("type", "string"),
        "prompt", Map.of("type", "string"),
        "marks", Map.of("type", "integer"),
        "markingHint", Map.of("type", "string")
    ), List.of("topic", "prompt", "marks", "markingHint"));
    Map<String, Object> schema = objectSchema(Map.of(
        "questions", Map.of("type", "array", "items", questionSchema, "minItems", 3, "maxItems", 3)
    ), List.of("questions"));
    return jsonSchemaFormat("mock_questions", schema);
  }

  private Map<String, Object> jsonSchemaFormat(String name, Map<String, Object> schema) {
    return Map.of(
        "type", "json_schema",
        "name", name,
        "strict", true,
        "schema", schema
    );
  }

  private Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("additionalProperties", false);
    schema.put("properties", properties);
    schema.put("required", required);
    return schema;
  }

  private Map<String, Object> stringArraySchema() {
    return Map.of(
        "type", "array",
        "items", Map.of("type", "string"),
        "minItems", 2,
        "maxItems", 5
    );
  }

  private JsonNode parseJsonObject(String output) {
    try {
      String json = output.strip();
      if (json.startsWith("```")) {
        json = json.replaceFirst("(?s)^```(?:json)?\\s*", "").replaceFirst("(?s)\\s*```$", "");
      }
      JsonNode node = objectMapper.readTree(json);
      if (!node.isObject()) {
        throw new AiServiceException("AI output was not a JSON object.");
      }
      return node;
    } catch (IOException exception) {
      throw new AiServiceException("AI output was not valid JSON.", exception);
    }
  }

  private String requiredText(JsonNode node, String field) {
    return optionalText(node, field)
        .orElseThrow(() -> new AiServiceException("AI output missed field: " + field));
  }

  private Optional<String> optionalText(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value.isTextual() && StringUtils.hasText(value.asText())) {
      return Optional.of(value.asText());
    }
    return Optional.empty();
  }

  private List<String> textArray(JsonNode node, int limit) {
    List<String> values = new ArrayList<>();
    if (!node.isArray()) {
      return values;
    }
    for (JsonNode item : node) {
      if (item.isTextual() && StringUtils.hasText(item.asText())) {
        values.add(item.asText());
      }
      if (values.size() == limit) {
        break;
      }
    }
    return values;
  }

  private String trimTrailingSlash(String value) {
    if (!StringUtils.hasText(value)) {
      return "https://api.openai.com/v1";
    }
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  private static class AiServiceException extends RuntimeException {
    AiServiceException(String message) {
      super(message);
    }

    AiServiceException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
