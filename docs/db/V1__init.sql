CREATE TABLE users (
  id            BIGSERIAL PRIMARY KEY,
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(150) NOT NULL,
  phone         VARCHAR(20),
  address       VARCHAR(255),
  role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADOPTER','STAFF','MANAGER')),
  is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE vet_clinics (
  id        BIGSERIAL PRIMARY KEY,
  name      VARCHAR(150) NOT NULL,
  address   VARCHAR(255),
  phone     VARCHAR(20),
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE rescue_reports (
  id             BIGSERIAL PRIMARY KEY,
  code           VARCHAR(12)  NOT NULL UNIQUE,
  reporter_name  VARCHAR(150) NOT NULL,
  reporter_phone VARCHAR(20)  NOT NULL,
  location_text  VARCHAR(255) NOT NULL,
  latitude       NUMERIC(9,6),
  longitude      NUMERIC(9,6),
  description    TEXT NOT NULL,
  status         VARCHAR(20) NOT NULL DEFAULT 'NEW'
                 CHECK (status IN ('NEW','PROCESSED','DUPLICATE','INVALID')),
  handled_by     BIGINT REFERENCES users(id),
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  handled_at     TIMESTAMPTZ
);

CREATE TABLE rescue_cases (
  id                BIGSERIAL PRIMARY KEY,
  location_text     VARCHAR(255) NOT NULL,
  assigned_staff_id BIGINT REFERENCES users(id),
  status            VARCHAR(20) NOT NULL DEFAULT 'NEW'
                    CHECK (status IN ('NEW','IN_PROGRESS','SUCCEEDED','FAILED')),
  failure_reason    TEXT,
  created_by        BIGINT NOT NULL REFERENCES users(id),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  closed_at         TIMESTAMPTZ,
  CONSTRAINT ck_case_failed_reason CHECK (status <> 'FAILED' OR failure_reason IS NOT NULL)
);

CREATE TABLE report_cases (
  report_id BIGINT NOT NULL REFERENCES rescue_reports(id),
  case_id   BIGINT NOT NULL REFERENCES rescue_cases(id),
  PRIMARY KEY (report_id, case_id)
);

CREATE TABLE pets (
  id                     BIGSERIAL PRIMARY KEY,
  code                   VARCHAR(12) NOT NULL UNIQUE,
  name                   VARCHAR(100),
  species                VARCHAR(10) NOT NULL CHECK (species IN ('DOG','CAT')),
  sex                    VARCHAR(10) NOT NULL DEFAULT 'UNKNOWN'
                         CHECK (sex IN ('MALE','FEMALE','UNKNOWN')),
  estimated_birth_date   DATE,
  color                  VARCHAR(100),
  description            TEXT,
  status                 VARCHAR(20) NOT NULL
                         CHECK (status IN ('IN_CARE','IN_TREATMENT','AVAILABLE','PENDING_HANDOVER',
                                           'ADOPTED','DECEASED','LOST','TRANSFERRED')),
  area                   VARCHAR(20) CHECK (area IN ('QUARANTINE','GENERAL')),
  current_location       VARCHAR(255),
  is_vaccinated          BOOLEAN NOT NULL DEFAULT FALSE,
  is_dewormed            BOOLEAN NOT NULL DEFAULT FALSE,
  is_sterilized          BOOLEAN NOT NULL DEFAULT FALSE,
  temperament_ok         BOOLEAN NOT NULL DEFAULT FALSE,
  eligibility_checked_at TIMESTAMPTZ,
  eligibility_checked_by BIGINT REFERENCES users(id),
  version                INTEGER NOT NULL DEFAULT 0,
  created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_pets_status ON pets (status);

CREATE TABLE care_logs (
  id            BIGSERIAL PRIMARY KEY,
  pet_id        BIGINT NOT NULL REFERENCES pets(id),
  staff_id      BIGINT NOT NULL REFERENCES users(id),
  activity_type VARCHAR(20) NOT NULL
                CHECK (activity_type IN ('FEEDING','CLEANING','WEIGHT','MEDICATION','HEALTH_NOTE','AREA_CHANGE')),
  weight_kg     NUMERIC(5,2) CHECK (weight_kg > 0),
  note          TEXT,
  logged_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE vet_requests (
  id                     BIGSERIAL PRIMARY KEY,
  pet_id                 BIGINT NOT NULL REFERENCES pets(id),
  clinic_id              BIGINT NOT NULL REFERENCES vet_clinics(id),
  reason                 TEXT NOT NULL,
  priority               VARCHAR(12) NOT NULL CHECK (priority IN ('URGENT','NEED_CHECK','STABLE')),
  care_mode              VARCHAR(12) NOT NULL CHECK (care_mode IN ('OUTPATIENT','INPATIENT')),
  status                 VARCHAR(15) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING','IN_TREATMENT','COMPLETED','CANCELLED')),
  diagnosis_summary      TEXT,
  treatment_summary      TEXT,
  total_cost             NUMERIC(12,0) CHECK (total_cost >= 0),
  aftercare_instructions TEXT,
  cancel_reason          TEXT,
  created_by             BIGINT NOT NULL REFERENCES users(id),
  requested_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  completed_at           TIMESTAMPTZ,
  CONSTRAINT ck_vet_completed_cost CHECK (status <> 'COMPLETED' OR total_cost IS NOT NULL),
  CONSTRAINT ck_vet_cancel_reason  CHECK (status <> 'CANCELLED' OR cancel_reason IS NOT NULL)
);
CREATE INDEX ix_vet_requests_open ON vet_requests (pet_id) WHERE status IN ('PENDING','IN_TREATMENT');

CREATE TABLE adoption_applications (
  id                   BIGSERIAL PRIMARY KEY,
  pet_id               BIGINT NOT NULL REFERENCES pets(id),
  applicant_id         BIGINT NOT NULL REFERENCES users(id),
  housing_type         VARCHAR(20) NOT NULL CHECK (housing_type IN ('HOUSE','APARTMENT','OTHER')),
  has_pet_experience   BOOLEAN NOT NULL,
  motivation           TEXT,
  sterilization_pledge BOOLEAN NOT NULL DEFAULT FALSE,
  status               VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED','HANDED_OVER')),
  review_note          TEXT,
  reviewed_by          BIGINT REFERENCES users(id),
  reviewed_at          TIMESTAMPTZ,
  cancel_reason        TEXT,
  submitted_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  version              INTEGER NOT NULL DEFAULT 0,
  CONSTRAINT ck_app_rejected_note CHECK (status <> 'REJECTED' OR review_note IS NOT NULL)
);
-- QĐ-09: mỗi thú tối đa một đơn Đã duyệt
CREATE UNIQUE INDEX uq_app_one_approved_per_pet ON adoption_applications (pet_id)
  WHERE status = 'APPROVED';
-- Một người không có hai đơn đang chờ cho cùng một thú
CREATE UNIQUE INDEX uq_app_one_pending_per_applicant ON adoption_applications (pet_id, applicant_id)
  WHERE status = 'PENDING';

CREATE TABLE handovers (
  id               BIGSERIAL PRIMARY KEY,
  application_id   BIGINT NOT NULL UNIQUE REFERENCES adoption_applications(id),
  pet_id           BIGINT NOT NULL REFERENCES pets(id),
  staff_id         BIGINT NOT NULL REFERENCES users(id),
  handed_over_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  donation_amount  NUMERIC(12,0) NOT NULL DEFAULT 0 CHECK (donation_amount >= 0),
  note             TEXT,
  follow_up_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                   CHECK (follow_up_status IN ('IN_PROGRESS','COMPLETED','RECALL_PENDING','RETURNED','CLOSED'))
);

CREATE TABLE follow_ups (
  id              BIGSERIAL PRIMARY KEY,
  handover_id     BIGINT NOT NULL REFERENCES handovers(id),
  sequence_no     SMALLINT NOT NULL CHECK (sequence_no > 0),
  is_additional   BOOLEAN NOT NULL DEFAULT FALSE,
  due_date        DATE NOT NULL,
  status          VARCHAR(12) NOT NULL DEFAULT 'SCHEDULED'
                  CHECK (status IN ('SCHEDULED','DONE','CANCELLED')),
  result          VARCHAR(10) CHECK (result IN ('PASS','WATCH','FAIL')),
  health_ok       BOOLEAN,
  housing_ok      BOOLEAN,
  commitment_ok   BOOLEAN,
  pet_present     BOOLEAN,
  owner_reachable BOOLEAN,
  reason          TEXT,
  performed_by    BIGINT REFERENCES users(id),
  performed_at    TIMESTAMPTZ,
  CONSTRAINT uq_follow_up_seq UNIQUE (handover_id, sequence_no),
  CONSTRAINT ck_follow_up_done_result CHECK (status <> 'DONE' OR result IS NOT NULL),
  CONSTRAINT ck_follow_up_reason CHECK (result IS NULL OR result = 'PASS' OR reason IS NOT NULL)
);
CREATE INDEX ix_follow_ups_due ON follow_ups (due_date) WHERE status = 'SCHEDULED';

CREATE TABLE intakes (
  id                   BIGSERIAL PRIMARY KEY,
  pet_id               BIGINT NOT NULL REFERENCES pets(id),
  source               VARCHAR(10) NOT NULL CHECK (source IN ('RESCUE','RETURN','FOUND')),
  rescue_case_id       BIGINT UNIQUE REFERENCES rescue_cases(id),
  returned_handover_id BIGINT UNIQUE REFERENCES handovers(id),
  intake_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  reason               TEXT,
  priority             VARCHAR(12) NOT NULL CHECK (priority IN ('URGENT','NEED_CHECK','STABLE')),
  initial_assessment   TEXT,
  created_by           BIGINT NOT NULL REFERENCES users(id),
  CONSTRAINT ck_intake_rescue CHECK (source <> 'RESCUE' OR rescue_case_id IS NOT NULL),
  CONSTRAINT ck_intake_return CHECK (source <> 'RETURN' OR returned_handover_id IS NOT NULL)
);

CREATE TABLE pet_end_events (
  id              BIGSERIAL PRIMARY KEY,
  pet_id          BIGINT NOT NULL REFERENCES pets(id),
  event_type      VARCHAR(12) NOT NULL CHECK (event_type IN ('DECEASED','LOST','TRANSFERRED')),
  occurred_at     TIMESTAMPTZ NOT NULL,
  detail          TEXT,
  destination_org VARCHAR(255),
  recorded_by     BIGINT NOT NULL REFERENCES users(id),
  CONSTRAINT ck_end_transfer_dest CHECK (event_type <> 'TRANSFERRED' OR destination_org IS NOT NULL)
);

CREATE TABLE attachments (
  id             BIGSERIAL PRIMARY KEY,
  file_path      VARCHAR(500) NOT NULL,
  original_name  VARCHAR(255),
  report_id      BIGINT REFERENCES rescue_reports(id),
  pet_id         BIGINT REFERENCES pets(id),
  vet_request_id BIGINT REFERENCES vet_requests(id),
  follow_up_id   BIGINT REFERENCES follow_ups(id),
  uploaded_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_attachment_one_owner
    CHECK (num_nonnulls(report_id, pet_id, vet_request_id, follow_up_id) = 1)
);

CREATE TABLE status_histories (
  id          BIGSERIAL PRIMARY KEY,
  entity_type VARCHAR(20) NOT NULL
              CHECK (entity_type IN ('PET','APPLICATION','VET_REQUEST','RESCUE_CASE','RESCUE_REPORT','HANDOVER')),
  entity_id   BIGINT NOT NULL,
  from_status VARCHAR(20),
  to_status   VARCHAR(20) NOT NULL,
  event       VARCHAR(50) NOT NULL,
  note        TEXT,
  changed_by  BIGINT REFERENCES users(id),
  changed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_status_histories_entity ON status_histories (entity_type, entity_id);

CREATE TABLE system_parameters (
  param_key   VARCHAR(50) PRIMARY KEY,
  param_value VARCHAR(100) NOT NULL,
  description VARCHAR(255)
);
INSERT INTO system_parameters (param_key, param_value, description) VALUES
  ('MIN_ADOPTION_AGE_WEEKS',     '8',  'Tuoi toi thieu tach me (tuan)'),
  ('QUARANTINE_DAYS',            '14', 'So ngay cach ly theo doi'),
  ('FOLLOW_UP_1_DAYS',           '14', 'Hau kiem lan 1 sau ban giao (ngay)'),
  ('FOLLOW_UP_2_DAYS',           '60', 'Hau kiem lan 2 sau ban giao (ngay)'),
  ('APPLICATION_ALERT_DAYS',     '7',  'Don cho qua so ngay nay vao bao cao ngoai le'),
  ('REPORT_RATE_LIMIT_PER_HOUR', '5',  'So bao cao toi da moi so dien thoai moi gio');
