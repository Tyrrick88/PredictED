package com.predicted.api;

import com.predicted.api.auth.AuthRequest;
import com.predicted.api.auth.AuthResponse;
import com.predicted.api.common.Models.PredictionInput;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PredictEdApiApplicationTests {

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate restTemplate;

  @Test
  void healthIsPublic() {
    ResponseEntity<Map> response = restTemplate.getForEntity(url("/api/health"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("status", "UP");
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

  private String url(String path) {
    return "http://localhost:" + port + path;
  }
}
