const STATUS_LABEL: Record<string, string> = {
  PENDING: "処理待ち",
  CONFIRMED: "確認済み",
  SHIPPED: "発送済み",
  DELIVERED: "配達完了",
  CANCELLED: "キャンセル",
};

const STATUS_CLASS: Record<string, string> = {
  PENDING: "status-pending",
  CONFIRMED: "status-confirmed",
  SHIPPED: "status-shipped",
  DELIVERED: "status-delivered",
  CANCELLED: "status-cancelled",
};

export default function OrderStatusBadge({
  status,
}: {
  status: string | undefined;
}) {
  if (!status) return null;
  const cls = STATUS_CLASS[status] ?? "status-pending";
  const label = STATUS_LABEL[status] ?? status;

  return (
    <span className={`status-badge ${cls}`}>
      <span className="dot" />
      {label}
    </span>
  );
}
