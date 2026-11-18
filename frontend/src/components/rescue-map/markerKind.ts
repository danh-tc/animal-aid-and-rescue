import type { MapPoint } from "@/types/api";

export type MarkerKind = "report" | "case-new" | "case-active";

/** Kept out of RescueMap.tsx so server-rendered code can use it without importing Leaflet. */
export function markerKind(p: MapPoint): MarkerKind {
  if (p.kind === "REPORT") return "report";
  return p.status === "NEW" ? "case-new" : "case-active";
}

/** Reports and cases can share an id. */
export function pointKey(p: MapPoint): string {
  return `${p.kind}-${p.id}`;
}
