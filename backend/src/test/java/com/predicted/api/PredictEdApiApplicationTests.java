package com.predicted.api;

import com.predicted.api.auth.AuthRequest;
import com.predicted.api.auth.AuthResponse;
import com.predicted.api.auth.RegisterRequest;
import com.predicted.api.auth.SocialAuthRequest;
import com.predicted.api.common.Models.MpesaPaymentRequest;
import com.predicted.api.common.Models.PredictionInput;
import com.predicted.api.common.Models.TutorRequest;
import com.predicted.api.common.Models.UpdateEnrollmentsRequest;
import com.predicted.api.common.Models.UpdateProfileRequest;
import com.predicted.api.persistence.PaymentAttemptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:predicted-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "predicted.security.rate-limit.auth-per-minute=1000",
        "predicted.security.rate-limit.uploads-per-minute=1000",
        "predicted.security.rate-limit.ai-per-minute=1000",
        "predicted.security.rate-limit.api-per-minute=1000"
    }
)
class PredictEdApiApplicationTests {

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate restTemplate;

  @Autowired
  PaymentAttemptRepository paymentAttemptRepository;

  @Test
  void healthIsPublic() {
    ResponseEntity<Map> response = restTemplate.getForEntity(url("/api/health"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("status", "UP");
    assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.getHeaders().getFirst("Referrer-Policy")).isEqualTo("no-referrer");
    assertThat(response.getHeaders().getFirst("Permissions-Policy")).contains("camera=()");
  }

  @Test
  void corsPreflightAllowsConfiguredFrontendOrigin() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.ORIGIN, "http://localhost:4173");
    headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

    ResponseEntity<String> response = restTemplate.exchange(
        url("/api/dashboard/overview"),
        HttpMethod.OPTIONS,
        new HttpEntity<>(headers),
        String.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:4173");
  }

