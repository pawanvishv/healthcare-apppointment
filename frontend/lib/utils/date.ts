/**
 * Parse API date values. Backend may send ISO-8601 strings or epoch seconds.
 */
export function parseApiDate(value: string | number): Date {
  if (typeof value === "number") {
    // Epoch seconds (values before year 2001 in ms range)
    return new Date(value < 1e12 ? value * 1000 : value);
  }

  if (/^\d+(\.\d+)?$/.test(value)) {
    const num = parseFloat(value);
    return new Date(num < 1e12 ? num * 1000 : num);
  }

  return new Date(value);
}

export function formatDateTime(value: string | number): string {
  const date = parseApiDate(value);
  if (isNaN(date.getTime())) return "—";
  return date.toLocaleString(undefined, {
    weekday: "short",
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatDate(value: string | number): string {
  const date = parseApiDate(value);
  if (isNaN(date.getTime())) return "—";
  return date.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

export function formatTime(value: string | number): string {
  const date = parseApiDate(value);
  if (isNaN(date.getTime())) return "—";
  return date.toLocaleTimeString(undefined, {
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function toDateInputValue(date: Date = new Date()): string {
  return date.toISOString().split("T")[0];
}
