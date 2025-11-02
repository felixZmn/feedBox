/**
 * Transforms a date string into a relative time format, e.g. "5m", "2h", "3d".
 * If the date is in the future, prefixes with a '-'.
 * @param {*} dateStr the date string to transform
 * @returns {string|null} the relative time string or null if invalid date
 */
export function getRelativeTime(dateStr) {
  const date = new Date(dateStr);
  if (isNaN(date)) return null;

  const now = new Date();
  let diffMs = date - now;
  const past = diffMs < 0;

  const absMs = Math.abs(diffMs);

  const SECOND = 1000;
  const MINUTE = 60 * SECOND;
  const HOUR = 60 * MINUTE;
  const DAY = 24 * HOUR;
  const MONTH = 30 * DAY;
  const YEAR = 365 * DAY;

  let value, unit;

  if (absMs < MINUTE) {
    value = Math.floor(absMs / SECOND);
    unit = "s";
  } else if (absMs < HOUR) {
    value = Math.floor(absMs / MINUTE);
    unit = "m";
  } else if (absMs < DAY) {
    value = Math.floor(absMs / HOUR);
    unit = "h";
  } else if (absMs < MONTH) {
    value = Math.floor(absMs / DAY);
    unit = "d";
  } else if (absMs < YEAR) {
    value = Math.floor(absMs / MONTH);
    unit = "mo";
  } else {
    value = Math.floor(absMs / YEAR);
    unit = "y";
  }

  // If in the future, prefix with '-'
  return (past ? "" : "-") + value + unit;
}

export function parseDate(dateStr) {
  const date = new Date(dateStr);

  const options = {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "numeric",
    hour12: false,
    minute: "2-digit",
    timeZone: "UTC",
    timeZoneName: "short",
  };
  const formatter = new Intl.DateTimeFormat(undefined, options);
  return formatter.format(date);
}
