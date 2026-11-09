

import type { components } from "./openapi";

type Schemas = components["schemas"];
type EnumOf<T> = NonNullable<T>;

export type Species = EnumOf<Schemas["PublicPet"]["species"]>;
export type Sex = EnumOf<Schemas["PublicPet"]["sex"]>;
export type Role = EnumOf<Schemas["UserView"]["role"]>;
export type PetStatus = EnumOf<Schemas["PetSummary"]["status"]>;
export type PetArea = EnumOf<Schemas["PetSummary"]["area"]>;
export type Priority = EnumOf<Schemas["IntakeView"]["priority"]>;
export type ReportStatus = EnumOf<Schemas["ReportSummary"]["status"]>;
export type CaseStatus = EnumOf<Schemas["CaseSummary"]["status"]>;
export type CareActivity = EnumOf<Schemas["CareLogView"]["activityType"]>;
export type CareMode = EnumOf<Schemas["VetRequestView"]["careMode"]>;
export type VetRequestStatus = EnumOf<Schemas["VetRequestView"]["status"]>;
export type IntakeSource = EnumOf<Schemas["IntakeView"]["source"]>;
export type ApplicationStatus = EnumOf<Schemas["ApplicationView"]["status"]>;
export type HousingType = EnumOf<Schemas["ApplicationView"]["housingType"]>;
export type HandoverStatus = EnumOf<Schemas["HandoverView"]["status"]>;
export type FollowUpStatus = EnumOf<Schemas["FollowUpView"]["status"]>;
export type FollowUpResult = EnumOf<Schemas["FollowUpView"]["result"]>;
export type EndEventType = EnumOf<Schemas["EndEventView"]["eventType"]>;
export type ContactMethod = EnumOf<Schemas["FollowUpView"]["contactMethod"]>;

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface ApiErrorBody {
  code: string;
  message: string;
  fieldErrors?: Record<string, string>;
}

export interface AttachmentView {
  id: number;
  originalName: string;
  uploadedAt: string;
}

export interface UserView {
  id: number;
  email: string;
  fullName: string;
  role: Role;
}

export interface MeView extends UserView {
  phone: string | null;
  address: string | null;
}

export interface AuthResponse {
  accessToken: string;
  expiresInMinutes: number;
  user: UserView;
}

export interface PublicPet {
  id: number;
  code: string;
  name: string | null;
  species: Species;
  sex: Sex;
  estimatedBirthDate: string | null;
  color: string | null;
  description: string | null;

  sterilized: boolean;

  photoIds: number[];
}

export interface ReportRequest {
  reporterName: string;
  reporterPhone: string;
  locationText: string;
  latitude?: number | null;
  longitude?: number | null;
  description: string;
}

export interface ReportReceipt {
  code: string;
  status: ReportStatus;
  createdAt: string;
}

export interface ReportSummary {
  id: number;
  code: string;
  reporterName: string;
  reporterPhone: string;
  locationText: string;
  description: string;
  status: ReportStatus;
  createdAt: string;
}

export interface CaseSummary {
  id: number;
  locationText: string;
  status: CaseStatus;
  assignedStaffId: number | null;
  failureReason: string | null;
  createdAt: string;
  closedAt: string | null;
}

export interface ReportDetail extends ReportSummary {
  latitude: number | null;
  longitude: number | null;
  handledBy: number | null;
  handledAt: string | null;
  photos: AttachmentView[];
  cases: CaseSummary[];
  openCases: OpenCase[];
}

export interface OpenCase {
  rescueCase: CaseSummary;

  distanceKm: number | null;
  reportCodes: string[];
}

export interface ReportLookup {
  code: string;
  status: ReportStatus;
  createdAt: string;
  handledAt: string | null;
  cases: CaseProgress[];
}

export interface CaseProgress {
  status: CaseStatus;
  closedAt: string | null;

  pet: PetProgress | null;
}

export type PetStage = EnumOf<Schemas["PetProgress"]["stage"]>;

export interface PetProgress {
  stage: PetStage;
  petName: string | null;

  petId: number | null;
}

export interface CaseDetail extends CaseSummary {
  createdBy: number | null;
  reports: ReportSummary[];
  petId: number | null;
  petCode: string | null;
}

export interface SucceedRequest {
  lostPetId?: number | null;
  pet?: {
    name?: string | null;
    species: Species;
    sex?: Sex | null;
    estimatedBirthDate?: string | null;
    color?: string | null;
    description?: string | null;
  };
  intake: {
    reason?: string | null;
    priority: Priority;
    initialAssessment?: string | null;
  };
}

