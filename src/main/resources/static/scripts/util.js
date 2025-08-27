import { dialogType } from "./main.js";

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

export function buildUrlParams(params) {
  if (!params || typeof params !== "object") return "";
  const encodedParams = [];

  for (let param of params) {
    if (param[0] != null && param[1] != null) {
      encodedParams.push(
        `${encodeURIComponent(param[0])}=${encodeURIComponent(param[1])}`
      );
    }
  }
  if (encodedParams.length == 0) {
    return "";
  }
  return "?" + encodedParams.join("&");
}

export function hideDialog() {
  document.getElementById("modal").style.display = "none";
  document.getElementById("folder-add-edit").style.display = "none";
  document.getElementById("feed-add-edit").style.display = "none";
}

export function showDialog(type) {
  switch (type) {
    case dialogType.ADD_FOLDER:
      document.getElementById("modal-headline").textContent = "Add Folder";
      document.getElementById("folder-add-edit").style.display = "grid";
      break;
    case dialogType.EDIT_FOLDER:
      document.getElementById("modal-headline").textContent = "Edit Folder";
      document.getElementById("folder-add-edit").style.display = "grid";
      break;
    case dialogType.ADD_FEED:
      document.getElementById("modal-headline").textContent = "Add Feed";
      document.getElementById("feed-add-edit").style.display = "grid";
      break;
    case dialogType.EDIT_FEED:
      document.getElementById("modal-headline").textContent = "Edit Feed";
      document.getElementById("feed-add-edit").style.display = "grid";
      break;

    default:
      break;
  }

  document.getElementById("modal").style.display = "block";
}
