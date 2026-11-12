# Quickstart: Bookmarkable Article URLs

Validation guide for [spec.md](./spec.md). Address fields are in [data-model.md](./data-model.md). Path grammar and outcomes are in [contracts/addresses.md](./contracts/addresses.md).

## Prerequisites

- Java 25 and Maven
- Node, for `npm run test:frontend`
- A signed-in reader with at least two feeds, one folder, and several articles, including one that is not on the first page of All Feeds
- A second browser or a private window for the signed-out checks

No new configuration key. Do not add a Helm value or a README parameter for this feature.

## Automated checks

From the repository root:

```bash
mvn test
npm run test:frontend
```

Expected:

- `src/test/js/router.test.js` covers the path grammar and the resolution table (ready, article unavailable, replaced home, foreign article still opens).
- `AuthPathPolicyIT` still requires a session for `/article/{id}`, `/feed/{id}`, `/folder/{id}`, `/unread`, and `/saved`, including list-plus-article forms. `/` and `/index.html` stay public. An unrecognized HTML path returns the shell rather than an error page. `GET /api/article/{id}` stays 401 when anonymous.
- No new outbound-HTTP test. Optional: `mvn verify -DskipITs=false` still green.

## Signed-in checks

Run the app (`mvn quarkus:dev`) and sign in.

1. **Article-only address.** Open an article and copy the address while All Feeds is selected (`/article/{id}`). Open that address in a new tab. The article is open, All Feeds is selected, and the address is unchanged. The tab title includes the article title.
2. **List plus article.** From a feed, open an article. The address is `/feed/{id}/article/{id}`. Refresh. The same feed and the same article are showing, the address is unchanged, and no failure dialog appears. Repeat from a folder, Unread, and Saved.
3. **After read state changes.** Open `/unread/article/{id}`, which marks the article read. Refresh that same address. The article stays open and the address stays. Remove an article from Saved and refresh `/saved/article/{id}`. Same result.
4. **Not on the first page.** Open `/article/{id}` for an article that is not among the first articles in All Feeds. The article still opens.
5. **Foreign article.** Open `/feed/{other}/article/{id}` where the article belongs to a different feed. That other feed is selected, the article is open, the address is unchanged, and there is no failure dialog. Previous and Next do not change the article and do not show an error.
6. **Missing article.** Open `/feed/{id}/article/{id}` with an id that does not exist, and a feed that does. The feed stays selected. The reader column says "This article is unavailable." The address is unchanged. Refresh repeats that. No "app failed to load" dialog. Choosing another article still works.
7. **Missing feed or folder.** Open `/feed/{id}` and `/folder/{id}` for ids that do not exist, including one path that also names an article. The address becomes `/`, All Feeds is selected, no article is open, and there is no failure dialog. Back does not return to the missing id.
8. **Unrecognized path.** Open `/no-such`. The address becomes `/` and All Feeds is shown, with no failure dialog. Back does not return to `/no-such`.
9. **Refresh a list.** With a feed selected and no article open, refresh. The feed stays selected, the reader stays empty, and the address is unchanged. Repeat for `/`, `/unread`, and `/saved`.
10. **History.** Open a feed, open an article, then Next. Refresh shows the article Next landed on. Back and forward move through those places without a failure dialog.
11. **Narrow.** At a phone width, an article address shows the article. A list address with no article shows that list. The unavailable sentence is what you see for a missing article.
12. **No share control.** The page header, lists, and article have no new share or copy-link control. Search is not kept in the address: refresh shows the list without the search that was typed before.

## Signed-out checks

13. **Known address.** Sign out. Open `/article/{id}` for an existing article. The article body is not shown. The sign-in screen (in-app panel or the identity provider) is shown. After a successful sign-in, that article is open.
14. **Home.** Sign out, open `/`, and sign in. All Feeds is selected and no article is open.
15. **Cannot sign in.** An anonymous request for the article body (`GET /api/article/{id}`) is rejected. The deep-link document is not a public article page.
