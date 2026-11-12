# Feature Specification: Unified Page Header

**Feature Branch**: `002-unified-page-header`

**Created**: 2026-11-11

**Status**: Draft

**Input**: User description: "Replace the separate header row on each column with one header bar across the full page. On the left, the FeedBox icon is enough. The middle stays clear. On the right, a Profile menu holds everything that belongs there, including sign out, and the current header More menu moves into it. Refresh sits just to the left of Profile. In a single-column layout the same header also carries the back control, so the reader is never stuck in one view. Reader controls move into the reader column, under the global header, because opening the original article only matters while reading."

## Clarifications

### Session 2026-11-12

- Q: Should the page header be one continuous bar above the three panes, with a single background, rather than three header strips that only look joined? → A: One continuous bar above all panes, single background
- Q: When no article is open, should the reader column still show its action header (Save, Open, Previous, Next)? → A: Empty until an article is selected; opening an article loads the action header and the article together
- Q: What should the Profile control look like in the page header? → A: Person/account icon with accessible name "Profile"
- Q: Should the Profile control show the identity provider’s profile picture when one is available? → A: Fixed person icon only (no IdP photo in this feature)
- Q: Where should article search live in the signed-in shell? → A: In the page-header middle (supersedes earlier “article list only” note)
- Q: How should Save, Open in browser, Previous, and Next appear when an article is open? → A: In-article toolbar at the top of the article stack (icons, then title/meta/body); scrolls with the article
- Q: When should the FeedBox mark appear in the page header? → A: Hide the mark when the back control is shown, and hide it on narrow viewports (about ≤700px); show it otherwise
- Q: Should the Profile menu use hairline separators between its action groups? → A: Same four items, with hairline separators after Export OPML and after Keyboard shortcuts
- Q: How should the open Profile menu line up with the Profile control? → A: Align the menu’s trailing edge with Profile’s trailing edge (end-aligned), keeping the menu fully on screen
- Q: What surface model should the signed-in shell use so panes and the page header feel like one app, not bolted-together boxes? → A: Page header and feed/folder sidebar share one surface; article list and reader share one surface
- Q: Which of those two surfaces should sit recessed relative to the other? → A: Chrome (header + feed sidebar) recessed; content (article list + reader) elevated
- Q: When only the article list or only the reader is on screen (one-pane or medium reader-full), which surface should fill that pane? → A: Article list and reader always use the elevated content surface; page header always uses recessed chrome (even with no sidebar)
- Q: How should the article-list search field sit relative to surfaces? → A: Search stays in its current page-header location; it uses the header’s recessed chrome surface (no separate search-strip fill on the article list)
- Q: How should article titles look in the article list versus in the open reader? → A: List titles regular weight; reader headline bold and larger than the list title
- Q: How dense should the feed/folder tree and its context menus be? → A: Comfortable tree rows (about one row under the pointer); context menus use the same compact chrome density as the tree
- Q: How large should the page-header icons (Refresh, Profile, mark, back) be relative to the feed tree icons? → A: Header icons at the original larger size; tree icons slightly smaller
- Q: Should the page header keep the taller bar height, or shrink to match the denser panes below? → A: Keep the taller page-header height (current “large” bar); do not shrink it to match tree density

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find app actions in one page header (Priority: P1)

A signed-in reader sees a single continuous header across the full width of the page, above the feed list, the article list, and the reader. The page header and the feed/folder sidebar share one chrome surface so the left side reads as continuous navigation. The article list and the reader share one content surface so those two panes read as one reading area, separated by a hairline rather than different fills. The header is not painted as three column-colored strips. On a wide or medium layout with no back control, the leading side shows only the FeedBox mark. On a narrow viewport, or whenever the back control is shown, the mark is hidden so the header stays uncrowded. The middle holds the article search field. The trailing side shows Refresh feeds, then Profile. Profile opens a menu with Import OPML, Export OPML, Keyboard shortcuts, and Sign Out, in that order, with hairline separators after Export OPML and after Keyboard shortcuts. The More button that used to sit in the feed-list header is gone. The feed list itself has no header bar; folders and feeds begin directly under the page header. The article list has no search bar of its own.

