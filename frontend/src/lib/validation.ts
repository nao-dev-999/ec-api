export function parsePrice(value: string): number | null {
  const n = Number(value);
  return Number.isFinite(n) && n > 0 ? n : null;
}

export function parseStock(value: string): number | null {
  const n = Number(value);
  return Number.isFinite(n) && n >= 0 ? n : null;
}

export function parseQuantity(value: number): number | null {
  return Number.isFinite(value) && value >= 1 ? value : null;
}
