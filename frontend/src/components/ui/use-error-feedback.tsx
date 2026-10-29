"use client";

import { useCallback, useState } from "react";
import { useToast } from "react-ui-lib";
import { ApiError } from "@/service/api";
import { RequestError } from "./request-error";

type ErrorOptions = {
  title: string;
  fallback: string;
  retry?: () => void;
  retryLabel?: string;
  context?: string;
};

type RetryNotice = {
  title: string;
  description: string;
  retry: () => void;
  retryLabel?: string;
};

/** Todo error genera un toast; los transitorios conservan un reintento en el layout. */
export function useErrorFeedback({ retrying = false }: { retrying?: boolean } = {}) {
  const { error: showToast } = useToast();
  const [notice, setNotice] = useState<RetryNotice | null>(null);
  const clearError = useCallback(() => setNotice(null), []);

  const reportError = useCallback((error: unknown, options: ErrorOptions) => {
    const message = error instanceof Error ? error.message : options.fallback;
    const description = [message, options.context].filter(Boolean).join(" ");
    showToast(options.title, description);

    const transient = !(error instanceof ApiError)
      || error.status === 0
      || error.status === 408
      || error.status === 429
      || error.status >= 500;

    if (options.retry && transient) {
      setNotice({
        title: options.title,
        description,
        retry: options.retry,
        retryLabel: options.retryLabel,
      });
    } else {
      setNotice(null);
    }
  }, [showToast]);

  return {
    reportError,
    clearError,
    hasRetryError: notice !== null,
    feedback: notice && (
      <RequestError
        title={notice.title}
        description={notice.description}
        retryLabel={notice.retryLabel}
        retrying={retrying}
        onRetry={notice.retry}
      />
    ),
  };
}
