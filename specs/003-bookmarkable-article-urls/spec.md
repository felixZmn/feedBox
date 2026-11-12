# Feature Specification: Bookmarkable Article URLs

**Feature Branch**: `003-bookmarkable-article-urls`

**Created**: 2026-11-12

**Status**: Draft

**Input**: User description: "As a User I want to have bookmarkable urls so that i can access articles directly by url. this is for bookmarks, but perspectively also for sharing articles with others. also, a browser refresh should stay at the given path / article, and not throw any errors"

## Clarifications

### Session 2026-11-12

- Q: When the address names a feed or folder that has been removed, or names a path the reader does not recognize, and the reader falls back to All Feeds, should the address bar switch to All Feeds? → A: Switch the address bar to All Feeds, with no article open
- Q: When an address names a feed or folder together with an article that still exists but belongs to a different feed or folder, what should the reader show? → A: Keep the address, select the named feed or folder, and open the article

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Open an article from its address (Priority: P1)

A signed-in reader can open a specific article by its address. They paste the address, choose a bookmark, or follow a link, and that article is open in the reader with its title, source, date, and body. They do not have to find it in a list first. The address may name the article alone, or it may name a list together with the article. The lists are All Feeds, Unread, Saved, a folder, and a feed.

**Why this priority**: A bookmark is useful only if opening it shows the article. This is the smallest slice that delivers the request.

**Independent Test**: While signed in, open an address that names one existing article and no list. Confirm that article is open and the address is unchanged. Repeat with an address that names Unread, Saved, a folder, or a feed together with an existing article in that place. Confirm the named list is selected and that article is open.

**Acceptance Scenarios**:

1. **Given** a signed-in reader and an existing article, **When** the reader opens an address that names only that article, **Then** that article is open, All Feeds is the selected list, and the address still names only that article.
2. **Given** a signed-in reader and an existing article in a feed, **When** the reader opens an address that names that feed and that article, **Then** that feed is selected and that article is open.
3. **Given** a signed-in reader and an existing article in a folder, **When** the reader opens an address that names that folder and that article, **Then** that folder is selected and that article is open.
4. **Given** a signed-in reader and an existing article, **When** the reader opens an address that names Unread or Saved together with that article, **Then** that list is selected and that article is open.
5. **Given** an article that is not among the first articles shown for its list, **When** the reader opens an address for that article, **Then** the article still opens.
6. **Given** an article opened from its address, **When** the reader looks at the browser tab, **Then** the tab title includes the article title.
7. **Given** the same article opened from its address and opened by choosing it in the list, **When** the reader compares the two, **Then** the title, source, date, body, read state, and saved state match, including the article becoming read.
8. **Given** a feed or folder that still exists, and an article that still exists but belongs to a different feed or folder, **When** the reader opens an address naming that feed or folder and that article, **Then** the named feed or folder is selected, the article is open, the address is unchanged, and no application-failure message appears.

---

### User Story 2 - Refresh and history keep the current place (Priority: P1)

While the reader moves through the library, the browser address follows. Choosing All Feeds, Unread, Saved, a folder, or a feed updates the address to that list. Opening an article updates the address to that list plus that article. Moving to the previous or next article updates the address to the article now open. Refreshing the browser shows the same list and the same article and keeps the same address when that list and article still exist. Back and forward move through those places. None of these actions presents an application-failure message when the list and the article still exist.

**Why this priority**: A bookmark is a copy of the address at a moment in time. If refresh leaves the article, rewrites the address, or reports a failure, the address cannot be trusted.

**Independent Test**: From All Feeds, open a feed, open an article, then move to the next article. Refresh and confirm the same feed and the same article, with the same address and no failure message. Use back until the feed list is showing, then forward until the article is showing again.

**Acceptance Scenarios**:

1. **Given** a signed-in reader on All Feeds, Unread, Saved, a folder, or a feed, with no article open, **When** the reader refreshes, **Then** that same list is selected, no article is open, the address is unchanged, and no application-failure message appears.
2. **Given** an article open inside one of those lists, **When** the reader refreshes, **Then** that list stays selected, that article stays open, the address is unchanged, and no application-failure message appears.
3. **Given** an article that was opened from Unread or Saved, the article still exists, and it is now read or no longer saved, **When** the reader refreshes that same address, **Then** the article stays open, the address is unchanged, and no application-failure message appears.
4. **Given** the reader moves from a list to an article and then to the next article, **When** the reader refreshes, **Then** the article that was open after the move is the article that is open after refresh.
5. **Given** a trail of list and article addresses, **When** the reader uses the browser back control and then the forward control, **Then** each step shows the list and article for that address, and no application-failure message appears.
6. **Given** an article address open on a narrow screen, **When** the page is shown or refreshed, **Then** the article is what the reader sees.
7. **Given** a list address with no article on a narrow screen, **When** the page is shown or refreshed, **Then** that list is what the reader sees.
8. **Given** an address that names a removed feed, a removed folder, or a path the reader does not recognize, **When** the reader opens it, **Then** the address bar shows All Feeds, no article is open, and no application-failure message appears. A refresh then stays on All Feeds, and the back control does not return to that removed or unrecognized address.

