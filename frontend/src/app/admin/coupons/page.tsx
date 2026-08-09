"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  Ticket,
  Plus,
  PencilLine,
  Trash2,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import {
  getAdminCoupons,
  deleteAdminCoupon,
  type AdminCoupon,
} from "@/lib/api/adminCoupons";
import ConfirmModal from "../ConfirmModal";
import { useToast } from "@/app/Toast";
import { getErrorMessage } from "@/lib/errors/messages";

const PAGE_SIZE = 20;

function formatDate(value: string | null | undefined) {
  if (!value) return "-";
  return new Date(value).toLocaleDateString("ja-JP");
}

export default function AdminCouponsPage() {
  const { showToast } = useToast();
  const [coupons, setCoupons] = useState<AdminCoupon[] | null>(null);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminCoupon | null>(null);

  useEffect(() => {
    getAdminCoupons(page - 1, PAGE_SIZE)
      .then((result) => {
        setCoupons(result.content ?? []);
        setTotalPages(Math.max(1, result.totalPages ?? 1));
        setTotalElements(result.totalElements ?? 0);
      })
      .catch((err) =>
        setError(getErrorMessage(err, "クーポン一覧の取得に失敗しました")),
      );
  }, [page]);

  async function handleDelete() {
    if (!deleteTarget) return;
    try {
      await deleteAdminCoupon(deleteTarget.id);
      setCoupons((prev) => prev!.filter((c) => c.id !== deleteTarget.id));
      setTotalElements((n) => Math.max(0, n - 1));
      showToast(`クーポン「${deleteTarget.code}」を削除しました`);
    } catch (err) {
      showToast(getErrorMessage(err, "削除に失敗しました"), "error");
    } finally {
      setDeleteTarget(null);
    }
  }

  if (error) return <p style={{ padding: 24, color: "red" }}>{error}</p>;
  if (coupons === null) return <p style={{ padding: 24 }}>読み込み中...</p>;

  return (
    <main>
      <div className="page-heading">
        <h1>
          <Ticket size={22} />
          クーポン管理
        </h1>
        <span className="page-count">全 {totalElements} 件</span>
      </div>

      <Link href="/admin/coupons/new" className="btn-primary">
        <Plus size={16} />
        新規クーポン
      </Link>

      <ul className="admin-list">
        {coupons.length === 0 && (
          <p style={{ padding: "16px 0", color: "var(--muted)" }}>
            クーポンはまだありません
          </p>
        )}
        {coupons.map((coupon) => (
          <li key={coupon.id}>
            <div className="card-head">
              <div>
                <strong>{coupon.code}</strong>
                <p className="card-desc" style={{ marginTop: 2 }}>
                  ¥{coupon.discountAmount.toLocaleString()} 引き
                  {" / "}
                  利用: {coupon.usageCount}
                  {coupon.usageLimit != null ? ` / ${coupon.usageLimit}` : ""}
                  {" / "}
                  {coupon.active ? "有効" : "無効"}
                </p>
                <p className="card-desc" style={{ marginTop: 2 }}>
                  期間: {formatDate(coupon.validFrom)} 〜{" "}
                  {formatDate(coupon.validTo)}
                </p>
              </div>
              <span className="card-updated">
                {formatDate(coupon.createdAt)}
              </span>
            </div>

            <div className="card-divider" />

            <div className="card-footer">
              <span />
              <span className="item-actions">
                <Link
                  href={`/admin/coupons/${coupon.id}`}
                  className="icon-btn"
                  title="編集"
                >
                  <PencilLine size={16} />
                </Link>
                <button
                  className="icon-btn"
                  title="削除"
                  onClick={() => setDeleteTarget(coupon)}
                >
                  <Trash2 size={16} />
                </button>
              </span>
            </div>
          </li>
        ))}
      </ul>

      <div className="pagination">
        <span className="page-count">
          {coupons.length === 0 ? 0 : (page - 1) * PAGE_SIZE + 1}〜
          {Math.min(page * PAGE_SIZE, totalElements)} 件 / 全{totalElements} 件
        </span>
        <div className="page-buttons">
          <button
            className="page-btn"
            disabled={page <= 1}
            onClick={() => setPage((p) => p - 1)}
          >
            <ChevronLeft size={16} />
          </button>
          {Array.from({ length: totalPages }, (_, i) => i + 1).map((n) => (
            <button
              key={n}
              className={"page-btn" + (n === page ? " active" : "")}
              onClick={() => setPage(n)}
            >
              {n}
            </button>
          ))}
          <button
            className="page-btn"
            disabled={page >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            <ChevronRight size={16} />
          </button>
        </div>
      </div>

      <ConfirmModal
        open={deleteTarget !== null}
        title="クーポンを削除"
        message={`クーポン「${deleteTarget?.code}」を削除しますか？この操作は取り消せません。`}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </main>
  );
}
