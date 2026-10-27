export const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"
).replace(/\/+$/, "");

export class ApiError extends Error {
  readonly status: number;
  readonly details: unknown;

  constructor(message: string, status: number, details?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

export type ApiRequestOptions = RequestInit & { json?: unknown };

/** Relative paths are appended to the API base; JSON and FormData are both supported. */
export async function apiRequest<T = unknown>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T | undefined> {
  const { json, ...request } = options;
  if (json !== undefined && request.body != null) {
    throw new TypeError("Usá json o body, pero no ambos en la misma petición.");
  }
  const headers = new Headers(request.headers);
  if (!headers.has("Accept")) headers.set("Accept", "application/json");
  if (json !== undefined) headers.set("Content-Type", "application/json");

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}/${path.replace(/^\/+/, "")}`, {
      cache: "no-store",
      ...request,
      headers,
      body: json !== undefined ? JSON.stringify(json) : request.body,
    });
  } catch (error) {
    if (request.signal?.aborted || (error instanceof Error && error.name === "AbortError"))
      throw error;
    throw new ApiError("No pudimos conectar con el servidor. Intentá nuevamente.", 0);
  }

  const text = await response.text();
  let data: unknown = text || undefined;
  const isJson = response.headers.get("Content-Type")?.includes("json");
  if (text && isJson) {
    try {
      data = JSON.parse(text);
    } catch {
      throw new ApiError("El servidor devolvió una respuesta JSON inválida.", response.status);
    }
  }

  if (!response.ok) {
    let message = "No se pudo completar la operación. Intentá nuevamente.";
    if (data && typeof data === "object") {
      const detail = data as Record<string, unknown>;
      const candidate = [detail.message, detail.detail, detail.error].find(
        (value) => typeof value === "string" && value.trim(),
      );
      if (typeof candidate === "string") message = candidate;
    }
    throw new ApiError(message, response.status, data);
  }
  return data as T | undefined;
}
