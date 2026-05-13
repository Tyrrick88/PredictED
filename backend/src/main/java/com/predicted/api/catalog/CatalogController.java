package com.predicted.api.catalog;

import com.predicted.api.common.AcademicExpansionService;
import com.predicted.api.common.Models.AcademicPathDetail;
import com.predicted.api.common.Models.AcademicPathSummary;
import com.predicted.api.upload.DownloadedFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

  private final AcademicExpansionService academicExpansionService;

  public CatalogController(AcademicExpansionService academicExpansionService) {
    this.academicExpansionService = academicExpansionService;
  }

  @GetMapping("/academic-paths")
  public List<AcademicPathSummary> academicPaths(
      @RequestParam(required = false) String category,
      @RequestParam(required = false, name = "q") String query
  ) {
    return academicExpansionService.academicCatalog(category, query);
  }

  @GetMapping("/academic-paths/{pathId}")
  public AcademicPathDetail academicPath(@PathVariable String pathId) {
    return academicExpansionService.academicPathDetail(pathId);
  }

  @GetMapping("/resources/{resourceId}/download")
  public ResponseEntity<Resource> downloadResource(@PathVariable Long resourceId) {
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
