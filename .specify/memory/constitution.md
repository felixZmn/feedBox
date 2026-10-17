# FeedBox Constitution

## Core Principles

### I. Self-Hosted Reader

FeedBox is a single-tenant, self-hosted feed reader. It MUST stay free of ads,
analytics, and any path that sends feed content, session cookies, or tokens to
a party other than the configured OIDC identity provider and the feed or icon
URLs that instance fetches for its operator.

Rationale: The README defines the product as self-hosted and free of ads,
tracking, and distractions. The schema has no user table. Content-Security-Policy
limits scripts to `'self'`.

Compliance: A tracker, a third-party script or style origin, or an outbound call
that carries article bodies, cookies, or tokens is non-compliant. OIDC traffic
to the configured issuer, and fetches of subscribed feeds and icons, are the
allowed exceptions.

### II. Layered Modules

Backend code MUST keep the resource → service → repository split. Resources
validate and map HTTP. Services own behavior. Repositories own SQL and translate
database failures into `DataAccessException`. Domain models MUST be records.
The UI MUST remain vanilla ES modules served by the application. A UI framework
or frontend bundler MUST NOT be added.

Rationale: The backend under `de._0x2b` is organized as resource, service,
repository, model, and job packages. `package.json` exists to run `node --test`.
The shell loads `scripts/main.js` as an ES module.

Compliance: A resource does not embed SQL. A repository does not choose an HTTP
status. New UI behavior lands in an ES module under `META-INF/resources/scripts`
and is loaded by the existing shell.

### III. Explicit Failures

Failures MUST surface as domain exceptions (`NotFoundException`,
`DataAccessException`, `DuplicateEntityException`, `InvalidFeedException`, or a
new domain exception of the same kind) or as JAX-RS client errors, mapped to
the matching HTTP status. Callers MUST NOT use `null`, `-1`, or HTTP 200 to
mean failure. Unexpected errors MUST be logged server-side and returned as an
empty 500, without a stack or internal detail in the body.

Rationale: `AbstractRepository` refuses to turn SQL errors into empty results.
`UnhandledExceptionMapper` logs the throwable and returns an empty 500.

Compliance: A new failure path has an exception type and, when it crosses HTTP,
a mapper or a JAX-RS status. Tests assert that status, not a sentinel payload.

### IV. Hostile Input and Safe Fetch

Feed, OPML, and icon bytes are hostile. Every server-side fetch MUST go through
`OutboundHttpClient` and `UrlSafety` (http or https, allowed ports, blocked
private addresses, redirect checks, and a response size cap). Article HTML MUST
pass through `sanitizeHTML` before display. UI strings MUST be inserted with
the `html` tagged template. Visibility and selection MUST be toggled with
`classList`. Feed-supplied navigation targets MUST be checked with
`isSafeHttpUrl`. The committed security headers, including
Content-Security-Policy, MUST stay intact. Icon responses MUST keep their
tighter `default-src 'none'` policy.

Rationale: Subscribed content is attacker-controlled. SSRF checks, vendored
DOMPurify, and the header block in `application.properties` are the controls
that exist today.

Compliance: A new outbound fetch that bypasses `OutboundHttpClient`, a view
that assigns unsanitized feed HTML, or a CSP change that widens `script-src`
or `object-src` without an explicit spec exception is non-compliant.

### V. Authenticated by Default

`quarkus.security.jaxrs.deny-unannotated-endpoints` stays enabled. Every new
JAX-RS method MUST be `@Authenticated`. `@PermitAll` is allowed only for an
explicit public route, and the path policy in `application.properties` MUST
match it. The public surface is the SPA shell and static assets, `/api/public/*`,
and `/q/*` health. Authentication is Quarkus OIDC web-app with authorization
code and PKCE. Tokens MUST remain in encrypted httpOnly cookies. The SPA MUST
NOT receive them. Any user who completes login at the configured identity
provider has full access. Further restriction MUST be enforced at the identity
provider. Secrets MUST live in `application-local.properties` or the
environment, never in committed `application.properties`, and MUST NOT be
logged.

Rationale: The README documents single-tenant OIDC and the cookie session.
`AuthPathPolicyIT` locks the public versus authenticated split. There is no
user or role table.

Compliance: A new API without `@Authenticated`, or without a documented
`@PermitAll` and matching path policy, is non-compliant. A log line that
includes a cookie, token, client secret, or encryption secret is non-compliant.
Per-user authorization is a constitution change.

### VI. Schema and Library Integrity

PostgreSQL schema changes MUST be new Flyway migrations under
`src/main/resources/db/migration`. Applied migrations MUST NOT be edited or
deleted. Migrations run at startup. A new list filter MUST add an index, or a
short comment stating why it does not need one. Article list endpoints MUST
return summaries and stay paginated. Full article HTML MUST be returned only
from the single-article read. Runtime code MUST NOT delete article rows except
as a cascade of deleting the owning feed or folder. A migration MUST NOT
discard article rows except to satisfy a new uniqueness constraint, and then
it MUST keep the newest row.

Rationale: V1 through V6 are forward-only and run via Flyway at startup.
`ArticleSummary` omits article bodies. Lists use a fixed page size. Feed and
folder deletes cascade to articles. V5 removed older duplicate rows and kept
the newest id. There is no unread-retention job.

Compliance: Rewriting an applied migration, returning article HTML from a list,
or adding a purge of unread articles is non-compliant. Deleting a feed or
folder may remove its articles.

