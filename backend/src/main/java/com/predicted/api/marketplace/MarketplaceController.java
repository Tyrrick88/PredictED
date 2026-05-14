package com.predicted.api.marketplace;

import com.predicted.api.common.AcademicDataService;
import com.predicted.api.common.Models.NotePack;
import com.predicted.api.upload.DownloadedFile;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

  private final AcademicDataService academicDataService;

  public MarketplaceController(AcademicDataService academicDataService) {
    this.academicDataService = academicDataService;
  }

  @GetMapping("/notes")
  public List<NotePack> notes() {
    return academicDataService.notePacks();
  }

  @PostMapping(value = "/notes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<NotePack> uploadNote(
      Principal principal,
      @RequestParam("file") MultipartFile file,
      @RequestParam("title") String title,
      @RequestParam("courseId") String courseId,
      @RequestParam(value = "priceKes", defaultValue = "0") int priceKes,
      @RequestParam(value = "tag", defaultValue = "Review") String tag
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(academicDataService.uploadNotePack(
        principal.getName(),
        title,
        courseId,
        priceKes,
        tag,
        file
    ));
  }

  @GetMapping("/notes/{noteId}/download")
  public ResponseEntity<Resource> downloadNote(@PathVariable String noteId) {
    DownloadedFile file = academicDataService.downloadNotePack(noteId);
    return ResponseEntity.ok()
        .contentType(contentType(file.contentType()))
        .contentLength(file.sizeBytes())
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
            .filename(file.filename(), StandardCharsets.UTF_8)
            .build()
            .toString())
        .header("X-Content-Type-Options", "nosniff")
        .body(file.resource());
  }

  private MediaType contentType(String value) {
    try {
      return MediaType.parseMediaType(value);
    } catch (RuntimeException exception) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }
}
