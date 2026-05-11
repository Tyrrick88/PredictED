alter table note_packs add column uploaded_by_user_id varchar(64);
alter table note_packs add column course_id varchar(64);
alter table note_packs add column original_filename varchar(255);
alter table note_packs add column storage_path varchar(500);
alter table note_packs add column content_type varchar(120);
alter table note_packs add column size_bytes bigint not null default 0;
alter table note_packs add column sha256 varchar(64);
alter table note_packs add column created_at timestamp with time zone not null default current_timestamp;

alter table note_packs
  add constraint fk_note_packs_uploaded_by
  foreign key (uploaded_by_user_id) references app_users (id);

alter table note_packs
  add constraint fk_note_packs_course
  foreign key (course_id) references courses (id);

create index idx_note_packs_uploaded_by on note_packs (uploaded_by_user_id);
create index idx_note_packs_course on note_packs (course_id);
create index idx_note_packs_created_at on note_packs (created_at desc);
