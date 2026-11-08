import { type NextRequest, NextResponse } from "next/server";

const RULES: { test: (path: string) => boolean; roles: string[] }[] = [
  { test: (p) => p.startsWith("/admin"), roles: ["MANAGER"] },
  { test: (p) => p.startsWith("/staff"), roles: ["STAFF", "MANAGER"] },
  { test: (p) => p.startsWith("/me"), roles: ["ADOPTER"] },
  { test: (p) => /^\/pets\/[^/]+\/apply/.test(p), roles: ["ADOPTER"] },
];

export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl;
  const rule = RULES.find((r) => r.test(pathname));
  if (!rule) return NextResponse.next();

  const role = request.cookies.get("aar_role")?.value;
  if (!role) {
    const url = new URL("/login", request.url);
    url.searchParams.set("next", pathname + search);
    return NextResponse.redirect(url);
  }
  if (!rule.roles.includes(role)) {
    return NextResponse.redirect(new URL(role === "ADOPTER" ? "/me/applications" : "/staff", request.url));
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/admin/:path*", "/staff/:path*", "/me/:path*", "/pets/:id/apply"],
};