**Why this priority**: The three column headers currently pretend to be one bar, but app actions are pinned to the feed list. A reader looking at an article cannot see refresh or sign out without knowing they live in another column. One page header makes those actions available everywhere.

**Independent Test**: Sign in on a wide screen. Confirm one header spans the page, the FeedBox mark is on the left, search sits in the header middle on the chrome surface, and Refresh feeds sits immediately left of Profile. Open Profile and run each of its four actions. Confirm the feed-list header and its More button are gone, the article list has no search strip, search still filters the article list, and the option menus on All Feeds, folders, and feeds are unchanged.

**Acceptance Scenarios**:

1. **Given** a signed-in reader on a wide screen, **When** the reader looks at the top of the page, **Then** one continuous taller page header spans the full width above every pane with the same recessed chrome surface as the feed/folder sidebar, the article list and reader share one elevated content surface, the FeedBox mark is the only thing on the leading side at the larger header icon size, search sits in the header middle, Refresh feeds is immediately left of Profile, and Profile is a person/account icon (not an identity-provider photo).
1a. **Given** a narrow viewport, or a layout where the page-header back control is shown, **When** the reader looks at the leading side, **Then** the FeedBox mark is not shown.
2. **Given** the page header, **When** the reader opens Profile, **Then** the menu offers Import OPML, Export OPML, Keyboard shortcuts, and Sign Out, in that order, with a hairline separator after Export OPML and after Keyboard shortcuts, and no other actions.
3. **Given** Profile is open, **When** the reader chooses Sign Out, **Then** the session ends the same way it does today.
4. **Given** Profile is open, **When** the reader chooses Import OPML, Export OPML, or Keyboard shortcuts, **Then** that action behaves as it does today.
5. **Given** the feed list, **When** the reader looks above it, **Then** there is no feed-list header and no More button. All Feeds, Unread, Saved, folders, and feeds begin under the page header.
6. **Given** an option button on All Feeds, a folder, or a feed, **When** the reader opens it, **Then** that row menu still offers the same actions it does today.
7. **Given** the page header, **When** the reader searches, **Then** search is in the header middle on the chrome surface, still filters the current article list, can still be cleared, and the article list has no search bar of its own.
8. **Given** a refresh is not already running, **When** the reader activates Refresh feeds, **Then** every subscription refreshes, progress is shown on that control, and the control returns to idle when the refresh finishes.
9. **Given** a refresh is already running, **When** the reader activates Refresh feeds again, **Then** a second refresh does not start and the in-progress indication stays until the current refresh finishes.
10. **Given** a refresh fails, **When** the failure is reported, **Then** the reader sees the same kind of error as today and Refresh feeds returns to idle.
11. **Given** the reader uses only the keyboard, **When** focus moves to Refresh feeds or Profile, **Then** focus is visible and each control has a name. Profile can be opened, moved through, and activated from the keyboard. Escape closes Profile and returns focus to Profile.
12. **Given** the reader is signed out, **When** the sign-in screen is shown, **Then** that screen is unchanged and does not gain this header.

---

### User Story 2 - Keep article actions with the article (Priority: P1)

When no article is selected, the reader column is empty: no article actions and no article body. Choosing an article loads one article stack in the reader column: a left-aligned icon toolbar (Save article, Open in browser, Previous article, Next article), then the headline, then the source/publisher line, then the date/author line, then a hairline divider, then the article body. Those actions belong to the open article, not to app chrome. They are not a second sticky header under the page header; they scroll away with the article. The page header remains the only sticky app chrome above the panes.

**Why this priority**: Pulling Open in browser into the page header would make a per-article action look global. A second sticky bar over the reader would look like another strip of app chrome. Embedding the actions in the article stack keeps them tied to the open article and leaves a single continuous page header.

