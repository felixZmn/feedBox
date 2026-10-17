# Feature Specification: Read, Unread, and Saved Articles

**Feature Branch**: `001-read-unread-saved`

**Created**: 2026-10-17

**Status**: Draft

**Input**: User description: "Currently, articles are marked as read in the background, but there is no indication on the frontend if the article is read or not. Please adjust the UI and add a read/unread indicator; a bookmark, read-later, or star so an article can be marked and found later; a new sidebar entry below All Feeds for those marked articles; and an Unread entry that shows the same articles as All Feeds filtered to unread. Leave a little space between All / Unread / Marked and the folders. Keep the UI clean and not overloaded."

## Clarifications

### Session 2026-10-17

- Q: Should the title font change to show that an article is read? → A: No. The title font stays the same.
- Q: What happens when the reader clicks the status mark? → A: That article is marked read, and it does not open. The mark cannot mark an article unread.
- Q: Where does the bookmark star sit on the article row? → A: Immediately beside the status mark, at the start of the row.
- Q: Should the status mark disappear once an article is read? → A: No. It is always shown, so the row does not jump. Unread uses a filled icon and read uses the matching unfilled icon, from the same icon library and in the same style as the reader's other icons.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See which articles are already read (Priority: P1)

A reader scans the article list and can tell, without opening anything, which articles are still unread and which have already been read. Every row shows a small status mark in the same place. Unread is a filled icon and read is the matching unfilled icon. The title font does not change. Activating the filled mark marks the article read without opening it, and the icon becomes unfilled. Opening an article still marks it read, as it does today.

**Why this priority**: The reader already records read state, but the list looks the same either way. Until that state is visible, the other views have nothing reliable to show.

**Independent Test**: Open a mix of read and unread articles in All Feeds, a folder, and a single feed. Confirm every row shows a status mark, titles use the same font, unread marks are filled, and read marks are the matching unfilled icon. Activate one filled mark and confirm that article becomes read without opening and the icon stays in place. Then open another unread article and confirm its mark becomes unfilled without a manual refresh.

**Acceptance Scenarios**:

1. **Given** an unread article in the list, **When** the reader looks at the row, **Then** a filled status mark is shown at the start of the row, the title font is unchanged, and the row exposes the word "Unread" to assistive technology.
2. **Given** a read article in the list, **When** the reader looks at the row, **Then** the title uses the same font as an unread article, the matching unfilled status mark is shown in the same place, and the row exposes the word "Read".
3. **Given** an unread article, **When** the reader opens it and it is marked read, **Then** its row switches to the read appearance and the All Feeds unread count decreases, without reloading the sidebar or the article list.
4. **Given** the reader uses Mark as read on a feed, a folder, or All Feeds, **When** the affected articles are already on screen, **Then** those rows switch to the read appearance and the unread counts update where they already are.
5. **Given** marking an article read does not succeed, **When** the reader looks at the row again, **Then** it still looks unread and the unread count is unchanged.
6. **Given** light appearance and dark appearance, **When** the same read and unread rows are shown, **Then** the two states remain distinguishable, including for a reader who cannot use color as the only cue.
7. **Given** the reader uses only the keyboard, **When** focus moves onto an article row, **Then** focus is visible and the row's name includes whether it is read or unread.
8. **Given** a selected article row, **When** it is either unread or read, **Then** the existing selected appearance stays visible along with the read state and the star.
9. **Given** an unread article, **When** the reader activates its filled status mark, **Then** the article becomes read, the mark switches to the unfilled icon in the same place, the article does not open, the star stays in place, and the All Feeds unread count decreases.
10. **Given** a read article, **When** the reader activates its unfilled status mark, **Then** the article stays read, the mark stays unfilled, and the article does not open.

---

### User Story 2 - Return to articles worth keeping (Priority: P2)

A reader marks an article to come back to it, then finds all marked articles in one quiet sidebar entry named Saved.

**Why this priority**: Read state answers "have I seen this?" Saving answers the separate question "do I want this again?" The reader asked for both, and for a place to find the marked articles.

**Independent Test**: Save an article from the list and from the open article, leave the reader, come back, and open Saved. The article is there. Unsave it and, after Saved is opened again, it is gone. Its read state never changes because it was saved. The sidebar shows Saved beneath All Feeds, with space before the folders.

**Acceptance Scenarios**:

1. **Given** an article in the list, **When** the reader activates its star, **Then** the article is saved, the star shows the saved state, the article does not open, and the star remains immediately beside the status mark at the start of the row.
2. **Given** an open article, **When** the reader activates the star beside the existing article actions, **Then** the same article is saved and the star on its list row matches.
3. **Given** a saved article, **When** the reader activates the star again, **Then** it is no longer saved and both stars show the unsaved state. If that article is already listed in Saved, the row stays until the reader opens Saved again or refreshes the page.
4. **Given** at least one saved article, **When** the reader chooses Saved in the sidebar, **Then** the article list shows saved articles, newest first, whether they are read or unread, and each row still shows its read state and a filled star.
5. **Given** a saved article, **When** the reader leaves the reader and later returns, **Then** the article is still saved and still appears under Saved.
6. **Given** an unsaved article, **When** it becomes read, **Then** it is not added to Saved. **Given** a saved article, **When** it becomes read, **Then** it stays saved.
7. **Given** no saved articles, **When** the reader opens Saved, **Then** the list shows "No saved articles" and the sidebar entry is still present with no count.
8. **Given** the reader is searching inside Saved, **When** matches exist, **Then** only saved articles that match are listed. **When** none match, **Then** the list shows the existing no-match message. **When** the search is cleared, **Then** the full Saved list returns.
9. **Given** Saved is the only new view under All Feeds, **When** the reader looks at the sidebar, **Then** Saved sits directly under All Feeds, uses one star icon and the name "Saved", has no options menu, and is separated from the folders by empty space, with no heading or rule. **When** Unread is also present, **Then** Saved sits under Unread.
10. **Given** Saved contains articles, **When** the reader looks at that sidebar row, **Then** a quiet count is shown. **When** nothing is saved, **Then** the count is hidden.
11. **Given** keyboard only, **When** the reader focuses a star and activates it, **Then** the article becomes saved or unsaved, focus stays visible, the button name switches between "Save article" and "Remove from Saved", and the star is at least as easy to tap as the existing header buttons.
12. **Given** a narrow layout, **When** the reader chooses Saved, **Then** the article list opens the same way it does after All Feeds. Refreshing the page keeps Saved selected.
13. **Given** a long Saved list, **When** the reader scrolls to the end, **Then** further saved articles load the same way they do in All Feeds.
14. **Given** saving or unsaving does not succeed, **When** the reader looks at the star, **Then** it shows the state from before the attempt and Saved is unchanged.
15. **Given** a saved article, **When** its feed or folder is removed, **Then** the article no longer appears in Saved.

---

### User Story 3 - Scan only what is still unread (Priority: P3)

A reader chooses Unread, directly under All Feeds, and sees the same library as All Feeds with read articles left out.

**Why this priority**: All Feeds already collects every article. Unread is that same list filtered, so it is valuable once read state is visible, and it should not introduce a second way of browsing folders.

**Independent Test**: With a mix of read and unread articles, choose Unread and confirm only unread articles appear, in the same newest-first order as All Feeds. Leave and come back, and confirm an article that was read in between is absent. Confirm Unread sits with the other top views and does not repeat the All Feeds count.

**Acceptance Scenarios**:

1. **Given** All Feeds contains both read and unread articles, **When** the reader chooses Unread, **Then** the list shows only the unread articles, newest first, with the same row contents as All Feeds.
2. **Given** the reader opens an article from Unread and it becomes read, **When** the row is still on screen, **Then** the row changes to the read appearance and stays put, and the All Feeds unread count decreases.
3. **Given** that article is now read, **When** the reader leaves Unread and chooses it again, or refreshes the reader, **Then** the article is no longer in the Unread list.
4. **Given** every article is read, **When** the reader opens Unread, **Then** the list shows "No unread articles".
5. **Given** the reader searches inside Unread, **When** results are shown, **Then** every result is unread and matches the search. Clearing the search shows the unread list again.
6. **Given** a long unread list, **When** the reader scrolls to the end, **Then** further unread articles load the same way they do in All Feeds.
7. **Given** Saved is also present, **When** the reader looks at the sidebar from top to bottom, **Then** the order is All Feeds, Unread, Saved, space, the folders, and Add. Unread has one simple dot icon, the name "Unread", no options menu, and no count of its own.
8. **Given** Unread is the only new view under All Feeds, **When** the reader looks at the sidebar, **Then** Unread sits directly under All Feeds, with empty space before the folders.
9. **Given** a narrow layout, **When** the reader chooses Unread, **Then** the article list opens the same way it does after All Feeds. Refreshing the page keeps Unread selected.
10. **Given** keyboard only, **When** the reader moves to Unread and activates it, **Then** Unread is selected with the same visible selection All Feeds uses, and no folder stays selected.

---

### Edge Cases

