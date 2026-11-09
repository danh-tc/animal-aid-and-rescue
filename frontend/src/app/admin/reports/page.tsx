"use client";

import { CalendarClock, HeartHandshake, Hourglass, PawPrint, Siren, Undo2 } from "lucide-react";
import Link from "next/link";
import { type FormEvent, useState } from "react";
import { StatusBadge } from "@/components/badge/Badge";
import { BarChart } from "@/components/bar-chart/BarChart";
import { Button } from "@/components/button/Button";
import { DataTable } from "@/components/data-table/DataTable";
import { EmptyState } from "@/components/empty-state/EmptyState";
import { Notice } from "@/components/notice/Notice";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Panel } from "@/components/panel/Panel";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { TabPanel, Tabs } from "@/components/tabs/Tabs";
import { useApi } from "@/hooks/useApi";
import { useUrlState } from "@/hooks/useUrlState";
import { formatCompact, formatDate, formatDateTime, formatMoney, formatMonth, petName } from "@/lib/format";
import { END_EVENT, PET_STATUS } from "@/lib/labels";
import type { CostReport, EndEventType, ExceptionReport, PetStatus, SummaryReport } from "@/types/api";
import "./report-page.scss";

type Tab = "summary" | "exceptions" | "costs";
const count = (n: number) => n.toLocaleString("vi-VN");

export default function ReportsPage() {
  const [tab, setTab] = useUrlState<Tab>("tab", "summary");
  const [from, setFrom] = useUrlState<string>("from", "");
  const [to, setTo] = useUrlState<string>("to", "");
  const [draft, setDraft] = useState({ from, to });
  const period = { from: from || undefined, to: to || undefined };

  function apply(event: FormEvent) {
    event.preventDefault();
    setFrom(draft.from);
    setTo(draft.to);
  }

  return (
    <>
      <PageHeader
        title="Báo cáo thống kê"
        description="Mặc định 12 tháng gần nhất. Báo cáo ngoại lệ luôn lấy số liệu tại thời điểm xem."
      />
      <Tabs
        label="Loại báo cáo"
        value={tab}
        onChange={(t) => setTab(t as Tab)}
        items={[
          { id: "summary", label: "Tổng hợp" },
          { id: "exceptions", label: "Ngoại lệ" },
          { id: "costs", label: "Chi phí và ủng hộ" },
        ]}
      />
      {tab !== "exceptions" && (
        <form className="report-page__period" onSubmit={apply}>
          <label className="report-page__field">
            <span>Từ ngày</span>
            <input type="date" value={draft.from} onChange={(e) => setDraft({ ...draft, from: e.target.value })} />
          </label>
          <label className="report-page__field">
            <span>Đến ngày</span>
            <input type="date" value={draft.to} onChange={(e) => setDraft({ ...draft, to: e.target.value })} />
          </label>
          <Button type="submit" size="sm">
            Xem
          </Button>
          {(from || to) && (
            <Button
              size="sm"
              variant="ghost"
              onClick={() => {
                setDraft({ from: "", to: "" });
                setFrom("");
                setTo("");
              }}
            >
              12 tháng gần nhất
            </Button>
          )}
        </form>
      )}
      <TabPanel id={tab}>
        {tab === "summary" && <SummaryTab period={period} />}
        {tab === "exceptions" && <ExceptionsTab />}
        {tab === "costs" && <CostsTab period={period} />}
      </TabPanel>
    </>
  );
}

function Tile({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="report-page__tile">
      <span className="report-page__tile-label">{label}</span>
      <span className="report-page__tile-value">{value}</span>
      {hint && <span className="report-page__tile-hint">{hint}</span>}
    </div>
  );
}