export interface SucceedResult {
  rescueCase: CaseSummary;
  petId: number;
  petCode: string;
  intakeId: number;
  urgentVetRequestSuggested: boolean;
}

export interface PetSummary {
  id: number;
  code: string;
  name: string | null;
  species: Species;
  sex: Sex;
  status: PetStatus;

  area: PetArea | null;
  currentLocation: string | null;
  createdAt: string;
}

export interface IntakeView {
  id: number;
  source: IntakeSource;
  rescueCaseId: number | null;
  returnedHandoverId: number | null;
  intakeAt: string;
  reason: string | null;
  priority: Priority;
  initialAssessment: string | null;
}

export interface ChecklistItem {
  key: string;
  label: string;
  passed: boolean;
  detail: string | null;
}

export interface Checklist {
  allPassed: boolean;
  items: ChecklistItem[];
}

export interface VetRequestView {
  id: number;
  petId: number;
  clinicId: number;
  clinicName: string;
  reason: string;
  priority: Priority;
  careMode: CareMode;
  status: VetRequestStatus;
  diagnosisSummary: string | null;
  treatmentSummary: string | null;
  totalCost: number | null;
  aftercareInstructions: string | null;

  healthOk: boolean | null;
  cancelReason: string | null;
  requestedAt: string;
  completedAt: string | null;
}

export interface PetDetail {
  id: number;
  code: string;
  name: string | null;
  species: Species;
  sex: Sex;
  estimatedBirthDate: string | null;
  color: string | null;
  description: string | null;
  status: PetStatus;

  area: PetArea | null;
  currentLocation: string | null;
  vaccinated: boolean;
  dewormed: boolean;
  sterilized: boolean;
  temperamentOk: boolean;
  eligibilityCheckedAt: string | null;
  eligibilityCheckedBy: number | null;
  version: number;
  createdAt: string;
  intakes: IntakeView[];
  checklist: Checklist;
  vetRequests: VetRequestView[];
  photos: AttachmentView[];
}

export interface UpdatePetRequest {
  name?: string | null;
  sex?: Sex | null;
  estimatedBirthDate?: string | null;
  color?: string | null;
  description?: string | null;
  area?: PetArea | null;
  currentLocation?: string | null;
}

export interface CareLogView {
  id: number;
  staffId: number;
  activityType: CareActivity;
  weightKg: number | null;
  note: string | null;
  loggedAt: string;
}

export interface EligibilityResult {
  status: PetStatus;
  checklist: Checklist;
  missing: string[];
}

export interface IneligibleResult {
  status: PetStatus;
  cancelledApplications: number;
}

export interface VetRequestResult {
  request: VetRequestView;
  petStatus: PetStatus;
  cancelledApplications: number;
}

export interface HistoryView {
  fromStatus: string | null;
  toStatus: string;
  event: string;
  note: string | null;
  changedBy: number | null;
  changedAt: string;
}

export interface SubmitApplicationRequest {
  petId: number;
  housingType: HousingType;
  hasPetExperience: boolean;
  motivation?: string | null;

  phone?: string | null;
  address?: string | null;
}

export interface ApplicationView {
  id: number;
  pet: { id: number; code: string; name: string | null };
  applicant: { id: number; fullName: string; email: string; phone: string | null; address: string | null };
  housingType: HousingType;
  hasPetExperience: boolean;
  motivation: string | null;
  status: ApplicationStatus;
  reviewNote: string | null;
  reviewedAt: string | null;

  handoverDate: string | null;
  cancelReason: string | null;
  submittedAt: string;
}

export interface ApplicantFlag {
  kind: "RECALLED" | "RETURNED";
  petId: number;
  petCode: string;
  petName: string | null;
  handedOverAt: string;
  note: string;
}

export interface ApplicationDetail {
  application: ApplicationView;
  petStatus: PetStatus;
  otherApplications: ApplicationView[];
  applicantHistory: ApplicantFlag[];
}

export interface FollowUpView {
  id: number;
  handoverId: number;
  sequenceNo: number;
  additional: boolean;
  dueDate: string;
  status: FollowUpStatus;
  result: FollowUpResult | null;

  contactMethod: ContactMethod | null;
  healthOk: boolean | null;
  housingOk: boolean | null;
  commitmentOk: boolean | null;
  petPresent: boolean | null;
  ownerReachable: boolean | null;
  reason: string | null;
  performedBy: number | null;
  performedAt: string | null;
}

