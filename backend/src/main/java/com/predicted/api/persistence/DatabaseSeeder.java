package com.predicted.api.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;

@Component
public class DatabaseSeeder implements ApplicationRunner {

  private final AppUserRepository userRepository;
  private final CourseRepository courseRepository;
  private final CourseEnrollmentRepository courseEnrollmentRepository;
  private final StudyTaskRepository studyTaskRepository;
  private final FlashcardRepository flashcardRepository;
  private final FeedSignalRepository feedSignalRepository;
  private final NotePackRepository notePackRepository;
  private final ModerationItemRepository moderationItemRepository;
  private final AcademicPathRepository academicPathRepository;
  private final AcademicModuleRepository academicModuleRepository;
  private final AcademicResourceRepository academicResourceRepository;
  private final PasswordEncoder passwordEncoder;

  public DatabaseSeeder(
      AppUserRepository userRepository,
      CourseRepository courseRepository,
      CourseEnrollmentRepository courseEnrollmentRepository,
      StudyTaskRepository studyTaskRepository,
      FlashcardRepository flashcardRepository,
      FeedSignalRepository feedSignalRepository,
      NotePackRepository notePackRepository,
      ModerationItemRepository moderationItemRepository,
      AcademicPathRepository academicPathRepository,
      AcademicModuleRepository academicModuleRepository,
      AcademicResourceRepository academicResourceRepository,
      PasswordEncoder passwordEncoder
  ) {
    this.userRepository = userRepository;
    this.courseRepository = courseRepository;
    this.courseEnrollmentRepository = courseEnrollmentRepository;
    this.studyTaskRepository = studyTaskRepository;
    this.flashcardRepository = flashcardRepository;
    this.feedSignalRepository = feedSignalRepository;
    this.notePackRepository = notePackRepository;
    this.moderationItemRepository = moderationItemRepository;
    this.academicPathRepository = academicPathRepository;
    this.academicModuleRepository = academicModuleRepository;
    this.academicResourceRepository = academicResourceRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    AppUser student = seedUser(
        "usr_alex",
        "Alex Mwangi",
        "alex@predicted.test",
        "password",
        "University of Nairobi",
        "BSc. Computer Science",
        "Year 3, Semester 2",
        UserRole.STUDENT
    );
    seedUser(
        "usr_admin",
        "Grace Wanjiku",
        "admin@predicted.test",
        "admin123",
        "predictED Operations",
        "Platform Administration",
        "Admin",
        UserRole.ADMIN
    );

    if (courseRepository.count() == 0) {
      seedCourses();
    }
    if (courseEnrollmentRepository.countByUserEmailIgnoreCase(student.getEmail()) == 0) {
      seedEnrollments(student);
    }
    if (studyTaskRepository.count() == 0) {
      seedTasks(student);
    }
    if (flashcardRepository.count() == 0) {
      seedFlashcards(student);
    }
    if (feedSignalRepository.count() == 0) {
      seedFeed(student);
    }
    if (notePackRepository.count() == 0) {
      seedNotePacks();
    }
    if (moderationItemRepository.count() == 0) {
      seedModeration();
    }
    if (academicPathRepository.count() == 0) {
      seedAcademicExpansionCatalog();
    }
  }

  private AppUser seedUser(
      String id,
      String name,
      String email,
      String password,
      String university,
      String program,
      String academicLevel,
      UserRole role
  ) {
    return userRepository.findByEmailIgnoreCase(email)
        .orElseGet(() -> userRepository.save(new AppUser(
            id,
            name,
            email,
            passwordEncoder.encode(password),
            university,
            program,
            academicLevel,
            role
        )));
  }