  @Test
  void studentCanLoginAndReadDashboard() {
    AuthResponse auth = login("alex@predicted.test", "password");

    ResponseEntity<Map> response = restTemplate.exchange(
        url("/api/dashboard/overview"),
        HttpMethod.GET,
        authorized(auth.token()),
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> student = (Map<String, Object>) response.getBody().get("student");
    assertThat(student).containsEntry("name", "Alex Mwangi");
  }

  @Test
  void newStudentCanRegisterAndUseAuthenticatedApi() {
    RegisterRequest request = new RegisterRequest(
        "Nia Kamau",
        "nia.kamau@predicted.test",
        "strongpass123",
        "Kenyatta University",
        "BSc. Software Engineering",
        "Year 2, Semester 1"
    );

    ResponseEntity<AuthResponse> register = restTemplate.postForEntity(
        url("/api/auth/register"),
        request,
        AuthResponse.class
    );

    assertThat(register.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(register.getBody()).isNotNull();
    assertThat(register.getBody().token()).isNotBlank();
    assertThat(register.getBody().user().email()).isEqualTo("nia.kamau@predicted.test");

    ResponseEntity<Map> dashboard = restTemplate.exchange(
        url("/api/dashboard/overview"),
        HttpMethod.GET,
        authorized(register.getBody().token()),
        Map.class
    );

    assertThat(dashboard.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> student = (Map<String, Object>) dashboard.getBody().get("student");
    assertThat(student).containsEntry("name", "Nia Kamau");
    assertThat((Iterable<?>) dashboard.getBody().get("tasks")).hasSize(3);
    assertThat(dashboard.getBody()).containsEntry("flashcardsDue", 3);
  }

  @Test
  void socialAuthProviderStatusIsPublicAndDisabledByDefault() {
    ResponseEntity<Map> providers = restTemplate.getForEntity(
        url("/api/auth/social/providers"),
        Map.class
    );
    ResponseEntity<Map> login = restTemplate.postForEntity(
        url("/api/auth/social"),
        new SocialAuthRequest("google", "not-a-token", null),
        Map.class
    );

    assertThat(providers.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(providers.getBody()).containsEntry("google", false);
    assertThat(providers.getBody()).containsEntry("apple", false);
    assertThat(login.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(login.getBody()).containsEntry("code", "BAD_REQUEST");
    assertThat((String) login.getBody().get("message")).contains("Google sign-in is not configured");
  }

  @Test
  void studentCanUpdateProfileAndCourseEnrollments() {
    RegisterRequest request = new RegisterRequest(
        "Maya Otieno",
        "maya.otieno@predicted.test",
        "strongpass123",
        "Jomo Kenyatta University",
        "BSc. Data Science",
        "Year 1, Semester 2",
        List.of("ai", "data")
    );
    AuthResponse auth = restTemplate.postForEntity(
        url("/api/auth/register"),
        request,
        AuthResponse.class
    ).getBody();

    ResponseEntity<Map> profile = restTemplate.exchange(
        url("/api/profile"),
        HttpMethod.PUT,
        authorized(auth.token(), new UpdateProfileRequest(
            "Maya A. Otieno",
            "Strathmore University",
            "BSc. Informatics",
            "Year 2, Semester 1"
        )),
        Map.class
    );

    ResponseEntity<Map> enrollments = restTemplate.exchange(
        url("/api/profile/courses"),
        HttpMethod.PUT,
        authorized(auth.token(), new UpdateEnrollmentsRequest(List.of("data"))),
        Map.class
    );

    ResponseEntity<Map[]> courses = restTemplate.exchange(
        url("/api/predictions/courses"),
        HttpMethod.GET,
        authorized(auth.token()),
        Map[].class
    );
    ResponseEntity<String> unavailableCourse = restTemplate.exchange(
        url("/api/predictions/ai/simulate"),
        HttpMethod.POST,
        authorized(auth.token(), new PredictionInput(72, 82, 90, 76)),
        String.class
    );

    assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> updatedProfile = (Map<String, Object>) profile.getBody().get("profile");
    assertThat(updatedProfile).containsEntry("name", "Maya A. Otieno");
    assertThat(updatedProfile).containsEntry("university", "Strathmore University");
    assertThat(enrollments.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((Iterable<?>) enrollments.getBody().get("enrolledCourses")).hasSize(1);
    assertThat(courses.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(courses.getBody()).hasSize(1);
    assertThat(courses.getBody()[0]).containsEntry("courseId", "data");
    assertThat(unavailableCourse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void duplicateRegistrationReturnsConflict() {
    RegisterRequest request = new RegisterRequest(
        "Duplicate Alex",
        "alex@predicted.test",
        "strongpass123",
        "University of Nairobi",
        "BSc. Computer Science",
        "Year 3"
    );

    ResponseEntity<Map> response = restTemplate.postForEntity(
        url("/api/auth/register"),
        request,
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).containsEntry("code", "CONFLICT");
  }

  @Test
  void predictionSimulationReturnsCalculatedScore() {
    AuthResponse auth = login("alex@predicted.test", "password");
    PredictionInput input = new PredictionInput(72, 82, 90, 76);

    ResponseEntity<Map> response = restTemplate.exchange(
        url("/api/predictions/distributed/simulate"),
        HttpMethod.POST,
        authorized(auth.token(), input),
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("score", 79);
    assertThat(response.getBody()).containsEntry("grade", "A-");
  }

  @Test
  void aiStatusAndFallbackTutorAreAvailable() {
    AuthResponse auth = login("alex@predicted.test", "password");

    ResponseEntity<Map> status = restTemplate.exchange(
        url("/api/ai/status"),
        HttpMethod.GET,
        authorized(auth.token()),
        Map.class
    );
    ResponseEntity<Map> tutor = restTemplate.exchange(
        url("/api/tutor/messages"),
        HttpMethod.POST,
        authorized(auth.token(), new TutorRequest("Explain vector clocks for my exam", "distributed")),
        Map.class
    );

    assertThat(status.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(status.getBody()).containsEntry("enabled", false);
    assertThat(status.getBody()).containsEntry("model", "fallback");
    assertThat(tutor.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((String) tutor.getBody().get("answer")).contains("vector clocks");
    assertThat((Iterable<?>) tutor.getBody().get("nextSteps")).isNotEmpty();
    assertThat((Iterable<?>) tutor.getBody().get("generatedFlashcards")).isNotEmpty();
  }

  @Test
  void tutorAcceptsMultipartNotesAndUsesThemInFallback() {
    AuthResponse auth = login("alex@predicted.test", "password");
    ByteArrayResource notesFile = new ByteArrayResource("""
        Vector clocks help show causality between distributed events.
        On receive, merge counters by taking the maximum for each position.
        """.getBytes(StandardCharsets.UTF_8)) {
      @Override
      public String getFilename() {
        return "vector-clocks.txt";
      }
    };
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("prompt", "Explain vector clocks for my exam");
    body.add("courseId", "distributed");
    body.add("notes", notesFile);

    ResponseEntity<Map> tutor = restTemplate.exchange(
        url("/api/tutor/messages"),
        HttpMethod.POST,
        authorizedMultipart(auth.token(), body),
        Map.class
    );

    assertThat(tutor.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((String) tutor.getBody().get("answer")).contains("uploaded notes from vector-clocks.txt");
    assertThat(tutor.getBody().get("nextSteps").toString())
        .contains("Re-read the matching section in vector-clocks.txt");
  }

  @Test
  void mockGenerationUsesAiServiceFallback() {
    AuthResponse auth = login("alex@predicted.test", "password");

    ResponseEntity<Map[]> mock = restTemplate.exchange(
        url("/api/predictions/distributed/mock"),
        HttpMethod.POST,
        authorized(auth.token()),
        Map[].class
    );

    assertThat(mock.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(mock.getBody()).hasSize(3);
    assertThat(mock.getBody()[0]).containsEntry("courseId", "distributed");
    assertThat((String) mock.getBody()[0].get("prompt")).contains("lecturer-style");
  }

  @Test
  void studentCanUploadAndDownloadNotePack() {
    AuthResponse auth = login("alex@predicted.test", "password");
    byte[] pdfBytes = "%PDF-1.4\nVector clocks notes\n%%EOF".getBytes(StandardCharsets.UTF_8);
    ByteArrayResource file = new ByteArrayResource(pdfBytes) {
      @Override
      public String getFilename() {
        return "vector-clocks.pdf";
      }
    };
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("title", "Vector Clocks Upload");
    body.add("courseId", "distributed");
    body.add("priceKes", "0");
    body.add("file", file);

    ResponseEntity<Map> upload = restTemplate.exchange(
        url("/api/marketplace/notes"),
        HttpMethod.POST,
        authorizedMultipart(auth.token(), body),
        Map.class
    );

    assertThat(upload.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(upload.getBody()).containsEntry("title", "Vector Clocks Upload");
    assertThat(upload.getBody()).containsEntry("courseId", "distributed");
    assertThat(upload.getBody()).containsEntry("downloadable", true);
    assertThat(upload.getBody()).containsEntry("originalFilename", "vector-clocks.pdf");
    assertThat(((Number) upload.getBody().get("sizeBytes")).longValue()).isEqualTo(pdfBytes.length);

    ResponseEntity<byte[]> download = restTemplate.exchange(
        url((String) upload.getBody().get("downloadUrl")),
        HttpMethod.GET,
        authorized(auth.token()),
        byte[].class
    );
    ResponseEntity<Map[]> notes = restTemplate.exchange(
        url("/api/marketplace/notes"),
        HttpMethod.GET,
        authorized(auth.token()),
        Map[].class
    );

    assertThat(download.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(download.getBody()).isEqualTo(pdfBytes);
    assertThat(download.getHeaders().getContentDisposition().getFilename()).isEqualTo("vector-clocks.pdf");
    assertThat(notes.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(List.of(notes.getBody()))
        .anySatisfy(note -> assertThat(note).containsEntry("id", upload.getBody().get("id")));
  }

  @Test
  void uploadRejectsFileContentThatDoesNotMatchExtension() {
    AuthResponse auth = login("alex@predicted.test", "password");
    ByteArrayResource file = new ByteArrayResource("not really a pdf".getBytes(StandardCharsets.UTF_8)) {
      @Override
      public String getFilename() {
        return "fake.pdf";
      }
    };
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("title", "Fake PDF");
    body.add("courseId", "distributed");
    body.add("priceKes", "0");
    body.add("file", file);

    ResponseEntity<Map> upload = restTemplate.exchange(
        url("/api/marketplace/notes"),
        HttpMethod.POST,
        authorizedMultipart(auth.token(), body),
        Map.class
    );

    assertThat(upload.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(upload.getBody()).containsEntry("code", "BAD_REQUEST");
  }

  @Test
  void adminModerationRequiresAdminRole() {
    AuthResponse student = login("alex@predicted.test", "password");
    ResponseEntity<String> denied = restTemplate.exchange(
        url("/api/admin/moderation"),
        HttpMethod.GET,
        authorized(student.token()),
        String.class
    );

    AuthResponse admin = login("admin@predicted.test", "admin123");
    ResponseEntity<Map[]> allowed = restTemplate.exchange(
        url("/api/admin/moderation"),
        HttpMethod.GET,
        authorized(admin.token()),
        Map[].class
    );

    assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(allowed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(allowed.getBody()).isNotEmpty();
  }

  @Test
  void plannerAndPaymentWritesPersistToDatabase() {
    AuthResponse auth = login("alex@predicted.test", "password");

    ResponseEntity<Map> completed = restTemplate.exchange(
        url("/api/planner/tasks/task_ds_vectors/complete"),
        HttpMethod.POST,
        authorized(auth.token()),
        Map.class
    );

    assertThat(completed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(completed.getBody()).containsEntry("completed", true);

    MpesaPaymentRequest request = new MpesaPaymentRequest("254700000000", 80, "pack_ds_final");
    ResponseEntity<Map> payment = restTemplate.exchange(
        url("/api/payments/mpesa/stk-push"),
        HttpMethod.POST,
        authorized(auth.token(), request),
        Map.class
    );

    assertThat(payment.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(payment.getBody()).containsEntry("status", "QUEUED");
    assertThat(paymentAttemptRepository.existsById((String) payment.getBody().get("checkoutRequestId"))).isTrue();
  }

  @Test
  void academicExpansionCatalogPlannerCoachAndAdminFlowsWork() {
    AuthResponse student = login("alex@predicted.test", "password");

    ResponseEntity<Map[]> catalog = restTemplate.exchange(
        url("/api/catalog/academic-paths"),
        HttpMethod.GET,
        authorized(student.token()),
        Map[].class
    );

    assertThat(catalog.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(catalog.getBody()).isNotEmpty();
    assertThat(catalog.getBody())
        .anySatisfy(item -> assertThat(item).containsEntry("category", "PROFESSIONAL_CERTIFICATION"));

    Map<String, Object> catalogPath = catalog.getBody()[0];
    String catalogPathId = (String) catalogPath.get("id");
    Map<String, Object> plannerPayload = Map.of(
        "institutionName", String.valueOf(catalogPath.get("providerName")),
        "academicPathId", catalogPathId,
        "learningPathTitle", String.valueOf(catalogPath.get("title")),
        "availableStudyHoursPerDay", 4,
        "weakSubjects", List.of("Financial Reporting"),
        "strongSubjects", List.of("Business Law"),
        "preferredStudyTimes", List.of("MORNING", "EVENING"),
        "milestones", List.of(Map.of(
            "type", "EXAM",
            "title", "Section II Exam",
            "subjectName", "Financial Reporting",
            "dueAt", LocalDateTime.now().plusDays(10).withSecond(0).withNano(0).toString(),
            "priority", "CRITICAL"
        )),
        "calendarConnected", true
    );

    ResponseEntity<Map> planner = restTemplate.exchange(
        url("/api/planner/coach"),
        HttpMethod.PUT,
        authorized(student.token(), plannerPayload),
        Map.class
    );

    assertThat(planner.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(planner.getBody()).containsEntry("configured", true);
    assertThat(planner.getBody()).containsEntry("institutionName", String.valueOf(catalogPath.get("providerName")));
    assertThat((Iterable<?>) planner.getBody().get("todaySessions")).isNotEmpty();
    assertThat((Iterable<?>) planner.getBody().get("aiRecommendations")).isNotEmpty();

    AuthResponse admin = login("admin@predicted.test", "admin123");
    Map<String, Object> pathPayload = Map.ofEntries(
        Map.entry("category", "TECH_BOOTCAMP_SHORT_COURSE"),
        Map.entry("title", "Cloud Engineering Launchpad"),
        Map.entry("providerName", "Predict.ed Labs"),
        Map.entry("duration", "16 weeks"),
        Map.entry("description", "Intensive bootcamp for Linux, cloud deployment, monitoring, and job readiness."),
        Map.entry("entryRequirements", "Comfort with basic computing and weekly hands-on labs."),
        Map.entry("structureLabel", "Stage 1 to Stage 4"),
        Map.entry("careerPaths", List.of("Cloud support engineer", "Junior DevOps engineer")),
        Map.entry("difficultyLevel", "INTERMEDIATE"),
        Map.entry("tags", List.of("cloud", "devops", "bootcamp")),
        Map.entry("customTrack", true),
        Map.entry("active", true)
    );

    ResponseEntity<Map> createdPath = restTemplate.exchange(
        url("/api/admin/academic/paths"),
        HttpMethod.POST,
        authorized(admin.token(), pathPayload),
        Map.class
    );

    assertThat(createdPath.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(createdPath.getBody()).containsEntry("title", "Cloud Engineering Launchpad");
    String createdPathId = (String) createdPath.getBody().get("id");

    ResponseEntity<Map> createdModule = restTemplate.exchange(
        url("/api/admin/academic/paths/" + createdPathId + "/modules"),
        HttpMethod.POST,
        authorized(admin.token(), Map.of(
            "title", "Observability Fundamentals",
            "summary", "Logging, metrics, dashboards, and alerting for production systems.",
            "stageType", "STAGE",
            "stageLabel", "Stage 2",
            "displayOrder", 1
        )),
        Map.class
    );

    assertThat(createdModule.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(createdModule.getBody()).containsEntry("title", "Observability Fundamentals");

    MultiValueMap<String, Object> resourceBody = new LinkedMultiValueMap<>();
    resourceBody.add("title", "Bootcamp study checklist");
    resourceBody.add("resourceType", "LINK");
    resourceBody.add("externalUrl", "https://example.com/bootcamp-study-checklist");
    resourceBody.add("description", "Recommended pacing checklist for the bootcamp.");

    ResponseEntity<Map> createdResource = restTemplate.exchange(
        url("/api/admin/academic/paths/" + createdPathId + "/resources"),
        HttpMethod.POST,
        authorizedMultipart(admin.token(), resourceBody),
        Map.class
    );

    assertThat(createdResource.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(createdResource.getBody()).containsEntry("title", "Bootcamp study checklist");

    ResponseEntity<Map[]> adminPaths = restTemplate.exchange(
        url("/api/admin/academic/paths"),
        HttpMethod.GET,
        authorized(admin.token()),
        Map[].class
    );

    assertThat(adminPaths.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(adminPaths.getBody())
        .anySatisfy(item -> assertThat(item).containsEntry("id", createdPathId));
  }

  private AuthResponse login(String email, String password) {
    ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
        url("/api/auth/login"),
        new AuthRequest(email, password),
        AuthResponse.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().token()).isNotBlank();
    return response.getBody();
  }

  private HttpEntity<Void> authorized(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(headers);
  }

  private HttpEntity<Object> authorized(String token, Object body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(body, headers);
  }

  private HttpEntity<MultiValueMap<String, Object>> authorizedMultipart(
      String token,
      MultiValueMap<String, Object> body
  ) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    return new HttpEntity<>(body, headers);
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }
}