---

### User Story 3 - Sign-in returns to the requested article (Priority: P2)

A reader who opens a bookmarked article address while signed out sees the sign-in screen, not the article. After a successful sign-in, the reader lands on that article. The address is remembered for this sign-in only.

**Why this priority**: Bookmarks are often opened in a fresh browser session. The article has to be there after sign-in, or the bookmark has failed.

**Independent Test**: Sign out, open an address for an existing article, sign in, and confirm that article is open. Sign out again and confirm the article body is not visible on the sign-in screen.

**Acceptance Scenarios**:

1. **Given** a signed-out reader and an address for an existing article, **When** the reader opens that address, **Then** the sign-in screen is shown and the article body is not shown.
2. **Given** the reader has just opened that address while signed out, **When** sign-in succeeds, **Then** that article is open.
3. **Given** a signed-out reader, **When** they open the library home and sign in, **Then** they arrive at All Feeds with no article open.
4. **Given** a person who cannot sign in to this FeedBox, **When** they open an article address, **Then** they see the sign-in screen and cannot read the article.

---

### User Story 4 - The same address works for another signed-in person (Priority: P3)

The article address is stable enough to send to someone else later. Another person who can sign in to this same FeedBox opens that address and sees the same article. This feature adds no share button, copy control, or public page. Copying the address from the browser is enough.

**Why this priority**: The request is for bookmarks now, and for sharing later. A stable address makes that later use possible without a sharing control in this feature.

**Independent Test**: Copy an article address from one signed-in browser. Open it in another signed-in browser that can access this FeedBox. Confirm the same article title and body. Confirm the reader has no new share control.

**Acceptance Scenarios**:

1. **Given** two people who can each sign in to this FeedBox, **When** the second person opens an article address copied from the first, **Then** both see the same article title and body.
2. **Given** the signed-in reader, **When** they look through the page header, the article, and the lists, **Then** there is no share control and no copy-link control added by this feature.
3. **Given** an article address, **When** it is opened on a later day in a new browser session by someone who can sign in, **Then** the article still opens after sign-in, as long as the article still exists.

---

### Edge Cases

- An address names an article that has been removed. The reader shows a plain message that the article is unavailable, stays usable, and shows no application-failure message. Refreshing that address shows the same message and keeps the address.
- An address names a feed or folder that has been removed, with or without an article. The address bar switches to All Feeds, no article is open, and no application-failure message appears. A refresh then stays on All Feeds. The back control does not return to that removed address.
- An address names an article that still exists but is no longer unread, or is no longer saved. The article opens, the address stays as opened, and no application-failure message appears.
- An address names a feed or folder that still exists, together with an article that still exists but belongs to a different feed or folder. The named feed or folder stays selected, the article opens, the address stays unchanged, and no application-failure message appears.
- An address has a trailing slash, or carries extra details the reader does not use. The same list and article open as they would without those extras.
- An address is not a library place and not an article. The address bar switches to All Feeds, no article is open, and no application-failure message appears. A refresh then stays on All Feeds. The back control does not return to that unrecognized address.
- The instance cannot be reached. The reader shows the existing connection notice, leaves the address unchanged, and a successful retry opens the same place. This is not reported as a failure of the app to load.
- Previous and Next, when the open article belongs to the selected list, move to the previous or next article in that list and update the address. When the open article is not part of that list, those actions show no error.
- Opening an address that is already open leaves a single article on screen.
- The sign-in screen, feed and folder management, search, save, and open-in-browser keep their current results. Search text is not part of the address. After a refresh, the list is shown without the previous search.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A signed-in reader can open an existing article from an address that names only that article. The article is shown without the reader choosing it from a list. All Feeds is selected. The address remains the article-only address.
- **FR-002**: A signed-in reader can open an address that names All Feeds, Unread, Saved, a folder, or a feed. That list is selected. When the address also names an existing article, that article is open.
- **FR-003**: An article address opens that article even when the article is not among the first articles shown for the selected list.
- **FR-004**: Choosing All Feeds, Unread, Saved, a folder, a feed, or an article updates the browser address to that place. Moving to the previous or next article updates the address to the article that is then open.
- **FR-005**: Refreshing a list address, or a list address with an article, restores that same place when the list and the article still exist. The address after refresh is the address before refresh. No application-failure message is shown.
- **FR-006**: Refreshing an article address keeps that article open when the article still exists, including after it has become read or has left Unread or Saved. The address stays unchanged. No application-failure message is shown.
- **FR-007**: The browser back and forward controls restore the list and article for each address in the trail. No application-failure message is shown when those items still exist.
- **FR-008**: Opening an article from an address has the same result as choosing that article in the list. The article becomes read. Its saved state stays as it was. The title, source, date, and body match.
- **FR-009**: While an article is open, the browser tab title includes the article title. A list with no open article uses that list's name. All Feeds with no open article uses the product name.
- **FR-010**: A signed-out reader who opens a library or article address sees the sign-in screen. The article body is not shown. After a successful sign-in, the reader opens the address they requested. Signing in from the library home opens All Feeds with no article selected.
- **FR-011**: A person who can sign in to this FeedBox can open an article address used by someone else and see that same article. A person who cannot sign in sees the sign-in screen and cannot read the article.
- **FR-012**: This feature adds no share control and no copy-link control. The reader keeps an article address by using the browser's own address.
- **FR-013**: When the named article no longer exists, the reader shows a plain unavailable message, remains usable, and shows no application-failure message. Refreshing that address keeps the address and shows the same message.
- **FR-014**: When the named feed or folder no longer exists, including when the address also names an article, the address bar switches to All Feeds, no article is open, and no application-failure message is shown. A refresh then stays on All Feeds. The back control does not return to the removed feed or folder address.
- **FR-015**: An address that is neither a library place nor an article switches the address bar to All Feeds, with no article open and no application-failure message. A refresh then stays on All Feeds. The back control does not return to that unrecognized address.
- **FR-016**: A trailing slash, or extra unused details on an otherwise valid address, still opens the same list and article.
- **FR-017**: On a narrow screen, an address that names an article shows that article. An address that names only a list shows that list.
- **FR-018**: When the instance cannot be reached, the reader shows the existing connection notice, leaves the address unchanged, and opens the same place after a successful retry. That situation is not presented as a failure of the app to load.
- **FR-019**: Previous and Next follow the selected list when the open article is part of that list, and update the address to match. When the open article is not part of that list, Previous and Next leave that article open and show no error.
- **FR-020**: When an address names a feed or folder that still exists together with an article that still exists but belongs to a different feed or folder, the named feed or folder stays selected, that article opens, the address stays unchanged, and no application-failure message is shown.