**Independent Test**: With the page header in place and nothing selected, confirm the reader column is empty. Open an article on a wide screen and confirm the icon toolbar appears at the top of the article stack (then title, source, date, divider, body), with Save, Open in browser, Previous, and Next. Confirm those four actions are absent from the page header and from the feed list. Scroll the article and confirm the toolbar scrolls with it while the page header stays put. Save an article, open its original page, and move to the previous and next articles.

**Acceptance Scenarios**:

1. **Given** an article is open on a wide screen, **When** the reader looks at the reader column, **Then** the page header is the only sticky app chrome, and the open article begins with a left-aligned icon toolbar of Save article, Open in browser, Previous article, and Next article, followed by a bold, larger headline, then source/publisher, date/author, a divider, and the body.
1a. **Given** the article list and an open article, **When** the reader compares the list title to the reader headline, **Then** the list title is regular weight and smaller than the bold reader headline.
2. **Given** the page header, the feed list, and the article list, **When** the reader looks at them, **Then** Save article, Open in browser, Previous article, and Next article are not shown there.
3. **Given** an open article, **When** the reader activates Save article, **Then** the article is saved or unsaved exactly as it is today, and the button name switches between "Save article" and "Remove from Saved".
4. **Given** an open article with a usable original page, **When** the reader activates Open in browser, **Then** that page opens as it does today.
5. **Given** an open article with no usable original page, **When** the reader looks at the article toolbar, **Then** Open in browser is not offered.
6. **Given** no article is open, **When** the reader looks at the reader column, **Then** that column is empty: no article toolbar, no article body, and no Save, Open in browser, Previous, or Next controls.
7. **Given** no article is open, **When** the reader chooses an article from the list, **Then** the article toolbar and the article content appear together in the reader column as one stack.
8. **Given** an article in a list, **When** the reader activates Previous article or Next article, **Then** the reader moves through that list as it does today, including at the first and last article.
9. **Given** the reader uses only the keyboard, **When** focus is on an article-toolbar control, **Then** focus is visible and the control's name matches its action. Existing keyboard shortcuts for refresh, save, previous, and next still perform those actions.
10. **Given** the reader scrolls a long open article, **When** the article moves, **Then** the page header stays on screen and the article toolbar scrolls with the article.
11. **Given** an open article on a wide screen, **When** the article is closed so none is selected, **Then** the article toolbar and the article content both leave, and the reader column is empty again.

---

### User Story 3 - Leave a one-pane view from the page header (Priority: P1)

When the layout shows only one pane, and that pane is not the feed list, the page header includes a back control on the leading side (the FeedBox mark is hidden while back is shown). From the article list it returns to the feed list. From the reader it returns to the article list. The in-article toolbar does not grow a second back control. When the feed list is on screen beside another pane, or is the only pane, the page header has no back control.

**Why this priority**: Moving the column back controls into the page header is what keeps a narrow layout usable. Without that control, opening the article list or an article would leave no visible way back.

**Independent Test**: On a narrow screen, open the feed list, then the article list, then an article. Use the page-header back control to return one pane at a time. Repeat on the medium width where the reader occupies the whole page. On a wide screen, confirm the page header has no back control and the reader stays beside the article list.

**Acceptance Scenarios**:

1. **Given** a narrow layout showing only the feed list, **When** the reader looks at the page header, **Then** there is no back control, and Refresh feeds and Profile are still available.
2. **Given** a narrow layout showing only the article list, **When** the reader activates the page-header back control, **Then** the feed list is shown and the control's name is "Back to feeds".
3. **Given** a narrow layout showing only the reader, **When** the reader activates the page-header back control, **Then** the article list is shown and the control's name is "Back to articles".
4. **Given** the reader is the only pane, **When** the reader looks at the open article stack, **Then** its toolbar has Save article, Open in browser when available, Previous article, and Next article, and it has no back control.
5. **Given** the medium layout with the feed list and the article list side by side, **When** the reader looks at the page header, **Then** there is no back control.
6. **Given** the medium layout with the reader occupying the whole page, **When** the reader activates the page-header back control, **Then** the article list returns, with the feed list beside it, as it does today.
7. **Given** the wide layout with all three panes visible, **When** the reader looks at the page header, **Then** there is no back control, and closing the article is unnecessary because the list remains visible.
8. **Given** a narrow layout, **When** the reader uses only the keyboard, **Then** the back control, when present, is reachable, shows a visible focus, and has the name of the pane it returns to.
9. **Given** a phone with a system status area or a home indicator, **When** the page header and the panes are shown, **Then** the header and the pane contents stay clear of those system areas.
10. **Given** a narrow or medium layout where the sidebar is hidden and only the article list or only the reader is shown, **When** the reader looks at surfaces, **Then** that pane still uses the elevated content surface and the page header still uses the recessed chrome surface.

---

### Edge Cases