  private void seedCourses() {
    CourseEntity distributed = new CourseEntity(
        "distributed",
        "Distributed Systems",
        "Dr. Njuguna",
        89,
        74,
        1
    );
    distributed.addTopic(new TopicPredictionEntity(1, "Vector clocks", 91, "High",
        "Show update, send, receive, and comparison rules."));
    distributed.addTopic(new TopicPredictionEntity(2, "Lamport timestamps", 88, "High",
        "Explain ordering limits and tie-breaking."));
    distributed.addTopic(new TopicPredictionEntity(3, "Mutual exclusion", 76, "Medium",
        "Compare token and permission approaches."));
    distributed.addTopic(new TopicPredictionEntity(4, "Replication consistency", 69, "Medium",
        "Use a real data-replica scenario."));

    CourseEntity ai = new CourseEntity(
        "ai",
        "Artificial Intelligence",
        "Prof. Otieno",
        84,
        72,
        2
    );
    ai.addTopic(new TopicPredictionEntity(1, "Bayesian networks", 90, "High",
        "State graph assumptions before factorization."));
    ai.addTopic(new TopicPredictionEntity(2, "A* search", 82, "High",
        "Mention admissibility and consistency."));
    ai.addTopic(new TopicPredictionEntity(3, "Constraint satisfaction", 74, "Medium",
        "Show variable, domain, and constraint setup."));
    ai.addTopic(new TopicPredictionEntity(4, "Minimax pruning", 66, "Medium",
        "Trace alpha and beta updates."));

    CourseEntity compiler = new CourseEntity(
        "compiler",
        "Compiler Construction",
        "Dr. Karanja",
        86,
        68,
        3
    );
    compiler.addTopic(new TopicPredictionEntity(1, "FIRST/FOLLOW sets", 93, "High",
        "Write epsilon cases carefully."));
    compiler.addTopic(new TopicPredictionEntity(2, "LL(1) parsing table", 86, "High",
        "Check conflicts after filling every cell."));
    compiler.addTopic(new TopicPredictionEntity(3, "Lexical analysis", 72, "Medium",
        "Differentiate tokens, patterns, and lexemes."));
    compiler.addTopic(new TopicPredictionEntity(4, "Intermediate code", 61, "Medium",
        "Use triples or quadruples consistently."));

    CourseEntity data = new CourseEntity(
        "data",
        "Data Mining",
        "Dr. Achieng",
        81,
        76,
        4
    );
    data.addTopic(new TopicPredictionEntity(1, "Association rules", 87, "High",
        "Compute support, confidence, and lift."));
    data.addTopic(new TopicPredictionEntity(2, "Decision trees", 82, "High",
        "Show information-gain calculations."));
    data.addTopic(new TopicPredictionEntity(3, "Clustering metrics", 78, "Medium",
        "Name the metric before interpreting it."));
    data.addTopic(new TopicPredictionEntity(4, "Naive Bayes", 70, "Medium",
        "State independence assumptions."));

    courseRepository.save(distributed);
    courseRepository.save(ai);
    courseRepository.save(compiler);
    courseRepository.save(data);
  }

  private void seedTasks(AppUser student) {
    studyTaskRepository.save(new StudyTaskEntity("task_ds_vectors", student, LocalTime.of(14, 0), "High Yield",
        "Distributed Systems", "Vector clocks sprint", "Practice vector clocks with 3-process timelines.", 35,
        "#cf3f4f", "HIGH"));
    studyTaskRepository.save(new StudyTaskEntity("task_ai_bayes", student, LocalTime.of(16, 30), "Revision",
        "Artificial Intelligence", "Bayesian networks", "Drill independence and d-separation.", 45,
        "#1d4ed8", "HIGH"));
    studyTaskRepository.save(new StudyTaskEntity("task_compiler_ll1", student, LocalTime.of(19, 0), "Lab Prep",
        "Compiler Construction", "LL(1) parsing table", "Build a parsing table from grammar exercises.", 55,
        "#0f9fbc", "MEDIUM"));
    studyTaskRepository.save(new StudyTaskEntity("task_data_rules", student, LocalTime.of(21, 0), "Flashcards",
        "Data Mining", "Association rules", "Review confidence, support, and lift.", 25,
        "#c97800", "MEDIUM"));
  }