### Key Entities

- **Article**: One stored item the reader can open. It has a title, source, date, body, a read state, and a saved state. It has one stable article address that does not depend on which list was selected.
- **List place**: All Feeds, Unread, Saved, one folder, or one feed. A list place may be open with no article, or with one article open.
- **Address**: The browser location of a list place, optionally including one article, or of one article alone. It can be bookmarked, copied, refreshed, and reopened after sign-in.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In 100% of trials, a signed-in reader who opens a bookmark for an existing article sees that article on the first load, without choosing it from a list, and the address is unchanged.
- **SC-002**: In 100% of trials, refreshing an existing list address or article address shows that same place, keeps the same address, and shows no application-failure message. This includes an article that has since become read, left Unread or Saved, or belongs to a different feed or folder than the one named.
- **SC-003**: In 100% of trials, a reader who opens an article bookmark while signed out and then signs in successfully lands on that article, not on All Feeds.
- **SC-004**: A second person who can sign in to this FeedBox opens a copied article address and sees the same article title and body as the person who copied it.
- **SC-005**: In 100% of trials, a removed article keeps its address, shows an unavailable message, and leaves the reader able to open another article, with no application-failure message. In 100% of trials, a removed feed, a removed folder, or an unrecognized address ends with the address bar on All Feeds, no article open, and no application-failure message.
- **SC-006**: In 100% of trials, back and then forward return to the article that was open before those steps, with no application-failure message.
- **SC-007**: A reader can bookmark the current article using only the browser address. The feature adds no share control, and an unsigned visitor cannot read the article body.

## Assumptions

- The immediate job is bookmarks and a refresh that stays on the current list or article. Sharing later uses that same address. This feature does not add a share button, a copy-link control, a public article page, or any sending of article text outside this FeedBox.
- "Others" means other people who can sign in to this same FeedBox. Anyone who completes sign-in can open the same library. Someone who cannot sign in sees the sign-in screen.
- An address that names only an article selects All Feeds and keeps the article-only address. An address that names a list, with or without an article, selects that list and keeps that address. That includes an article that is no longer a member of Unread or Saved, and an article that belongs to a different feed or folder than the one named.
- Opening an article from an address follows the current open-article behavior: the article is marked read, and saved state is left as it was.
- Search text is not stored in the address. Refresh shows the list without the search that was active before refresh.
- The browser tab title continues to name the open article, the selected list, or the product, so a bookmark is recognizable.
- A removed article keeps its address and shows an unavailable message. A removed feed, a removed folder, or an unrecognized address switches the address bar to All Feeds with no article open. None of these is an application failure. A failure to reach the instance keeps the existing connection notice and the current address.
- Previous and Next stay list actions. They do not invent an error when the open article is outside the selected list.
- No new account, theme, or library data is introduced. Feed management, save, open in browser, and the sign-in screen keep their current behavior aside from returning to the requested address after sign-in.
- The unavailable message uses plain language. This feature adds no new control. Existing controls stay keyboard reachable, named, and comfortable to use, in light and dark appearance.
