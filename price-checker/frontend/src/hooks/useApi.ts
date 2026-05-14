import { useState, useEffect, useCallback } from "react";

const BASE = (import.meta.env.VITE_API_URL ?? "") + "/api";

async function apiFetch<T>(path: string): Promise<T> {
  const res = await fetch(BASE + path);
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json() as Promise<T>;
}

export function useGet<T>(path: string) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    apiFetch<T>(path)
      .then(setData)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [path]);

  useEffect(() => {
    load();
  }, [load]);

  return { data, loading, error, reload: load };
}

export async function triggerSync(): Promise<void> {
  await fetch(BASE + "/sync/trigger", { method: "POST" });
}
