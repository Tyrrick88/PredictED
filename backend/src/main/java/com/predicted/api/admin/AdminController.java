package com.predicted.api.admin;

import com.predicted.api.common.AcademicDataService;
import com.predicted.api.common.AcademicExpansionService;
import com.predicted.api.common.Models.AcademicModule;
import com.predicted.api.common.Models.AcademicModuleUpsertRequest;
import com.predicted.api.common.Models.AcademicPathDetail;
import com.predicted.api.common.Models.AcademicPathUpsertRequest;
import com.predicted.api.common.Models.AcademicResource;
import com.predicted.api.common.Models.ModerationItem;
import com.predicted.api.upload.DownloadedFile;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final AcademicDataService academicDataService;
  private final AcademicExpansionService academicExpansionService;

  public AdminController(
      AcademicDataService academicDataService,
      AcademicExpansionService academicExpansionService
  ) {
    this.academicDataService = academicDataService;
    this.academicExpansionService = academicExpansionService;
  }

  @GetMapping("/moderation")
  @PreAuthorize("hasRole('ADMIN')")
  public List<ModerationItem> moderation() {
    return academicDataService.moderationItems();
  }

  @GetMapping("/academic/paths")
  @PreAuthorize("hasRole('ADMIN')")
  public List<AcademicPathDetail> academicPaths(
      @RequestParam(required = false) String category,
      @RequestParam(required = false, name = "q") String query
  ) {
    return academicExpansionService.adminAcademicPaths(category, query);
  }

  @PostMapping("/academic/paths")
  @PreAuthorize("hasRole('ADMIN')")
  public AcademicPathDetail createAcademicPath(@Valid @RequestBody AcademicPathUpsertRequest request) {
    return academicExpansionService.createAcademicPath(request);
  }

  @PutMapping("/academic/paths/{pathId}")
  @PreAuthorize("hasRole('ADMIN')")
  public AcademicPathDetail updateAcademicPath(
      @PathVariable String pathId,
      @Valid @RequestBody AcademicPathUpsertRequest request
  ) {
    return academicExpansionService.updateAcademicPath(pathId, request);
  }

  @DeleteMapping("/academic/paths/{pathId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteAcademicPath(@PathVariable String pathId) {
    academicExpansionService.deleteAcademicPath(pathId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/academic/paths/{pathId}/modules")
  @PreAuthorize("hasRole('ADMIN')")
  public AcademicModule createAcademicModule(
      @PathVariable String pathId,
      @Valid @RequestBody AcademicModuleUpsertRequest request
  ) {
    return academicExpansionService.createAcademicModule(pathId, request);
  }

  @PutMapping("/academic/modules/{moduleId}")
  @PreAuthorize("hasRole('ADMIN')")
  public AcademicModule updateAcademicModule(
      @PathVariable Long moduleId,
      @Valid @RequestBody AcademicModuleUpsertRequest request
  ) {
    return academicExpansionService.updateAcademicModule(moduleId, request);
  }

  @DeleteMapping("/academic/modules/{moduleId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteAcademicModule(@PathVariable Long moduleId) {
    academicExpansionService.deleteAcademicModule(moduleId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/academic/paths/{pathId}/resources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public AcademicResource uploadAcademicResource(
      @PathVariable String pathId,
      @RequestParam(required = false) Long moduleId,
      @RequestParam String title,
      @RequestParam String resourceType,
      @RequestParam(required = false) String description,
      @RequestParam(required = false) String externalUrl,
      @RequestParam(required = false) MultipartFile file,
      java.security.Principal principal
  ) {
    return academicExpansionService.uploadAcademicResource(
        principal.getName(),
        pathId,
        moduleId,
        title,
        resourceType,
        description,
        externalUrl,
        file
    );
  }

  @DeleteMapping("/academic/resources/{resourceId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteAcademicResource(@PathVariable Long resourceId) {
    academicExpansionService.deleteAcademicResource(resourceId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/academic/resources/{resourceId}/download")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Resource> downloadAcademicResource(@PathVariable Long resourceId) {
    DownloadedFile file = academicExpansionService.downloadAcademicResource(resourceId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(
            file.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.contentType()
        ))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
        .contentLength(file.sizeBytes())
        .body(file.resource());
  }
}
