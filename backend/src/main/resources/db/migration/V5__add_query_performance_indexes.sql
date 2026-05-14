create index if not exists idx_academic_paths_display_order on academic_paths (display_order);

create index if not exists idx_academic_resources_module_order
  on academic_resources (module_id, display_order);

create index if not exists idx_academic_resources_uploaded_by_created_at
  on academic_resources (uploaded_by_user_id, created_at);

create index if not exists idx_feed_signals_created_by_created_at
  on feed_signals (created_by_user_id, created_at desc);

create index if not exists idx_study_tasks_user_course_time
  on study_tasks (user_id, course, scheduled_time);

create index if not exists idx_flashcards_user_course_due
  on flashcards (user_id, course, due_in_hours, id);

create index if not exists idx_planner_profiles_academic_path
  on planner_profiles (academic_path_id);

create index if not exists idx_planner_sessions_user_date_completed
  on planner_sessions (user_id, session_date, completed);