- An article with no title still shows the read mark and the star; the existing "No Title" text remains.
- The status mark, the star, and the row are separate actions. Activating the status mark marks that article read and does not open it. Activating the star never opens or closes the article and never changes read state. Activating the row opens the article and does not save or unsave it.
- Saving or unsaving fails: the star returns to the state it had before the attempt, and Saved does not gain or lose the article.
- The article currently open is unsaved or becomes read: it stays open. Its row updates where it is. The next time the reader opens Unread or Saved, or refreshes the page, the list matches the new state.
- The same article is visible in the list and open at the same time: both stars and the read appearance always agree.
- Using Mark as read on a feed, a folder, or All Feeds does not unsave articles. Those articles remain under Saved and show the read appearance the next time the reader opens Saved or refreshes the page.
- Deleting a feed or folder removes its articles from Saved along with the articles.
- A saved article that is also unread is counted in the All Feeds unread count and in the Saved count.
- Search text is not carried silently into another view. Choosing All Feeds, Unread, Saved, a folder, or a feed starts from that view's unfiltered list.
- The space above the folders remains space only when there are no folders, when folders are collapsed, and when the sidebar scrolls. It is about the same size as the space already used above Add, and it is larger than the space between All Feeds, Unread, and Saved.
- Changing read or saved state does not add a new animation. The status mark switches between its filled and unfilled icon in place, and the star switches between its filled and unfilled icon in place.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every article row MUST show whether that article is read or unread.
- **FR-002**: Every article row MUST show one small status mark in the same position. An unread row MUST use a filled icon. A read row MUST use the matching unfilled icon of that same mark. The title's font, weight, and color MUST NOT change to show read state. When two rows differ only in read state, the filled or unfilled mark MUST be the only visual difference.
- **FR-003**: Read and unread MUST remain distinguishable in light and dark appearance, and MUST NOT depend on color alone.
- **FR-004**: Each article row MUST expose "Read" or "Unread" as text for assistive technology.
- **FR-005**: When an article becomes read because it was opened, because its status mark was activated, or because the reader used Mark as read on its feed, its folder, or All Feeds, every visible row for that article MUST switch to the read appearance where it already is. The sidebar unread counts MUST update where they already are, as they do today.
- **FR-006**: If an attempt to mark an article read fails, the row MUST stay unread and the unread count MUST stay unchanged.
- **FR-007**: The reader MUST be able to save and unsave an article with a single star on the article row and the same star in the open-article header.
- **FR-008**: The status mark and the row star MUST sit together at the start of the article row, with the mark first and the star immediately beside it. Both MUST stay in those positions when the article becomes read or unread, so the title, source, and age do not shift. The pair MUST NOT add a line of text or change the title font.
- **FR-009**: The header star MUST sit with the existing article actions and use their size and spacing. Its accessible name MUST be "Save article" when the article is not saved and "Remove from Saved" when it is saved. The row star uses the same names.
- **FR-010**: Activating either star MUST NOT open, close, or change the selected article.
- **FR-011**: A filled star MUST mean saved. The matching unfilled star MUST mean not saved. Both forms MUST stay in the same position and MUST be visible in light and dark appearance.
- **FR-012**: Saving or unsaving MUST NOT change whether an article is read. Becoming read MUST NOT save or unsave an article.
- **FR-013**: Saved state MUST still be there after the reader leaves and comes back.
- **FR-014**: If saving or unsaving fails, the star MUST return to its previous state during the same visit, and the Saved list MUST NOT include or drop the article because of that failed attempt.
- **FR-015**: With both new views present, the sidebar MUST show, from top to bottom: All Feeds, Unread, Saved, empty space, the folders, and Add.
- **FR-016**: Unread and Saved MUST be selectable in the same way as All Feeds, including the selected appearance, keyboard use, and visible focus. They MUST NOT have an options menu.
- **FR-017**: Unread and Saved MUST each use one icon of the same size and style as the All Feeds icon, from the same icon library as the reader's other icons. Unread's icon MUST be a simple dot. Saved's icon MUST be a star.
- **FR-018**: The space between the top views and the folders MUST be empty space, about the same size as the space above Add, and larger than the space between All Feeds, Unread, and Saved. It MUST NOT add a heading, rule, or label.
- **FR-019**: All Feeds MUST keep the unread count it shows today. The Unread row MUST NOT repeat that count. The Saved row MUST show how many articles are saved when that number is greater than zero, in the same quiet style as folder unread counts, and MUST hide the count at zero.
- **FR-020**: Choosing Unread MUST list the same articles as All Feeds, excluding read articles, newest first.
- **FR-021**: Choosing Saved MUST list every saved article, read or unread, newest first.
- **FR-022**: An article that becomes read while it is already visible in Unread MUST update to the read appearance and remain in that on-screen list. The next time the reader opens Unread, or refreshes the page, that article MUST be absent.
- **FR-023**: An article that is unsaved while it is already visible in Saved MUST show an unfilled star and remain in that on-screen list. The next time the reader opens Saved, or refreshes the page, that article MUST be absent.
- **FR-024**: Search started from Unread MUST return only unread matches. Search started from Saved MUST return only saved matches. No matches MUST use the existing no-match message. Clearing search MUST restore the current view.
- **FR-025**: Unread and Saved MUST continue loading older articles in the same way All Feeds does.
- **FR-026**: Unread with nothing to show MUST display "No unread articles". Saved with nothing to show MUST display "No saved articles".
- **FR-027**: On a narrow layout, choosing Unread or Saved MUST open the article list the same way choosing All Feeds does.
- **FR-028**: Refreshing the reader MUST keep All Feeds, Unread, or Saved selected when that was the current view.
- **FR-029**: New and changed controls MUST be keyboard reachable, show visible focus, have an accessible name, and use a touch target at least as large as the existing header buttons. Read and saved changes MUST happen without adding animation.
- **FR-030**: The new rows, marks, and stars MUST reuse the reader's existing type, accent, corners, and light and dark appearance. They MUST follow Apple's Human Interface Guidelines: clarity, deference, depth, consistency, and comfortable touch targets.
- **FR-031**: A selected or hovered article row MUST keep today's selected and hover appearance together with its read state and its star.
- **FR-032**: Removing a feed or folder MUST remove its articles from Saved together with those articles.
- **FR-033**: Activating the filled status mark MUST mark that article read, switch the mark to the unfilled icon in place, and MUST NOT open the article or change whether it is saved. The filled mark's accessible name MUST be "Mark as read". The unfilled mark MUST remain visible, MUST be named "Read", and activating it MUST NOT mark the article unread or open it. The visible icon MUST stay small, and its tappable area MUST be at least as large as the existing header buttons.
- **FR-034**: The status mark, the row star, and the open-article star MUST use filled and unfilled forms from the same icon library and in the same visual style as the other icons in the reader. A second icon style MUST NOT be introduced.

