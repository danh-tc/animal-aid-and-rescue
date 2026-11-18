"use client";

import "leaflet/dist/leaflet.css";
import L from "leaflet";
import { type Dispatch, type SetStateAction, useEffect, useRef, useState } from "react";
import { formatRelative } from "@/lib/format";
import { CASE_STATUS } from "@/lib/labels";
import type { MapPoint } from "@/types/api";
import { type MarkerKind, markerKind, pointKey } from "./markerKind";
import "./rescue-map.scss";

const DEFAULT_CENTER: L.LatLngTuple = [10.7769, 106.7009];
const FIT_OPTIONS: L.FitBoundsOptions = { padding: [56, 56], maxZoom: 15 };

const OSM_CREDIT = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

/**
 * MapTiler when a key is configured, keyless CARTO otherwise.
 * tile.openstreetmap.org is avoided because some Vietnamese ISP resolvers fail to resolve it.
 */
function tileLayer(dark: boolean): L.TileLayer {
  const key = process.env.NEXT_PUBLIC_MAPTILER_KEY;
  if (key) {
    const style = dark ? "dataviz-dark" : "dataviz";
    return L.tileLayer(`https://api.maptiler.com/maps/${style}/256/{z}/{x}/{y}{r}.png?key=${encodeURIComponent(key)}`, {
      maxZoom: 19,
      attribution: `&copy; <a href="https://www.maptiler.com/copyright/">MapTiler</a> ${OSM_CREDIT}`,
    });
  }
  return L.tileLayer(`https://{s}.basemaps.cartocdn.com/${dark ? "dark_all" : "light_all"}/{z}/{x}/{y}{r}.png`, {
    maxZoom: 19,
    subdomains: "abcd",
    attribution: `${OSM_CREDIT} &copy; <a href="https://carto.com/attributions">CARTO</a>`,
  });
}

/** Mirrors the design tokens: data-theme wins, otherwise the OS preference. */
function useDarkMode(): boolean {
  const [dark, setDark] = useState(false);
  useEffect(() => {
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    const read = () => {
      const forced = document.documentElement.dataset.theme;
      setDark(forced ? forced === "dark" : media.matches);
    };
    read();
    media.addEventListener("change", read);
    const observer = new MutationObserver(read);
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ["data-theme"] });
    return () => {
      media.removeEventListener("change", read);
      observer.disconnect();
    };
  }, []);
  return dark;
}

const svg = (body: string) =>
  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.25" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${body}</svg>`;
const ICONS: Record<MarkerKind, string> = {
  report: svg(
    '<path d="M7 18v-6a5 5 0 1 1 10 0v6"/><path d="M5 21a1 1 0 0 1-1-1v-1a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v1a1 1 0 0 1-1 1z"/><path d="M21 12h1"/><path d="M18.5 4.5 18 5"/><path d="M2 12h1"/><path d="M12 2v1"/><path d="m4.929 4.929.707.707"/><path d="M12 12v6"/>',
  ),
  "case-new": svg('<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>'),
  "case-active": svg(
    '<path d="M10 10H6"/><path d="M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2"/><path d="M19 18h2a1 1 0 0 0 1-1v-3.28a1 1 0 0 0-.684-.948l-1.923-.641a1 1 0 0 1-.578-.502l-1.539-3.076A1 1 0 0 0 16.382 8H14"/><path d="M8 8v4"/><path d="M9 18h6"/><circle cx="17" cy="18" r="2"/><circle cx="7" cy="18" r="2"/>',
  ),
};
const FIT_ICON = svg(
  '<path d="M8 3H5a2 2 0 0 0-2 2v3"/><path d="M21 8V5a2 2 0 0 0-2-2h-3"/><path d="M3 16v3a2 2 0 0 0 2 2h3"/><path d="M16 21h3a2 2 0 0 0 2-2v-3"/>',
);

function pinIcon(kind: MarkerKind, selected: boolean): L.DivIcon {
  return L.divIcon({
    className: "rescue-pin-anchor",
    html: `<span class="rescue-pin rescue-pin--${kind}${selected ? " rescue-pin--selected" : ""}">${ICONS[kind]}</span>`,
    iconSize: [36, 44],
    iconAnchor: [18, 42],
    popupAnchor: [0, -38],
    tooltipAnchor: [0, -40],
  });
}

const STATUS_TEXT: Record<MarkerKind, string> = {
  report: "Báo cáo mới",
  "case-new": CASE_STATUS.NEW.label,
  "case-active": CASE_STATUS.IN_PROGRESS.label,
};