function SummaryTab({ period }: { period: { from?: string; to?: string } }) {
  const { data, error, loading } = useApi<SummaryReport>("/api/admin/reports/summary", period);
  if (error) return <Notice tone="error">{error}</Notice>;
  if (!data) return loading ? <Skeleton card lines={6} /> : null;
  const successRate = data.casesSucceeded + data.casesFailed > 0
    ? Math.round((data.casesSucceeded / (data.casesSucceeded + data.casesFailed)) * 100)
    : null;

  return (
    <div className="stack stack--lg">
      <p className="report-page__range">
        Kỳ {formatDate(data.period.from)} đến {formatDate(data.period.to)}
      </p>
      <div className="report-page__tiles">
        <Tile label="Báo cáo cứu hộ đã nhận" value={count(data.reportsReceived)} />
        <Tile label="Ca cứu hộ được tạo" value={count(data.casesCreated)} />
        <Tile
          label="Ca thành công"
          value={count(data.casesSucceeded)}
          hint={successRate === null ? undefined : `${successRate}% ca đã đóng, ${count(data.casesFailed)} thất bại`}
        />
        <Tile label="Thú được nhận nuôi" value={count(data.adoptions)} />
        <Tile
          label="Thú bị trả lại"
          value={count(data.returns)}
          hint={data.adoptions > 0 ? `${Math.round((data.returns / data.adoptions) * 100)}% số lượt bàn giao` : undefined}
        />
      </div>
      <Panel title="Theo tháng">
        <BarChart
          title="Ca cứu thành công, nhận nuôi và trả lại"
          summary={`Theo tháng từ ${formatMonth(data.months[0]?.month ?? "")} đến ${formatMonth(data.months.at(-1)?.month ?? "")}: tổng ${data.casesSucceeded} ca cứu thành công, ${data.adoptions} lượt nhận nuôi, ${data.returns} lượt trả lại.`}
          categories={data.months.map((m) => m.month)}
          categoryLabel={formatMonth}
          series={[
            { key: "cases", label: "Cứu thành công" },
            { key: "adoptions", label: "Nhận nuôi" },
            { key: "returns", label: "Trả lại" },
          ]}
          values={data.months.map((m) => [m.casesSucceeded, m.adoptions, m.returns])}
          format={count}
        />
      </Panel>
      <div className="report-page__columns">
        <Panel title="Sự kiện kết thúc trong kỳ">
          <ul className="report-page__counts">
            {(Object.keys(data.endEvents) as EndEventType[]).map((k) => (
              <li key={k}>
                <StatusBadge map={END_EVENT} value={k} />
                <strong>{count(data.endEvents[k])}</strong>
              </li>
            ))}
          </ul>
        </Panel>
        <Panel title="Thú theo trạng thái hiện tại">
          <ul className="report-page__counts">
            {(Object.keys(data.petsByStatus) as PetStatus[]).map((k) => (
              <li key={k}>
                <StatusBadge map={PET_STATUS} value={k} />
                <strong>{count(data.petsByStatus[k] ?? 0)}</strong>
              </li>
            ))}
          </ul>
        </Panel>
      </div>
    </div>
  );
}