### Key Entities

- **Article read state**: Whether the reader has already read an article. It already exists. This feature shows it and lets Unread filter on it. An article becomes read when it is opened, when its status mark is activated, or when Mark as read is used on a feed, a folder, or All Feeds.
- **Saved article**: An article the reader has chosen to find again. Saved is independent of read state. An article is either saved or not; it is not filed into a further list.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a mixed list, a person identifies every visible article as read or unread in under 2 seconds per row from the filled or unfilled status mark alone, without opening it and without a difference in title font, in both light and dark appearance, and without using color as the only cue.
- **SC-002**: Within 1 second of an article becoming read, whether it was opened or its status mark was activated, its row shows the read appearance and the All Feeds unread count has decreased, without a manual refresh.
- **SC-003**: A reader saves an article and opens it again from Saved in under 15 seconds. The same article is still under Saved after the reader leaves and returns.
- **SC-004**: On a freshly loaded Unread list, 100% of the listed articles are unread, and articles that are read in All Feeds are absent.
- **SC-005**: A freshly loaded Saved list contains 100% of the reader's saved articles and no unsaved article.
- **SC-006**: Every article row shows one small status mark with the star beside it. The mark is filled when unread and unfilled when read, and it stays in place. The title font is the same on both. The sidebar gains two entries and one gap. No new menu or heading is added.
- **SC-007**: All Feeds, Unread, Saved, the status mark, and both stars can be used from the keyboard alone, and each has a name that assistive technology can speak.

## Assumptions

- The collection is named Saved. The reader allowed a bookmark, a read-later pile, or a star; Saved plus one star is the short, familiar choice and does not imply a due date or a queue. The sidebar order the reader named (All, Unread, marked) is All Feeds, Unread, Saved.
- Readers do not get a way to mark a single article unread. A single article becomes read when it is opened, when its status mark is activated, or when Mark as read is used on a feed, a folder, or All Feeds.
- An on-screen Unread or Saved list is not rearranged under the open article. The list matches the new read or saved state the next time the reader opens that view or refreshes the page. Sidebar counts stay current immediately.
- Unread and Saved use the same newest-first order as All Feeds.
- Search stays inside the view the reader is already in, matching search inside a folder or feed today.
- Empty Saved and empty Unread use one quiet line of text, in the same manner as today's empty article list.
- No saved-article folders, tags, notes, reminders, ordering, or export are included.
- No new bulk action is added. Existing actions that mark a feed, folder, or the whole library read stay where they are.
- The feature stays inside the signed-in reader. It adds no public page, no setting, and no third-party service.
- The new interface follows the reader's existing appearance and Apple's Human Interface Guidelines. No exception is required.