function popupContent(p: MapPoint, onOpen: (point: MapPoint) => void): HTMLElement {
  // Built with DOM nodes, not an HTML string: locationText is public user input.
  const kind = markerKind(p);
  const el = <K extends keyof HTMLElementTagNameMap>(tag: K, className: string, text?: string) => {
    const node = document.createElement(tag);
    node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  };

  const root = el("div", "rescue-popup");
  const head = el("div", "rescue-popup__head");
  head.append(el("span", `rescue-popup__chip rescue-popup__chip--${kind}`, STATUS_TEXT[kind]), el("span", "rescue-popup__time", formatRelative(p.createdAt)));
  const open = el("button", "rescue-popup__action", p.kind === "REPORT" ? "Xử lý báo cáo →" : "Mở ca cứu hộ →");
  open.type = "button";
  open.addEventListener("click", () => onOpen(p));
  root.append(head, el("strong", "rescue-popup__title", p.label), el("p", "rescue-popup__location", p.locationText), open);
  return root;
}

interface RescueMapProps {
  points: MapPoint[];
  selected: string | null;
  onSelect: Dispatch<SetStateAction<string | null>>;
  onOpen: (point: MapPoint) => void;
}

export default function RescueMap({ points, selected, onSelect, onOpen }: Readonly<RescueMapProps>) {
  const dark = useDarkMode();
  const container = useRef<HTMLElement>(null);
  const map = useRef<L.Map | null>(null);
  const tiles = useRef<L.TileLayer | null>(null);
  const layer = useRef<L.LayerGroup | null>(null);
  const markers = useRef(new Map<string, { marker: L.Marker; point: MapPoint }>());
  const bounds = useRef<L.LatLngBounds | null>(null);
  const callbacks = useRef({ onSelect, onOpen });
  callbacks.current = { onSelect, onOpen };

  useEffect(() => {
    if (!container.current) return;
    const m = L.map(container.current, { center: DEFAULT_CENTER, zoom: 12, zoomControl: false });
    L.control.zoom({ position: "bottomright", zoomInTitle: "Phóng to", zoomOutTitle: "Thu nhỏ" }).addTo(m);

    const FitControl = L.Control.extend({
      onAdd() {
        const button = L.DomUtil.create("button", "rescue-map__fit");
        button.type = "button";
        button.title = "Xem tất cả điểm";
        button.setAttribute("aria-label", "Xem tất cả điểm");
        button.innerHTML = FIT_ICON;
        L.DomEvent.disableClickPropagation(button);
        L.DomEvent.on(button, "click", () => bounds.current && m.flyToBounds(bounds.current, FIT_OPTIONS));
        return button;
      },
    });
    new FitControl({ position: "bottomright" }).addTo(m);

    map.current = m;
    layer.current = L.layerGroup().addTo(m);
    return () => {
      // Cancel an in-flight flyTo so Leaflet does not draw on a removed map.
      m.stop();
      m.remove();
      map.current = null;
      tiles.current = null;
      layer.current = null;
    };
  }, []);

  useEffect(() => {
    const m = map.current;
    if (!m) return;
    tiles.current?.remove();
    tiles.current = tileLayer(dark).addTo(m);
  }, [dark]);

  useEffect(() => {
    const m = map.current;
    const group = layer.current;
    if (!m || !group) return;
    group.clearLayers();
    markers.current.clear();
    for (const p of points) {
      const key = pointKey(p);
      const marker = L.marker([p.latitude, p.longitude], {
        icon: pinIcon(markerKind(p), false),
        title: `${p.label}: ${p.locationText}`,
        riseOnHover: true,
      })
        .bindTooltip(p.label, { direction: "top", className: "rescue-map__tooltip" })
        .bindPopup(() => popupContent(p, (x) => callbacks.current.onOpen(x)), {
          className: "rescue-map__popup",
          closeButton: false,
          minWidth: 220,
          maxWidth: 280,
        })
        .on("click", () => callbacks.current.onSelect(key))
        // Opening another popup closes this one; only clear the selection if it is still ours.
        .on("popupclose", () => callbacks.current.onSelect((cur) => (cur === key ? null : cur)))
        .addTo(group);
      markers.current.set(key, { marker, point: p });
    }
    bounds.current = points.length > 0 ? L.latLngBounds(points.map((p) => [p.latitude, p.longitude] as L.LatLngTuple)) : null;
    // Animated zoom runs in a later frame and crashes in createTile if StrictMode has already removed the map.
    if (bounds.current) m.fitBounds(bounds.current, { ...FIT_OPTIONS, animate: false });
  }, [points]);

  useEffect(() => {
    const m = map.current;
    if (!m) return;
    for (const [key, { marker, point }] of markers.current) {
      const isSelected = key === selected;
      marker.setIcon(pinIcon(markerKind(point), isSelected));
      marker.setZIndexOffset(isSelected ? 1000 : 0);
      if (isSelected && !marker.isPopupOpen()) {
        // Open after the flight so the popup's autoPan does not fight flyTo.
        m.once("moveend", () => markers.current.get(key)?.marker === marker && marker.openPopup());
        m.flyTo(marker.getLatLng(), Math.max(m.getZoom(), 15), { duration: 0.6 });
      }
    }
  }, [selected, points]);

  return <section ref={container} className="rescue-map" aria-label="Bản đồ cứu hộ" />;
}
