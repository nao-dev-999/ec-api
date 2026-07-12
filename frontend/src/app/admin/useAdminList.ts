"use client";

import { useCallback, useEffect, useState } from "react";
import { useToast } from "@/app/Toast";
import { getErrorMessage } from "@/lib/errors/messages";

export function useAdminList<T>({
  fetchList,
  deleteItem,
  getId,
  loadErrorMessage,
  deleteSuccessMessage,
  deleteErrorFallback = "削除に失敗しました",
}: {
  fetchList: () => Promise<T[]>;
  deleteItem: (id: number) => Promise<void>;
  getId: (item: T) => number;
  loadErrorMessage: string;
  deleteSuccessMessage: (item: T) => string;
  deleteErrorFallback?: string;
}) {
  const { showToast } = useToast();
  const [items, setItems] = useState<T[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<T | null>(null);

  useEffect(() => {
    fetchList()
      .then(setItems)
      .catch((err) => setError(getErrorMessage(err, loadErrorMessage)));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleDelete = useCallback(async () => {
    if (!deleteTarget) return;
    const id = getId(deleteTarget);
    try {
      await deleteItem(id);
      setItems((prev) => (prev ? prev.filter((i) => getId(i) !== id) : prev));
      showToast(deleteSuccessMessage(deleteTarget));
    } catch (err) {
      showToast(getErrorMessage(err, deleteErrorFallback), "error");
    } finally {
      setDeleteTarget(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deleteTarget]);

  return { items, error, deleteTarget, setDeleteTarget, handleDelete };
}
