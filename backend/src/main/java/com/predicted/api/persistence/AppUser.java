package com.predicted.api.persistence;

import com.predicted.api.common.Models.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "app_users",
    indexes = {
        @Index(name = "idx_app_users_email", columnList = "email", unique = true)
    }
)
public class AppUser {

  @Id
  @Column(length = 64)
  private String id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, unique = true, length = 180)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(nullable = false, length = 140)
  private String university;

  @Column(nullable = false, length = 140)
  private String program;

  @Column(name = "academic_level", nullable = false, length = 80)
  private String academicLevel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role;

  protected AppUser() {
  }

  public AppUser(
      String id,
      String name,
      String email,
      String passwordHash,
      String university,
      String program,
      String academicLevel,
      UserRole role
  ) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.passwordHash = passwordHash;
    this.university = university;
    this.program = program;
    this.academicLevel = academicLevel;
    this.role = role;
  }

  public UserProfile toProfile() {
    return new UserProfile(id, name, email, university, program, academicLevel, role.name());
  }

  public void updateProfile(String name, String university, String program, String academicLevel) {
    this.name = name;
    this.university = university;
    this.program = program;
    this.academicLevel = academicLevel;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getUniversity() {
    return university;
  }

  public String getProgram() {
    return program;
  }

  public String getAcademicLevel() {
    return academicLevel;
  }

  public UserRole getRole() {
    return role;
  }
}
