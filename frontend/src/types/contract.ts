

import type * as Api from "./api";
import type { components } from "./openapi";

type S = components["schemas"];

type SameKeys<Generated, Hand> = [keyof Generated] extends [keyof Hand]
  ? [keyof Hand] extends [keyof Generated]
    ? true
    : { thieuTrongOpenApi: Exclude<keyof Hand, keyof Generated> }
  : { thieuTrongApiTs: Exclude<keyof Generated, keyof Hand> };

type Expect<T extends true> = T;

export type Contract = [
  Expect<SameKeys<S["PublicPet"], Api.PublicPet>>,
  Expect<SameKeys<S["UserView"], Api.UserView>>,
  Expect<SameKeys<S["MeView"], Api.MeView>>,
  Expect<SameKeys<S["ApplicantFlag"], Api.ApplicantFlag>>,
  Expect<SameKeys<S["AuthResponse"], Api.AuthResponse>>,
  Expect<SameKeys<S["ReportReceipt"], Api.ReportReceipt>>,
  Expect<SameKeys<S["ReportLookup"], Api.ReportLookup>>,
  Expect<SameKeys<S["CaseProgress"], Api.CaseProgress>>,
  Expect<SameKeys<S["PetProgress"], Api.PetProgress>>,
  Expect<SameKeys<S["ReportSummary"], Api.ReportSummary>>,
  Expect<SameKeys<S["ReportDetail"], Api.ReportDetail>>,
  Expect<SameKeys<S["OpenCase"], Api.OpenCase>>,
  Expect<SameKeys<S["CaseSummary"], Api.CaseSummary>>,
  Expect<SameKeys<S["CaseDetail"], Api.CaseDetail>>,
  Expect<SameKeys<S["SucceedResult"], Api.SucceedResult>>,
  Expect<SameKeys<S["PetSummary"], Api.PetSummary>>,
  Expect<SameKeys<S["PetDetail"], Api.PetDetail>>,
  Expect<SameKeys<S["IntakeView"], Api.IntakeView>>,
  Expect<SameKeys<S["Item"], Api.ChecklistItem>>,
  Expect<SameKeys<S["Checklist"], Api.Checklist>>,
  Expect<SameKeys<S["VetRequestView"], Api.VetRequestView>>,
  Expect<SameKeys<S["VetRequestResult"], Api.VetRequestResult>>,
  Expect<SameKeys<S["CareLogView"], Api.CareLogView>>,
  Expect<SameKeys<S["EligibilityResult"], Api.EligibilityResult>>,
  Expect<SameKeys<S["IneligibleResult"], Api.IneligibleResult>>,
  Expect<SameKeys<S["HistoryView"], Api.HistoryView>>,
  Expect<SameKeys<S["ApplicationView"], Api.ApplicationView>>,
  Expect<SameKeys<S["ApplicationDetail"], Api.ApplicationDetail>>,
  Expect<SameKeys<S["FollowUpView"], Api.FollowUpView>>,
  Expect<SameKeys<S["HandoverView"], Api.HandoverView>>,
  Expect<SameKeys<S["ScheduledFollowUp"], Api.ScheduledFollowUp>>,
  Expect<SameKeys<S["FollowUpOutcome"], Api.FollowUpOutcome>>,
  Expect<SameKeys<S["ReturnResult"], Api.ReturnResult>>,
  Expect<SameKeys<S["EndEventView"], Api.EndEventView>>,
  Expect<SameKeys<S["EndEventResult"], Api.EndEventResult>>,
  Expect<SameKeys<S["FoundResult"], Api.FoundResult>>,
  Expect<SameKeys<S["PetCosts"], Api.PetCosts>>,
  Expect<SameKeys<S["Dashboard"], Api.Dashboard>>,
  Expect<SameKeys<S["StaffMember"], Api.StaffMember>>,
  Expect<SameKeys<S["ClinicView"], Api.ClinicView>>,
  Expect<SameKeys<S["UserAdminView"], Api.UserAdminView>>,
  Expect<SameKeys<S["ParameterView"], Api.ParameterView>>,
  Expect<SameKeys<S["AttachmentView"], Api.AttachmentView>>,
  Expect<SameKeys<S["SummaryReport"], Api.SummaryReport>>,
  Expect<SameKeys<S["ExceptionReport"], Api.ExceptionReport>>,
  Expect<SameKeys<S["RecallPending"], Api.RecallPending>>,
  Expect<SameKeys<S["CostReport"], Api.CostReport>>,
  Expect<SameKeys<S["SubmitRequest"], Api.SubmitApplicationRequest>>,
  Expect<SameKeys<S["FollowUpResultRequest"], Api.FollowUpResultRequest>>,
  Expect<SameKeys<S["UpdatePetRequest"], Api.UpdatePetRequest>>,
  Expect<SameKeys<S["ReportRequest"], Api.ReportRequest>>,
];
