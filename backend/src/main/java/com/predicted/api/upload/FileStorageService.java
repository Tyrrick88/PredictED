package com.predicted.api.upload;

import com.predicted.api.common.BadRequestException;
import com.predicted.api.common.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
      ".pdf",
      ".doc",
      ".docx",
      ".ppt",
      ".pptx",
      ".txt",
      ".png",
      ".jpg",
      ".jpeg"
  );

  private final Path uploadRoot;
  private final long maxFileBytes;

  public FileStorageService(
      @Value("${predicted.uploads.root:./data/uploads}") String uploadRoot,
      @Value("${predicted.uploads.max-file-bytes:20971520}") long maxFileBytes
  ) {
    this.uploadRoot = Path.of(uploadRoot).toAbsolutePath().normalize();
    this.maxFileBytes = maxFileBytes;
  }

  public StoredFile storeNote(MultipartFile file, String ownerId) {
    validate(file);
    String originalFilename = safeOriginalFilename(file);
    String extension = extension(originalFilename);
    String contentType = StringUtils.hasText(file.getContentType())
        ? file.getContentType()
        : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    String safeOwner = ownerId.replaceAll("[^A-Za-z0-9_-]", "_");
    String storedName = UUID.randomUUID().toString().replace("-", "") + extension;
    Path relativePath = Path.of("notes", safeOwner, storedName).normalize();
    Path target = uploadRoot.resolve(relativePath).normalize();
    if (!target.startsWith(uploadRoot)) {
      throw new BadRequestException("Upload path is invalid.");
    }

    try {
      Files.createDirectories(target.getParent());
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream inputStream = file.getInputStream();
           DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
        Files.copy(digestInputStream, target, StandardCopyOption.REPLACE_EXISTING);
      }
      return new StoredFile(
          relativePath.toString().replace('\\', '/'),
          originalFilename,
          contentType,
          file.getSize(),
          HexFormat.of().formatHex(digest.digest())
      );
    } catch (IOException exception) {
      throw new BadRequestException("Could not store uploaded file.", exception);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 digest is unavailable.", exception);
    }
  }

  public Resource load(String storagePath) {
    Path file = uploadRoot.resolve(storagePath).normalize();
    if (!file.startsWith(uploadRoot) || !Files.isRegularFile(file)) {
      throw new ResourceNotFoundException("Uploaded file not found.");
    }
    try {
      Resource resource = new UrlResource(file.toUri());
      if (!resource.exists() || !resource.isReadable()) {
        throw new ResourceNotFoundException("Uploaded file is not readable.");
      }
      return resource;
    } catch (MalformedURLException exception) {
      throw new ResourceNotFoundException("Uploaded file path is invalid.");
    }
  }

  private void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("Choose a file to upload.");
    }
    if (file.getSize() > maxFileBytes) {
      throw new BadRequestException("File must be " + maxFileBytes + " bytes or smaller.");
    }
    String originalFilename = safeOriginalFilename(file);
    String extension = extension(originalFilename);
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new BadRequestException("Upload a PDF, Word, PowerPoint, text, or image file.");
    }
  }

  private String safeOriginalFilename(MultipartFile file) {
    String filename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename());
    Path path = Path.of(filename).getFileName();
    String originalFilename = path == null ? "upload" : path.toString().replaceAll("[\\\\/]+", "_");
    if (!StringUtils.hasText(originalFilename) || originalFilename.contains("..")) {
      throw new BadRequestException("Uploaded filename is invalid.");
    }
    return originalFilename;
  }

  private String extension(String filename) {
    String lower = filename.toLowerCase(Locale.ROOT);
    int dot = lower.lastIndexOf('.');
    return dot >= 0 ? lower.substring(dot) : "";
  }
}
