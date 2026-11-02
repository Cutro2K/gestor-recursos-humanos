import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { SESSION_COOKIE } from "@/components/auth/session-cookie";

/**
 * Control optimista del CU-01: corre antes de renderizar cada página.
 * Sin sesión, todo lleva a /login; con sesión, /login lleva al inicio.
 * Solo mira si existe la cookie: la validación real queda en el backend.
 */
export function proxy(request: NextRequest) {
  const loggedIn = request.cookies.has(SESSION_COOKIE);
  const onLogin = request.nextUrl.pathname === "/login";

  if (!loggedIn && !onLogin) return NextResponse.redirect(new URL("/login", request.url));
  if (loggedIn && onLogin) return NextResponse.redirect(new URL("/", request.url));
  return NextResponse.next();
}

// No corre para archivos internos de Next ni para archivos públicos (imágenes, GIF, CSV…).
export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|.*\\.[a-z0-9]+$).*)"],
};