function ExceptionsTab() {
  const { data, error, loading } = useApi<ExceptionReport>("/api/admin/reports/exceptions");
  if (error) return <Notice tone="error">{error}</Notice>;
  if (!data) return loading ? <Skeleton card lines={6} /> : null;

  return (
    <div className="stack stack--lg">
      <div className="report-page__tiles">
        <Tile label="Hậu kiểm quá hạn" value={count(data.overdueFollowUps.length)} />
        <Tile label={`Đơn chờ quá ${data.applicationAlertDays} ngày`} value={count(data.staleApplications.length)} hint="Đổi ngưỡng ở mục Tham số hệ thống" />
        <Tile label="Bàn giao chờ thu hồi" value={count(data.recallPending.length)} />
        <Tile
          label={`Ca chưa phân công quá ${data.caseUnassignedAlertHours} giờ`}
          value={count(data.unassignedCases.length)}
          hint="Đổi ngưỡng ở mục Tham số hệ thống"
        />
      </div>
      <Panel title={`Ca cứu hộ chưa phân công quá ${data.caseUnassignedAlertHours} giờ`} flush>
        {data.unassignedCases.length === 0 ? (
          <EmptyState icon={Siren} title="Không có ca chờ phân công quá hạn" />
        ) : (
          <DataTable
            caption="Ca cứu hộ chưa phân công quá hạn"
            rows={data.unassignedCases}
            rowKey={(r) => r.caseId}
            rowHref={(r) => `/staff/cases/${r.caseId}`}
            columns={[
              { key: "case", header: "Ca", render: (r) => `#${r.caseId} · ${r.locationText}` },
              { key: "created", header: "Tạo lúc", render: (r) => formatDateTime(r.createdAt), hideOnMobile: true },
              { key: "wait", header: "Đã chờ", numeric: true, render: (r) => `${r.hoursWaiting} giờ` },
            ]}
          />
        )}
      </Panel>
      <Panel title="Hậu kiểm quá hạn" flush>
        {data.overdueFollowUps.length === 0 ? (
          <EmptyState icon={CalendarClock} title="Không có lịch hậu kiểm quá hạn" />
        ) : (
          <DataTable
            caption="Hậu kiểm quá hạn"
            rows={data.overdueFollowUps}
            rowKey={(r) => r.followUpId}
            rowHref={(r) => `/staff/follow-ups?range=all&focus=${r.followUpId}`}
            columns={[
              { key: "pet", header: "Thú", render: (r) => `${petName(r.petName, r.petCode)} · lần ${r.sequenceNo}` },
              { key: "due", header: "Hạn", render: (r) => formatDate(r.dueDate) },
              { key: "late", header: "Quá hạn", numeric: true, render: (r) => `${r.daysOverdue} ngày` },
              { key: "owner", header: "Chủ nuôi", render: (r) => `${r.ownerName}${r.ownerPhone ? ` · ${r.ownerPhone}` : ""}` },
            ]}
          />
        )}
      </Panel>
      <Panel title={`Đơn chờ xét quá ${data.applicationAlertDays} ngày`} flush>
        {data.staleApplications.length === 0 ? (
          <EmptyState icon={Hourglass} title="Không có đơn chờ quá lâu" />
        ) : (
          <DataTable
            caption="Đơn chờ xét quá lâu"
            rows={data.staleApplications}
            rowKey={(r) => r.applicationId}
            rowHref={(r) => `/staff/applications/${r.applicationId}`}
            columns={[
              { key: "pet", header: "Thú", render: (r) => `${petName(r.petName, r.petCode)} · đơn #${r.applicationId}` },
              { key: "applicant", header: "Người nộp", render: (r) => r.applicantName },
              { key: "submitted", header: "Nộp lúc", render: (r) => formatDateTime(r.submittedAt), hideOnMobile: true },
              { key: "wait", header: "Đã chờ", numeric: true, render: (r) => `${r.daysWaiting} ngày` },
            ]}
          />
        )}
      </Panel>
      <Panel title="Bàn giao chờ thu hồi" flush>
        {data.recallPending.length === 0 ? (
          <EmptyState icon={Undo2} title="Không có bàn giao chờ thu hồi" />
        ) : (
          <DataTable
            caption="Bàn giao chờ thu hồi"
            rows={data.recallPending}
            rowKey={(r) => r.handoverId}
            rowHref={(r) => `/staff/pets/${r.petId}?tab=handovers`}
            columns={[
              { key: "pet", header: "Thú", render: (r) => petName(r.petName, r.petCode) },
              { key: "owner", header: "Chủ nuôi", render: (r) => `${r.ownerName}${r.ownerPhone ? ` · ${r.ownerPhone}` : ""}` },
              { key: "date", header: "Bàn giao", render: (r) => formatDate(r.handedOverAt), hideOnMobile: true },
              {
                key: "closable",
                header: "Đóng thu hồi được từ",
                render: (r) => (r.closableFrom ? formatDate(r.closableFrom) : "—"),
              },
            ]}
          />
        )}
      </Panel>
      {data.overdueFollowUps.length + data.staleApplications.length + data.recallPending.length + data.unassignedCases.length > 0 && (
        <Notice tone="warning" title="Cần xử lý">
          Bấm vào từng dòng để mở màn hình xử lý tương ứng.
        </Notice>
      )}
    </div>
  );
}

