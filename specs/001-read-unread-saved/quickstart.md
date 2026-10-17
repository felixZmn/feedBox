# Quickstart: Read, Unread, and Saved Articles

Validation guide for [spec.md](./spec.md). Field rules are in [data-model.md](./data-model.md). HTTP and routes are in [contracts/article-views.md](./contracts/article-views.md).

## Prerequisites

- Java 25 and Maven
- Node, for `npm run test:frontend`
- A signed-in reader with at least one feed that has both read and unread dated articles
- Docker available if integration tests use Dev Services PostgreSQL

No new configuration key. Do not add a Helm value or a README parameter for this feature.

## Automated checks

From the repository root:

```bash
mvn test
mvn verify -DskipITs=false
npm run test:frontend
```

Expected:

- Unit tests pass, including route round-trips for `/unread`, `/saved`, `/unread/article/{id}`, and `/saved/article/{id}`.
- `ArticleRepositoryIT` shows a saved article staying saved after it is marked read, an unread list that omits horizon-read articles, and a saved list that contains both read and unread saved articles in newest-first order.
- Anonymous `GET /api/article/saved/count`, `POST /api/article/{id}/saved`, and `DELETE /api/article/{id}/saved` return 401.
- Anonymous `GET /unread` and `GET /saved` return 401, same as `GET /feed/1`.
- `npm run test:frontend` passes, including the saved count being hidden at zero.

## Reader checks

Run the app (`mvn quarkus:dev`) and sign in.

1. **Read mark.** In All Feeds, unread rows show a filled circle and read rows show the unfilled circle. Titles use the same font. The circle and the star do not move when a row becomes read. Light and dark both show the difference without relying on color alone.
2. **Mark from the list.** Activate the filled circle on an unread row. The article does not open. The circle becomes unfilled, and the All Feeds unread count drops by one without a reload. Activate the unfilled circle. The article stays read and does not open.
3. **Open still marks read.** Open a different unread article. Its row becomes unfilled, and the unread count drops. If the request fails, the row stays filled and the count returns.
4. **Star.** Activate the star on a row and again in the open article. The article does not open or close. Both stars fill together. Leave the reader and come back. The article is still saved. Activate the star again. Both stars empty. A failed save restores the previous star.
5. **Saved.** The sidebar order is All Feeds, Unread, Saved, a gap, then folders, then Add. Saved has no options menu. Its count matches dated saved articles and disappears at zero. Saved lists those articles, read and unread, newest first. Search inside Saved returns only saved matches.
6. **Unread.** Unread lists only unread articles, with no second copy of the All Feeds count and no options menu. Mark one read while Unread is open: the row stays, with an unfilled circle. Leave Unread and open it again: that article is gone. Search inside Unread returns only unread matches. An empty Unread list says "No unread articles". An empty Saved list says "No saved articles".
7. **Mark feed or all.** Mark a feed, a folder, or All Feeds read. Visible rows become unfilled. Saved articles stay saved.
8. **Refresh and narrow width.** Refresh on Unread and on Saved. The same view is selected. On a narrow window, choosing either view opens the article list the same way All Feeds does. Keyboard focus is visible on the two sidebar rows, both stars, and the status mark. Their names match the contract.
9. **Delete.** Delete a feed that has a saved article. It disappears from Saved.