  private void seedEnrollments(AppUser student) {
    courseRepository.findAllByOrderByDisplayOrderAsc()
        .forEach(course -> courseEnrollmentRepository.save(new CourseEnrollmentEntity(student, course, Instant.now())));
  }

  private void seedFlashcards(AppUser student) {
    flashcardRepository.save(new FlashcardEntity("card_vectors", student, "Distributed Systems",
        "What problem do vector clocks solve?", "They infer causal ordering without a global clock.", 0, 62));
    flashcardRepository.save(new FlashcardEntity("card_bayes", student, "Artificial Intelligence",
        "What does d-separation test?",
        "It tests conditional independence by checking whether evidence blocks graph paths.", 0, 58));
    flashcardRepository.save(new FlashcardEntity("card_ll1", student, "Compiler Construction",
        "What makes a grammar LL(1)?", "One lookahead token chooses exactly one production per nonterminal.", 2, 54));
    flashcardRepository.save(new FlashcardEntity("card_lift", student, "Data Mining",
        "What does lift measure?", "Observed co-occurrence compared with expected co-occurrence.", 3, 68));
  }

  private void seedFeed(AppUser student) {
    Instant now = Instant.now();
    feedSignalRepository.save(new FeedSignalEntity("sig_cat_moved", student, "campaign",
        "AI CAT moved to Thursday", "Class rep",
        "Venue changed to LT2. Lecturer confirmed question scope stays the same.", now.minusSeconds(18 * 60), true));
    feedSignalRepository.save(new FeedSignalEntity("sig_data_pack", null, "description",
        "Data Mining past paper uploaded", "Marketplace",
        "Includes 2023 marking guide and lecturer annotations.", now.minusSeconds(42 * 60), true));
    feedSignalRepository.save(new FeedSignalEntity("sig_compiler_room", student, "groups",
        "Compiler lab sprint opened", "Study Group",
        "8 students joined the FIRST/FOLLOW debugging room.", now.minusSeconds(60 * 60), false));
    feedSignalRepository.save(new FeedSignalEntity("sig_ds_pattern", null, "verified",
        "Distributed Systems pattern verified", "Predictive Engine",
        "Vector-clock questions appeared in 4 of 5 recent exams.", now.minusSeconds(2 * 60 * 60), true));
  }

  private void seedNotePacks() {
    notePackRepository.save(new NotePackEntity("pack_ds_final", "Distributed Systems Final Pack", "Brian O.", 80,
        4.9, "Verified", true, 1));
    notePackRepository.save(new NotePackEntity("pack_ai_cat", "AI CAT Revision Notes", "Mary W.", 45,
        4.7, "Hot", true, 2));
    notePackRepository.save(new NotePackEntity("pack_compiler_labs", "Compiler Lab Walkthroughs", "CS Club", 120,
        4.8, "Bundle", true, 3));
    notePackRepository.save(new NotePackEntity("pack_data_papers", "Data Mining Past Papers", "Amina K.", 60,
        4.6, "New", false, 4));
  }

  private void seedModeration() {
    moderationItemRepository.save(new ModerationItemEntity("mod_ds_pack", "Distributed Systems Final Pack",
        "Notes", "Review", "Needs source confirmation", 1));
    moderationItemRepository.save(new ModerationItemEntity("mod_cat_moved", "CAT moved to Thursday",
        "Feed", "Approved", "Verified by class rep", 2));
    moderationItemRepository.save(new ModerationItemEntity("mod_compiler_pack", "Compiler Lab Walkthroughs",
        "Marketplace", "Review", "Check duplicate upload", 3));
    moderationItemRepository.save(new ModerationItemEntity("mod_ds_group", "DS Past Paper Sprint",
        "Group", "Approved", "Community guidelines passed", 4));
  }