function CostsTab({ period }: { period: { from?: string; to?: string } }) {
  const { data, error, loading } = useApi<CostReport>("/api/admin/reports/costs", period);
  if (error) return <Notice tone="error">{error}</Notice>;
  if (!data) return loading ? <Skeleton card lines={6} /> : null;
  const gap = data.periodTreatmentCost - data.periodDonations;

  return (
    <div className="stack stack--lg">
      <p className="report-page__range">
        Kỳ {formatDate(data.period.from)} đến {formatDate(data.period.to)}
      </p>
      <div className="report-page__tiles">
        <Tile label="Chi phí điều trị trong kỳ" value={formatMoney(data.periodTreatmentCost)} />
        <Tile label="Ủng hộ nhận trong kỳ" value={formatMoney(data.periodDonations)} />
        <Tile
          label={gap >= 0 ? "Chênh lệch chưa bù đắp" : "Ủng hộ vượt chi phí"}
          value={formatMoney(Math.abs(gap))}
        />
      </div>
      <Panel title="Đối chiếu theo tháng">
        <BarChart
          title="Chi phí điều trị và tiền ủng hộ"
          summary={`Trong kỳ, chi phí điều trị ${formatMoney(data.periodTreatmentCost)}, ủng hộ ${formatMoney(data.periodDonations)}.`}
          categories={data.months.map((m) => m.month)}
          categoryLabel={formatMonth}
          series={[
            { key: "cost", label: "Chi phí điều trị" },
            { key: "donation", label: "Ủng hộ" },
          ]}
          values={data.months.map((m) => [m.treatmentCost, m.donations])}
          format={formatMoney}
          formatAxis={formatCompact}
        />
      </Panel>
      <Panel title="Theo cơ sở thú y" flush>
        {data.byClinic.length === 0 ? (
          <EmptyState title="Chưa có yêu cầu thú y hoàn tất trong kỳ" />
        ) : (
          <DataTable
            caption="Chi phí theo cơ sở thú y"
            rows={data.byClinic}
            rowKey={(r) => r.clinicId}
            columns={[
              { key: "name", header: "Cơ sở", render: (r) => r.clinicName },
              { key: "n", header: "Số lượt hoàn tất", numeric: true, render: (r) => count(r.completedRequests) },
              { key: "cost", header: "Chi phí", numeric: true, render: (r) => formatMoney(r.treatmentCost) },
            ]}
          />
        )}
      </Panel>
      <Panel title="Theo từng thú" description="Cộng dồn từ lần tiếp nhận đầu tiên, khớp với số liệu cho người nhận xem lúc bàn giao." flush>
        {data.byPet.length === 0 ? (
          <EmptyState icon={PawPrint} title="Chưa có dữ liệu chi phí" />
        ) : (
          <DataTable
            caption="Chi phí và ủng hộ theo từng thú"
            rows={data.byPet}
            rowKey={(r) => r.petId}
            rowHref={(r) => `/staff/pets/${r.petId}`}
            columns={[
              { key: "pet", header: "Thú", render: (r) => `${petName(r.petName, r.petCode)} · ${r.petCode}` },
              { key: "status", header: "Trạng thái", render: (r) => <StatusBadge map={PET_STATUS} value={r.petStatus} />, hideOnMobile: true },
              { key: "cost", header: "Chi phí", numeric: true, render: (r) => formatMoney(r.treatmentCost) },
              { key: "donated", header: "Đã ủng hộ", numeric: true, render: (r) => formatMoney(r.donated) },
              { key: "out", header: "Chưa bù đắp", numeric: true, render: (r) => formatMoney(r.outstanding) },
            ]}
          />
        )}
      </Panel>
      <p className="report-page__note">
        <HeartHandshake aria-hidden="true" /> Tiền ủng hộ không được hoàn lại khi thú bị trả lại, phần vượt chi phí thuộc về trung tâm.{" "}
        <Link href="/admin/reports?tab=exceptions">Xem báo cáo ngoại lệ</Link>
      </p>
    </div>
  );
}