- Profile stays fully on screen beside the Profile control. The open menu’s trailing edge aligns with Profile’s trailing edge (end-aligned). Its menu uses hairline separators after Export OPML and after Keyboard shortcuts (not after every item). Choosing an item, pressing Escape, or activating somewhere outside the menu closes it. Escape and an outside activation return focus to Profile.
- A second Refresh feeds activation during a refresh does not start another refresh. A failed refresh reports the failure and returns the control to idle.
- When no article is open, the reader column stays empty (no article toolbar and no article). Opening an article brings the toolbar and the article stack together; closing the selection removes both. Open in browser is absent when an open article has no usable original page.
- Previous article and Next article keep their current behavior at the first and last item of the current list.
- Search stays usable while the page header is present, including an empty query, a query with no matches, and clearing the query.
- The "Can't reach FeedBox" notice stays where it is and is not moved into the page header.
- Light appearance and dark appearance both keep the page header and the in-article toolbar readable. The page header matches the recessed feed/folder sidebar surface; the article list matches the elevated reader surface. Panes are not three differently filled boxes.
- Motion in the header and in Profile follows the reader's reduced-motion preference.
- On a narrow screen, every page-header and article-toolbar control is comfortable to tap, at the same comfort already expected of the reader's other controls.
- The feed/folder tree stays clickable at about one row per pointer. Context menus match that compact chrome density rather than appearing oversized next to the tree.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The signed-in reader has one continuous page header that sits above the feed list, the article list, and the reader (not as three joined column headers), spans the full width, and uses one chrome background across that width, in the wide, medium, and narrow layouts. That chrome background is the same surface as the feed/folder sidebar and is recessed relative to content. The article list and the reader use one shared elevated content surface. The page header keeps the taller comfortable bar height; densifying panes or menus MUST NOT shrink that bar to match tree density.
- **FR-002**: The page header stays on screen while a pane scrolls. The in-article toolbar scrolls with the open article; it is not a second sticky header. Article search lives in the page-header middle and therefore stays on screen with the page header.
- **FR-003**: On layouts where the page-header back control is hidden and the viewport is wider than about 700px, the leading side shows the FeedBox mark already used on the sign-in screen, with no wordmark beside it. The mark is not a control. When the back control is shown, or on a narrow viewport (about ≤700px), the mark is hidden.
- **FR-004**: The middle of the page header holds the article search field. Search sits on the page header’s recessed chrome surface; it does not introduce a separate full-width search-strip fill on the article list.
- **FR-005**: The trailing side of the page header shows Refresh feeds, then Profile. Profile is a fixed person/account icon with the accessible name "Profile". It does not show an identity-provider profile picture in this feature. Refresh feeds is outside the Profile menu. Page-header icons (mark, back, Refresh, Profile, and the search glyph in the header) use the original larger control size; feed/folder tree icons are slightly smaller.
- **FR-006**: Profile opens a menu containing exactly Import OPML, Export OPML, Keyboard shortcuts, and Sign Out, in that order, with a hairline separator after Export OPML and after Keyboard shortcuts. Separators are not actions. Each item does what it does today. The open menu’s trailing edge aligns with Profile’s trailing edge and stays fully on screen.
- **FR-007**: The More button is removed from the feed-list header. The feed list has no header bar of its own.
- **FR-008**: Option menus on All Feeds, folders, and feeds stay on those rows and keep their current actions.
- **FR-009**: Refresh feeds still refreshes every subscription. Its in-progress indication stays on that control. A refresh that is already running is not started again. A failed refresh is reported as it is today, and the control then returns to idle.
- **FR-010**: Search lives in the page-header middle. It still searches the current article list and can still be cleared. The article list has no search bar and no back control of its own.
- **FR-011**: When an article is open, the reader column shows one article stack: a left-aligned icon toolbar (Save article, Open in browser, Previous article, Next article), then a bold headline that is larger than the article-list title, then source/publisher, date/author, a hairline divider, then body. Clear space separates the toolbar from the headline. Those controls have the same availability and effect as today, including the saved and unsaved names of the save control. Choosing an article shows the toolbar and content together. When no article is open, the reader column shows neither the toolbar nor article content. The toolbar is not a sticky second header under the page header.
- **FR-011a**: Article-list titles use regular (non-bold) weight. They MUST NOT match the reader headline’s bold weight.
- **FR-012**: Save article, Open in browser, Previous article, and Next article appear only in the open article's toolbar, and only while an article is open.
- **FR-013**: When the feed list is visible, the page header has no back control. This covers the wide layout, the medium layout while the feed list and article list are side by side, and the narrow layout while the feed list is the only pane.
- **FR-014**: When the article list is the only pane, the page header shows a leading back control named "Back to feeds" that returns to the feed list. The control sits on the leading side (the FeedBox mark is hidden while back is shown).
- **FR-015**: When the reader is the only pane, including the medium layout where the reader occupies the whole page, the page header shows a leading back control named "Back to articles" that returns to the article list. The in-article toolbar has no back control.
- **FR-016**: Refresh feeds, Profile, the back control when it is present, and every in-article toolbar control are keyboard reachable, show a visible focus, and have a name. Profile's menu can be operated from the keyboard. Escape closes it and returns focus to Profile.
- **FR-017**: Existing keyboard shortcuts for refresh, save, previous article, and next article still perform those actions.
- **FR-018**: The page header and the in-article toolbar use the reader's existing type, corner radius, and light and dark appearance. Surfaces follow a two-tone shell: page header + feed/folder sidebar share a recessed chrome surface; article list + reader share an elevated content surface. That assignment does not change when a pane is hidden: article list and reader stay elevated whenever shown; the page header stays recessed chrome even when the sidebar is off-screen. When only the feed list is shown, it uses the recessed chrome surface under the header. The page header keeps one fill across its full width (it does not adopt each column’s color in segments). Column separation uses hairlines, not a third fill. The same recessed-vs-elevated relationship holds in light and dark appearance. Controls are comfortable to tap on a narrow screen. Motion follows the reduced-motion preference. On a phone, the header and the panes stay clear of the system status area and the home indicator.
- **FR-018a**: The feed/folder tree uses comfortable row height so roughly one row sits under a normal pointer (not three). Context menus (Profile and row option menus) use the same compact chrome type size and item padding density as that tree — not a larger “default” menu scale.
- **FR-019**: The sign-in screen is unchanged. The page header is shown only in the signed-in reader.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On every signed-in layout, Refresh feeds and Profile are visible in one continuous page header without scrolling. A reader can tell the header is a single bar above the panes, not three column headers, and that the header continues the sidebar’s chrome surface while the article list and reader share one content surface. Opening Profile takes one action.
- **SC-002**: Signing out takes two actions from the feed list, from the article list, and from an open article: open Profile, then choose Sign Out. This holds on a wide screen and on a one-pane screen.
- **SC-003**: On a one-pane screen, returning from an article to the article list takes one action, and returning from the article list to the feed list takes one action. Both actions are in the page header.
- **SC-004**: On a wide screen with no article selected, the reader column is empty. After opening an article, a reader can point to the in-article toolbar at the top of that article's stack and see that it belongs only to the open article (not a second app header), and that it appeared with the article. The reader headline is clearly bold and larger than the regular-weight title in the article list. Save article, Open in browser, Previous article, and Next article are absent from the page header, the feed list, the article list, and the empty reader.
- **SC-005**: All four actions that live in today's header More menu remain available from Profile, and each completes with the same outcome as today, including a successful import, a successful export, the shortcuts help, and sign out.
- **SC-006**: A keyboard-only reader can reach every control in the page header and the in-article toolbar, see which control has focus, and dismiss Profile with Escape so focus returns to Profile.
- **SC-007**: In light appearance and in dark appearance, the page header and the in-article toolbar remain readable, and the two-surface shell (chrome vs content) stays clear. On a phone, every header and toolbar control is comfortable to tap, and the page header stays clear of the system status area.
- **SC-008**: A reader can open Profile or a feed/folder option menu and see menu type and row padding in the same density band as the feed tree (not comically larger). Tree rows remain individually targetable with a normal pointer.