### VII. Regression Tests

A behavior change MUST add or update a unit test. A change to persistence,
authentication, or outbound HTTP MUST add or update an integration test (`*IT`,
Failsafe, PostgreSQL via Dev Services). Frontend behavior that can be exercised
without a browser MUST be covered by `node --test` under `src/test/js`. Pull
requests and the release workflow MUST stay green on
`mvn verify -DskipITs=false` and `npm run test:frontend`.

Rationale: `pom.xml` splits Surefire unit tests from Failsafe integration tests,
and `skipITs` defaults to true for local `mvn test`. CI enables integration
tests and the frontend script. Strict test-first ordering is not an established
practice.

Compliance: A behavior change with no test, or a persistence, auth, or outbound
HTTP change covered only by a Mockito unit test, is non-compliant. A coverage
percentage is not required. No lint or typecheck gate is configured.

### VIII. Bounded Work

List reads MUST stay paginated. Request size, outbound response size, redirect
count, timeout, refresh concurrency, OPML import concurrency, and outlines per
import MUST stay bounded by configuration. The UI MUST update the affected row
or badge in place instead of rebuilding a pane.

Rationale: Caps live in `application.properties` (`app.http.*`,
`refresh.concurrency`, `import.concurrency`, `import.max-outlines`). Article
search length is capped in `ArticleResource`. Unread badges are updated on the
existing node.

Compliance: An unbounded query, an uncapped upload or import, or a refresh path
with unlimited parallelism is non-compliant.

### IX. Apple Human Interface Guidelines

Every UI change and every new UI feature MUST follow Apple's Human Interface
Guidelines. The requirement is absolute. A UI change includes layout, visual
design, motion, interface copy, interaction, and any new or changed control.
A spec, plan, or implementation that touches the UI and does not conform MUST
NOT proceed unless the spec records an explicit exception to this constitution.

Applicable demands are clarity, deference, depth, consistency, and comfortable
touch targets. Concrete FeedBox checks are in the Interface section.

Rationale: The reader is a direct-manipulation interface on desktop and mobile.
The shell already uses the system font, accent color, radii, light and dark
appearance, visible focus, named controls, and reduced motion. Later UI work
has to meet that same standard.

Compliance: A UI change or new UI feature is non-compliant when it contradicts
the Human Interface Guidelines. That includes unclear hierarchy, inconsistent
controls, targets that are uncomfortable to use, motion that ignores
reduced-motion, or chrome that abandons the established FeedBox appearance.
Backend-only changes are outside this principle.

## Interface

Principle IX applies to every UI change and every new UI feature. Conformity
with Apple's Human Interface Guidelines is mandatory for that work. New and
changed chrome MUST reuse the tokens in `style.css` (system font, accent,
radii, and light and dark via `prefers-color-scheme`). Interactive controls
MUST be keyboard reachable, show a visible focus, and have an accessible name.
Motion MUST respect `prefers-reduced-motion`.

An HTTP API change and the SPA call site MUST land in the same change. The API
has no separate public version. The bundled SPA is the client.

Rationale: `style.css` defines the tokens, color scheme, `focus-visible`, and
reduced-motion rules. `index.html` names controls with `aria-label`. The
application and the SPA ship in one image.

## Dependencies, Configuration, and Release

A new dependency MUST NOT duplicate a capability the JDK or Quarkus already
provides. Backend dependencies MUST be managed by the Quarkus BOM unless no
Quarkus-managed artifact exists. The frontend MUST NOT gain an npm runtime
dependency. DOMPurify stays vendored and is updated by
`scripts/update-dompurify.sh`. A new configuration key MUST be added to
`application.properties` with a safe default, to the README parameter table,
and to the Helm chart in the `feedbox-chart` repository.

Versions follow SemVer. Git tags are the source of truth. `pom.xml` keeps
`0.0.0-SNAPSHOT` via `${revision}`. Release commits MUST use conventional
subjects so the release workflow can bump the version. `BREAKING CHANGE:` or
`type!:` is a major bump. The release artifact is the Quarkus native image
published to GHCR. Operational status is the SmallRye health endpoint at
`/q/health`, plus server logs that omit secrets. Exporting telemetry that
includes feed content, cookies, or tokens is forbidden by Principle I. Any
other telemetry addition MUST be justified in the spec.

Rationale: `release.md` and `.github/workflows/release.yml` derive the tag from
conventional commits and publish the native image. Renovate automerges
non-major dependency updates after CI. The Helm chart is published from a
separate repository and is bumped manually.

## Governance

This constitution is the governance source for FeedBox specifications, plans,
and implementations. Where it conflicts with an incidental pattern in the code,
the constitution wins until the code is brought in line or the constitution is
amended.

Amendments MUST update this file and the version line.

- MAJOR: a principle is removed or redefined so existing specs would become
  non-compliant.
- MINOR: a principle or section is added, or guidance is materially expanded.
- PATCH: wording, clarification, or a non-semantic fix.

Ratification of an amendment is the merge of the constitution change.
Specifications MUST cite any principle they cannot meet and treat that gap as
an explicit exception. Review of a spec, plan, or implementation checks the
principles above. A departure from the current layering, a new dependency, a
public route, or a new configuration key MUST be justified in the spec.

**Version**: 1.1.0 | **Ratified**: 2026-10-17 | **Last Amended**: 2026-10-17