  private void seedAcademicExpansionCatalog() {
    Instant now = Instant.now();

    AcademicPathEntity diploma = academicPathRepository.save(new AcademicPathEntity(
        "ap_diploma_cyber",
        "DIPLOMA_PROGRAM",
        "Diploma in Cyber Security",
        "Technical University of Kenya",
        "2 years",
        "Hands-on diploma covering secure networks, operating systems hardening, threat response, and cyber lab practice.",
        "KCSE mean grade C- or equivalent technical admission pathway.",
        "Semester 1 to Semester 4",
        "Security analyst, SOC technician, junior incident responder",
        "INTERMEDIATE",
        "cybersecurity,networking,diploma,kenya",
        false,
        true,
        1,
        now,
        now
    ));
    AcademicPathEntity certification = academicPathRepository.save(new AcademicPathEntity(
        "ap_cert_cpa",
        "PROFESSIONAL_CERTIFICATION",
        "CPA Professional Path",
        "KASNEB",
        "18-24 months",
        "Stage-based accounting certification focused on financial reporting, audit, taxation, and business law.",
        "KASNEB technician route, relevant degree, or approved accounting entry pathway.",
        "Foundation, Intermediate, Advanced",
        "Auditor, accountant, finance analyst, tax associate",
        "ADVANCED",
        "cpa,finance,accounting,professional",
        false,
        true,
        2,
        now,
        now
    ));
    AcademicPathEntity language = academicPathRepository.save(new AcademicPathEntity(
        "ap_lang_french",
        "LANGUAGE_PROGRAM",
        "French Fluency Track",
        "Alliance Francaise Nairobi",
        "12 months",
        "Progressive French language track for grammar, listening, conversation, and exam preparation.",
        "Open entry; placement test for continuing learners.",
        "A1, A2, B1, B2",
        "Translator, hospitality roles, immigration prep, academic exchange",
        "BEGINNER",
        "french,language,communication,global",
        false,
        true,
        3,
        now,
        now
    ));
    AcademicPathEntity bootcamp = academicPathRepository.save(new AcademicPathEntity(
        "ap_bootcamp_moringa",
        "TECH_BOOTCAMP_SHORT_COURSE",
        "Software Engineering Bootcamp",
        "Moringa School",
        "6 months",
        "Project-driven bootcamp covering frontend, backend, product thinking, and career coaching for junior developers.",
        "Basic computer literacy and commitment to full-time project work.",
        "Foundation, Core, Capstone",
        "Frontend developer, backend trainee, junior software engineer",
        "INTENSIVE",
        "bootcamp,software-engineering,projects,career",
        false,
        true,
        4,
        now,
        now
    ));
    AcademicPathEntity online = academicPathRepository.save(new AcademicPathEntity(
        "ap_online_google_data",
        "ONLINE_CERTIFICATION_SKILL_TRACK",
        "Google Data Analytics Skill Track",
        "Coursera / Google",
        "3-6 months",
        "Self-paced data analytics certificate covering spreadsheets, SQL, Tableau, and job-ready case studies.",
        "Open entry; ideal for beginners with consistent weekly study time.",
        "Course 1 to Course 8",
        "Data analyst, reporting associate, business intelligence support",
        "BEGINNER",
        "data,analytics,online,certificate,skills",
        true,
        true,
        5,
        now,
        now
    ));

    AcademicModuleEntity diplomaOne = academicModuleRepository.save(new AcademicModuleEntity(
        diploma,
        "Network Defense Fundamentals",
        "Core unit for TCP/IP review, firewall rules, and secure network segmentation.",
        "SEMESTER",
        "Semester 1",
        1
    ));
    academicModuleRepository.save(new AcademicModuleEntity(
        diploma,
        "Ethical Hacking Lab",
        "Guided penetration-testing practice with reporting discipline and lab safety.",
        "SEMESTER",
        "Semester 3",
        2
    ));

    AcademicModuleEntity cpaOne = academicModuleRepository.save(new AcademicModuleEntity(
        certification,
        "Financial Accounting",
        "Ledger flow, adjustments, financial statements, and exam-standard workings.",
        "CERTIFICATION_STAGE",
        "Foundation",
        1
    ));
    academicModuleRepository.save(new AcademicModuleEntity(
        certification,
        "Taxation",
        "VAT, income tax, corporate tax, and exam-oriented calculation practice.",
        "CERTIFICATION_STAGE",
        "Intermediate",
        2
    ));

    AcademicModuleEntity frenchOne = academicModuleRepository.save(new AcademicModuleEntity(
        language,
        "French A1 Communication",
        "Greetings, introductions, survival grammar, and short listening drills.",
        "LEVEL",
        "A1",
        1
    ));
    academicModuleRepository.save(new AcademicModuleEntity(
        language,
        "French B1 Conversation",
        "Structured speaking, reading comprehension, and grammar reinforcement.",
        "LEVEL",
        "B1",
        2
    ));

    AcademicModuleEntity bootcampOne = academicModuleRepository.save(new AcademicModuleEntity(
        bootcamp,
        "Frontend Product Build",
        "Responsive UI delivery with JavaScript, APIs, and production polish.",
        "STAGE",
        "Core",
        1
    ));
    academicModuleRepository.save(new AcademicModuleEntity(
        bootcamp,
        "Career Readiness Sprint",
        "Portfolio, interview rehearsal, and capstone storytelling.",
        "STAGE",
        "Capstone",
        2
    ));

    AcademicModuleEntity onlineOne = academicModuleRepository.save(new AcademicModuleEntity(
        online,
        "Foundations: Ask, Prepare, Process",
        "Data cleaning, spreadsheet structure, and analytical framing.",
        "MODULE",
        "Course 1-3",
        1
    ));
    academicModuleRepository.save(new AcademicModuleEntity(
        online,
        "Analyze and Share",
        "SQL, Tableau, dashboards, and portfolio case-study writing.",
        "MODULE",
        "Course 4-8",
        2
    ));

    academicResourceRepository.save(new AcademicResourceEntity(
        diploma,
        diplomaOne,
        "usr_admin",
        "Cyber lab checklist",
        "PDF",
        "Starter checklist for secure lab discipline and revision priorities.",
        "https://predict.ed/resources/cyber-lab-checklist",
        null,
        null,
        null,
        0,
        1,
        now
    ));
    academicResourceRepository.save(new AcademicResourceEntity(
        certification,
        cpaOne,
        "usr_admin",
        "CPA formula pack",
        "NOTES",
        "Quick-recall sheet for accounting adjustments and tax computation patterns.",
        "https://predict.ed/resources/cpa-formula-pack",
        null,
        null,
        null,
        0,
        1,
        now
    ));
    academicResourceRepository.save(new AcademicResourceEntity(
        language,
        frenchOne,
        "usr_admin",
        "French listening playlist",
        "AUDIO",
        "Beginner listening and pronunciation drills.",
        "https://predict.ed/resources/french-a1-listening",
        null,
        null,
        null,
        0,
        1,
        now
    ));
    academicResourceRepository.save(new AcademicResourceEntity(
        bootcamp,
        bootcampOne,
        "usr_admin",
        "Bootcamp capstone rubric",
        "GUIDE",
        "Project quality checklist for frontend/backend delivery and demos.",
        "https://predict.ed/resources/bootcamp-capstone-rubric",
        null,
        null,
        null,
        0,
        1,
        now
    ));
    academicResourceRepository.save(new AcademicResourceEntity(
        online,
        onlineOne,
        "usr_admin",
        "Google DA study map",
        "ROADMAP",
        "Weekly completion guide for self-paced learners balancing work and study.",
        "https://predict.ed/resources/google-da-study-map",
        null,
        null,
        null,
        0,
        1,
        now
    ));
  }
}