export interface HandoverView {
  id: number;
  applicationId: number;
  petId: number;
  petCode: string;
  petName: string | null;
  ownerId: number;
  ownerName: string;
  ownerPhone: string | null;
  handedOverAt: string;
  donationAmount: number;
  note: string | null;
  status: HandoverStatus;
  followUps: FollowUpView[];
}

export interface ScheduledFollowUp {
  followUp: FollowUpView;
  petId: number;
  petCode: string;
  petName: string | null;
  ownerName: string;
  ownerPhone: string | null;
  ownerAddress: string | null;
}

export interface FollowUpResultRequest {
  result: FollowUpResult;
  contactMethod: ContactMethod;
  healthOk: boolean;
  housingOk: boolean;
  commitmentOk: boolean;
  petPresent: boolean;
  ownerReachable: boolean;
  reason?: string | null;
}

export interface FollowUpOutcome {
  followUp: FollowUpView;
  handoverStatus: HandoverStatus;
  additionalFollowUp: FollowUpView | null;
}

export interface ReturnResult {
  petId: number;
  petStatus: PetStatus;
  intakeId: number;
  handoverId: number;
  cancelledFollowUps: number;
}

export interface EndEventView {
  id: number;
  eventType: EndEventType;
  occurredAt: string;
  detail: string | null;
  recordedBy: number;
}

export interface EndEventResult {
  event: EndEventView;
  petStatus: PetStatus;
  cancelledVetRequests: number;
  cancelledApplications: number;
  cancelledFollowUps: number;
  handoverClosed: boolean;
}

export interface FoundResult {
  petId: number;
  petStatus: PetStatus;
  intakeId: number;
}

export interface PetCosts {
  petId: number;
  totalTreatmentCost: number;
  totalDonated: number;
  outstanding: number;
}

export interface Period {
  from: string;
  to: string;
}

export interface SummaryReport {
  period: Period;
  reportsReceived: number;
  casesCreated: number;
  casesSucceeded: number;
  casesFailed: number;
  adoptions: number;
  returns: number;
  endEvents: Record<EndEventType, number>;
  petsByStatus: Partial<Record<PetStatus, number>>;
  months: { month: string; casesSucceeded: number; adoptions: number; returns: number }[];
}

export type RecallPending = ExceptionReport["recallPending"][number];

export interface ExceptionReport {
  applicationAlertDays: number;
  overdueFollowUps: {
    followUpId: number;
    petId: number;
    petCode: string;
    petName: string | null;
    sequenceNo: number;
    dueDate: string;
    daysOverdue: number;
    ownerName: string;
    ownerPhone: string | null;
  }[];
  staleApplications: {
    applicationId: number;
    petId: number;
    petCode: string;
    petName: string | null;
    applicantName: string;
    submittedAt: string;
    daysWaiting: number;
  }[];
  recallPending: {
    handoverId: number;
    petId: number;
    petCode: string;
    petName: string | null;
    ownerName: string;
    ownerPhone: string | null;
    handedOverAt: string;

    failedAt: string | null;

    closableFrom: string | null;
  }[];
  caseUnassignedAlertHours: number;

  unassignedCases: {
    caseId: number;
    locationText: string;
    createdAt: string;
    hoursWaiting: number;
  }[];
}

export interface CostReport {
  period: Period;
  periodTreatmentCost: number;
  periodDonations: number;
  months: { month: string; treatmentCost: number; donations: number; difference: number }[];
  byClinic: { clinicId: number; clinicName: string; completedRequests: number; treatmentCost: number }[];
  byPet: {
    petId: number;
    petCode: string;
    petName: string | null;
    petStatus: PetStatus;
    treatmentCost: number;
    donated: number;
    outstanding: number;
  }[];
}

export interface Dashboard {
  newReports: number;
  openCases: number;
  pendingApplications: number;
  followUpsDueThisWeek: number;
  openVetRequests: number;

  overdueUnassignedCases: number;
  recallPending: number;
}

export interface StaffMember {
  id: number;
  fullName: string;
  role: Role;
}

export interface ClinicView {
  id: number;
  name: string;
  address: string | null;
  phone: string | null;
  active: boolean;
}

export interface UserAdminView {
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  address: string | null;
  role: Role;
  active: boolean;
  createdAt: string;
}

export interface ParameterView {
  key: string;
  value: string;
  description: string | null;
}
