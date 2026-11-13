# Feature Specification: Context Menu Touch and Placement

**Feature Branch**: `004-context-menu-touch`

**Created**: 2026-11-13

**Status**: Draft

**Input**: User description: "the context menu on the feeds, the folders and the profile. please rework the spacing between the labels, so that they are not too small for beeing tapped with fingers. also, please ensure that they open on a consistent location, and not jumping around depending on if the trigger was clicked a few pixels left or right"

## Clarifications

### Session 2026-11-13

- Q: How should menu item spacing balance finger-friendly taps with a still-dense look? → A: Modest bump: still dense, just enough for reliable finger taps (not full fingertip-sized roomy rows; not viewport-dependent density)
- Q: When a feed or folder options menu opens, where should it sit relative to the options control? → A: Below the control; leading edges aligned (Profile keeps end/trailing align)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Tap menu items comfortably (Priority: P1)

A signed-in reader opens the options menu on a feed, a folder, or Profile (and the other menus that share the same chrome menu pattern). Each action label is modestly taller than today’s tight rows so a finger can land on the intended item more reliably, while the menu still reads as dense chrome—not a sparse list with large empty gaps. Spacing feels consistent across those menus.

**Why this priority**: On touch devices, overly tight menu rows cause mis-taps. A small spacing increase improves hit rate without abandoning the compact look of the shell.

**Independent Test**: On a touch-sized viewport (or with a finger-sized pointer), open the feed options menu, the folder options menu, and the Profile menu. Confirm each visible action row is slightly easier to tap than before, the overall menu still looks dense (no large empty padding), and every existing action still appears and works.

**Acceptance Scenarios**:

1. **Given** a signed-in reader looking at the feed list, **When** the reader opens the options menu for a feed, **Then** each action label has a modestly increased tap target, adjacent items remain visually dense (not large empty gaps), and a finger can select one item without easily hitting another.
2. **Given** a folder in the feed list, **When** the reader opens the folder options menu, **Then** its action labels use the same modest, dense spacing as the feed options menu.
3. **Given** the page header, **When** the reader opens Profile, **Then** its action labels use the same modest, dense spacing as the feed and folder menus.
4. **Given** any of those menus is open, **When** the reader taps or clicks an action, **Then** that action behaves as it does today (no new or removed commands).

---

### User Story 2 - Open menus in a stable place (Priority: P1)

When the reader activates the same options control more than once, the menu appears in the same place relative to that control. Shifting the press a few pixels left or right inside the control does not move the menu. Feed and folder options menus open just below their control with leading edges aligned. Profile opens just below Profile with trailing edges aligned. Placement stays consistent for those menus.

**Why this priority**: Menus that jump with click coordinates feel unreliable and make muscle memory impossible. Stable anchoring is as important as tap comfort for touch and mouse users.

**Independent Test**: Open a feed options menu by pressing near the left edge of the options control, close it, then open it again by pressing near the right edge of the same control. Confirm the menu sits below the control with matching leading edges both times. Repeat for a folder options control. For Profile, confirm it sits below with matching trailing edges.

**Acceptance Scenarios**:

1. **Given** a feed options control, **When** the reader opens the menu from different points inside that control, **Then** the menu appears just below the control with its leading edge aligned to the control’s leading edge each time (not at the exact press coordinates).
2. **Given** a folder options control, **When** the reader opens the menu repeatedly from different points inside that control, **Then** the menu appears just below the control with leading edges aligned each time.
3. **Given** the Profile control, **When** the reader opens Profile repeatedly from different points inside that control, **Then** the menu appears just below Profile with its trailing edge aligned to Profile’s trailing edge each time.
4. **Given** a menu open near the edge of the viewport, **When** the menu would otherwise leave the screen, **Then** it stays fully visible while remaining as close as possible to its normal below-trigger anchor.

---

### User Story 3 - Keep keyboard and existing actions intact (Priority: P2)

Readers who use only the keyboard can still open a menu, move through items, activate an item, and dismiss with Escape, with focus returning to the control that opened the menu. Menu content and order for feeds, folders, and Profile stay the same as today.

**Why this priority**: Spacing and placement must not regress accessibility or the actions people already rely on.

**Independent Test**: With keyboard only, open feed options, move through items, Escape to close and confirm focus returns. Repeat for folder options and Profile. Spot-check that listed actions match today’s menus.

**Acceptance Scenarios**:

1. **Given** keyboard focus on a feed or folder options control or on Profile, **When** the reader opens the menu, **Then** the first actionable item can receive focus and arrow keys move between items.
2. **Given** an open menu, **When** the reader presses Escape, **Then** the menu closes and focus returns to the control that opened it.
3. **Given** feed, folder, and Profile menus, **When** the reader compares their actions to today’s product, **Then** the available commands and their order are unchanged.

---

### Edge Cases

- What happens if spacing were enlarged to full fingertip-sized rows? That is out of scope; oversized empty padding between labels is a failure for this feature.
- What happens when the trigger sits near the bottom or trailing edge of the viewport so a full-height or full-width menu would overflow? The menu stays on screen and still anchors to the trigger as consistently as possible.
- How does the system handle menus of different lengths (short “All feeds” vs longer feed or Profile menus)? Placement rules stay the same; only height or width differs.
- What happens when the same trigger is used with a mouse versus a finger? Spacing and stable placement apply in both cases.
- What happens if the reader opens one menu while another would have been open? Only one chrome options menu is visible at a time, as today.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Context menus for feeds, folders, and Profile MUST present action labels with a modest increase in spacing and hit area so finger taps are more reliable, while the menu remains visually dense (no large empty gaps or sparse, roomy rows).
- **FR-002**: Spacing between consecutive action labels in those menus MUST be consistent across feed, folder, and Profile menus, and MUST use the same density on all viewport sizes (not a separate touch-only layout).
- **FR-003**: Opening a feed, folder, or Profile menu MUST position the menu relative to the trigger control’s geometry, not relative to the exact press or click coordinates inside that control.
- **FR-004**: Reopening the same trigger MUST place the menu at the same origin relative to that trigger (modulo viewport clamping when space is limited).
- **FR-005**: Feed and folder options menus (and All feeds / Add when using the same pattern) MUST open just below the trigger with the menu’s leading edge aligned to the trigger’s leading edge; Profile MUST open just below Profile with the menu’s trailing edge aligned to Profile’s trailing edge.
- **FR-006**: Menus MUST remain fully within the visible viewport when the preferred anchor would overflow.
- **FR-007**: Existing menu actions, labels, separators, and status lines for feeds, folders, and Profile MUST remain available and behave as they do today.
- **FR-008**: Keyboard open, navigate, activate, and dismiss (including returning focus to the trigger) MUST continue to work for these menus.
- **FR-009**: Other chrome menus that share the same options-menu pattern (All feeds options and Add) MUST receive the same spacing and the same below-trigger leading-edge placement so behavior stays consistent across the shell.

### Key Entities

- **Options trigger**: The control the reader activates to open a menu (feed options, folder options, Profile, and related chrome controls that share the pattern).
- **Context menu**: The overlay list of actions (and optional status or separators) that appears when a trigger is activated.
- **Menu item**: A single tappable or clickable action label inside the menu.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Each actionable menu item in feed, folder, and Profile menus is modestly taller than the previous tight rows and still reads as dense chrome (no large empty padding between labels), verifiable by side-by-side comparison with today’s menus.
- **SC-002**: Opening the same options control ten times from different points inside the control yields the same below-trigger origin every time (leading-edge align for feed/folder/All feeds/Add; trailing-edge align for Profile), with no drift from press position.
- **SC-003**: In a short usability check on a phone-sized viewport, readers can select the intended menu action on the first tap in at least 9 of 10 attempts without correcting for a neighboring item, without the menu looking sparse or oversized.
- **SC-004**: After the change, every previously available feed, folder, and Profile menu action still completes successfully in a manual regression pass.
- **SC-005**: Keyboard-only users can open, move through, activate, and dismiss these menus without loss of focus or stuck overlays.

## Assumptions

- “All feeds” options and the Add control share the same chrome menu system as feed, folder, and Profile; they get the same spacing and placement fixes even though the request named only feeds, folders, and Profile.
- No new menu commands are added and none are removed; this feature is spacing and placement only.
- Article-list or reader-specific menus (if any) outside this chrome options pattern are out of scope.
- Tap comfort is a modest bump over today’s dense menu rows, not a full fingertip-minimum (~44 display points) enlargement that would add obvious empty whitespace.
- Feed, folder, All feeds, and Add menus open below the trigger with leading-edge alignment; Profile opens below with trailing-edge (end) alignment.
- Visual density of the feed tree rows themselves is unchanged; only the overlay menus are reworked, and those menus stay dense rather than switching to a roomy touch layout.
