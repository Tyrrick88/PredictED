package com.predicted.api.ai;

import com.predicted.api.common.BadRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TutorNoteContextService {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".ppt", ".pptx", ".txt");

  private final int maxFiles;
  private final long maxFileBytes;
  private final int maxCharactersPerFile;
  private final int maxTotalCharacters;

  public TutorNoteContextService(
      @Value("${predicted.tutor.notes.max-files:3}") int maxFiles,
      @Value("${predicted.tutor.notes.max-file-bytes:8388608}") long maxFileBytes,
      @Value("${predicted.tutor.notes.max-characters-per-file:4000}") int maxCharactersPerFile,
      @Value("${predicted.tutor.notes.max-total-characters:10000}") int maxTotalCharacters
  ) {
    this.maxFiles = maxFiles;
    this.maxFileBytes = maxFileBytes;
    this.maxCharactersPerFile = maxCharactersPerFile;
    this.maxTotalCharacters = maxTotalCharacters;
  }

  public List<TutorNoteContext> extract(List<MultipartFile> files) {
    if (files == null || files.isEmpty()) {
      return List.of();
    }

    List<MultipartFile> selected = files.stream()
        .filter(file -> file != null && !file.isEmpty())
        .toList();
    if (selected.isEmpty()) {
      return List.of();
    }
    if (selected.size() > maxFiles) {
      throw new BadRequestException("Upload up to " + maxFiles + " notes at a time.");
    }

    List<TutorNoteContext> notes = new ArrayList<>();
    int remainingCharacters = maxTotalCharacters;
    for (MultipartFile file : selected) {
      validate(file);
      String filename = safeOriginalFilename(file);
      String normalized = normalize(extractText(file, extension(filename)));
      if (!StringUtils.hasText(normalized)) {
        throw new BadRequestException(filename + " does not contain readable text.");
      }

      int excerptLimit = Math.min(maxCharactersPerFile, Math.max(remainingCharacters, 0));
      if (excerptLimit <= 0) {
        break;
      }
      boolean truncated = normalized.length() > excerptLimit;
      String excerpt = truncated ? normalized.substring(0, excerptLimit).trim() : normalized;
      notes.add(new TutorNoteContext(filename, excerpt, truncated));
      remainingCharacters -= excerpt.length();
    }
    return notes;
  }

  private void validate(MultipartFile file) {
    if (file.getSize() > maxFileBytes) {
      throw new BadRequestException("Tutor note files must be " + maxFileBytes + " bytes or smaller.");
    }
    String filename = safeOriginalFilename(file);
    if (!ALLOWED_EXTENSIONS.contains(extension(filename))) {
      throw new BadRequestException("Upload tutor notes as PDF, PPT, PPTX, or TXT files.");
    }
  }

  private String extractText(MultipartFile file, String extension) {
    try {
      return switch (extension) {
        case ".pdf" -> extractPdf(file);
        case ".ppt" -> extractPpt(file);
        case ".pptx" -> extractPptx(file);
        case ".txt" -> new String(file.getBytes(), StandardCharsets.UTF_8);
        default -> throw new BadRequestException("Unsupported tutor note format.");
      };
    } catch (IOException exception) {
      throw new BadRequestException("Could not read " + safeOriginalFilename(file) + ".", exception);
    }
  }

  private String extractPdf(MultipartFile file) throws IOException {
    try (PDDocument document = Loader.loadPDF(file.getBytes())) {
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);
      return stripper.getText(document);
    }
  }

  private String extractPpt(MultipartFile file) throws IOException {
    try (HSLFSlideShow slideShow = new HSLFSlideShow(file.getInputStream());
         SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(slideShow)) {
      return extractor.getText();
    }
  }

  private String extractPptx(MultipartFile file) throws IOException {
    try (XMLSlideShow slideShow = new XMLSlideShow(file.getInputStream());
         SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(slideShow)) {
      return extractor.getText();
    }
  }

  private String normalize(String value) {
    return value
        .replace("\u0000", " ")
        .replace("\r", "\n")
        .replaceAll("[\\t\\x0B\\f]+", " ")
        .replaceAll("(?m)[ ]{2,}", " ")
        .replaceAll("\\n{3,}", "\n\n")
        .trim();
  }

  private String safeOriginalFilename(MultipartFile file) {
    String filename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename());
    Path path = Path.of(filename).getFileName();
    String sanitized = path == null ? "upload" : path.toString().replaceAll("[\\\\/]+", "_");
    if (!StringUtils.hasText(sanitized) || sanitized.contains("..")) {
      throw new BadRequestException("Uploaded tutor note filename is invalid.");
    }
    return sanitized;
  }

  private String extension(String filename) {
    String lower = filename.toLowerCase(Locale.ROOT);
    int dot = lower.lastIndexOf('.');
    return dot >= 0 ? lower.substring(dot) : "";
  }

  public record TutorNoteContext(String filename, String excerpt, boolean truncated) {
  }
}
