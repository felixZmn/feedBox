import assert from "node:assert/strict";
import test from "node:test";

import { placeContextMenu } from "../../main/resources/META-INF/resources/scripts/menu-placement.js";

const trigger = { left: 100, right: 132, top: 40, bottom: 72 };
const menu = { menuWidth: 160, menuHeight: 120 };
const viewport = { width: 800, height: 600 };

test("start align places menu below trigger with leading edges aligned", () => {
  const { left, top } = placeContextMenu({
    triggerRect: trigger,
    ...menu,
    viewport,
    align: "start",
    gap: 4,
    edge: 8,
  });
  assert.equal(left, 100);
  assert.equal(top, 76);
});

test("end align places menu below trigger with trailing edges aligned", () => {
  const wideEnough = { left: 400, right: 432, top: 40, bottom: 72 };
  const { left, top } = placeContextMenu({
    triggerRect: wideEnough,
    ...menu,
    viewport,
    align: "end",
    gap: 4,
    edge: 8,
  });
  assert.equal(left, 432 - 160);
  assert.equal(top, 76);
});

test("same trigger geometry yields same origin regardless of press point", () => {
  const a = placeContextMenu({
    triggerRect: trigger,
    ...menu,
    viewport,
    align: "start",
  });
  const b = placeContextMenu({
    triggerRect: { ...trigger },
    ...menu,
    viewport,
    align: "start",
  });
  assert.deepEqual(a, b);
});

test("clamps left when start align would overflow the right edge", () => {
  const nearRight = { left: 700, right: 740, top: 40, bottom: 72 };
  const { left } = placeContextMenu({
    triggerRect: nearRight,
    menuWidth: 160,
    menuHeight: 120,
    viewport,
    align: "start",
    edge: 8,
  });
  assert.equal(left, 800 - 160 - 8);
});

test("clamps left when end align would overflow the left edge", () => {
  const nearLeft = { left: 10, right: 40, top: 40, bottom: 72 };
  const { left } = placeContextMenu({
    triggerRect: nearLeft,
    menuWidth: 160,
    menuHeight: 120,
    viewport,
    align: "end",
    edge: 8,
  });
  assert.equal(left, 8);
});

test("clamps top when menu would overflow the bottom edge", () => {
  const nearBottom = { left: 100, right: 132, top: 520, bottom: 552 };
  const { top } = placeContextMenu({
    triggerRect: nearBottom,
    menuWidth: 160,
    menuHeight: 120,
    viewport,
    align: "start",
    gap: 4,
    edge: 8,
  });
  assert.equal(top, 600 - 120 - 8);
});