## Assumptions

- "Refresh all" is the existing Refresh feeds control. It keeps that name and still refreshes every subscription. It moves; it is not renamed.
- "Open source" means the existing Open in browser control, which opens the article's original page. It stays in the reader column because it applies only to the open article.
- The single-column case is every layout in which the article list or the reader is shown without the pane it returns to. That is the narrow one-pane layout, and the medium layout while the reader occupies the whole page. The wide three-pane layout, and the medium layout while the feed list and article list are side by side, have no back control in the page header.
- Back returns one step, using today's destinations: article list to feed list, reader to article list.
- The page header is real app chrome above the three panes: one continuous taller bar with the recessed chrome surface shared by the feed/folder sidebar, not three column headers that only look joined. Pane density changes do not shrink that bar. The article list and reader share the elevated content surface so they do not read as separately painted boxes. Hiding a pane does not recolor the remaining panes or the page header.
- The FeedBox mark identifies the product when shown. Activating it does not navigate, refresh, or open a menu. It is hidden with the back control and on narrow viewports so trailing actions and search keep usable width. Page-header icons stay at the original larger size; tree icons stay slightly smaller.
- The empty reader is a blank column. Article actions are not placeholders and are not a sticky second header; they arrive as the top of the open article stack and leave when none is selected. The open-article order is icons → bold larger headline → source/publisher → date/author → divider → body. Article-list titles stay regular weight.
- Profile uses a fixed person/account icon and the accessible name "Profile". Identity-provider profile pictures are out of scope for this feature. The product has no profile page or display name in the menu. Profile holds the app actions that used to live in the header More menu, with Sign Out last, grouped by hairline separators after the OPML pair and after Keyboard shortcuts. The open menu is end-aligned to the Profile control.
- Search lives in the page-header middle on the recessed chrome surface already specified for the header. The article list does not gain a second search strip.
- Row menus, article-row actions, folders, unread and saved views, and the sign-in screen keep their actions; menu presentation follows the compact chrome density of the feed tree.
- Feed/folder tree density is comfortable (one row under the pointer), denser than the pre-feature spacing but not ultra-cramped.
- The "Can't reach FeedBox" notice stays outside the page header.
- No new account setting, theme control, or other menu item is added.
- This is an interface change. It follows Apple's Human Interface Guidelines and the reader's existing appearance: clarity of which actions are global and which belong to the article, comfortable touch targets, visible focus, named controls, and reduced motion. It adds no stored data, no new configuration, and no new dependency.
