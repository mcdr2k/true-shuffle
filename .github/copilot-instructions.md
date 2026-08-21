# Copilot instructions for True Shuffle

## Build, test, and lint

- Requires Java 17 and Maven.
- Build the library and generate the JARs, sources JAR, and Javadoc JAR with `mvn package`.
- Install the artifact into the local Maven repository with `mvn install`.
- Run the full test suite with `mvn test`.
- Run one test class with `mvn -Dtest=ShuffleUtilTest test`.
- Run one test method with `mvn -Dtest=ShuffleUtilTest#testShuffleDiffUniqueTracks test`.
- No separate lint or formatting plugin is configured in `pom.xml`; keep changes compatible with the Maven compiler and Javadoc plugin.

## Architecture

True Shuffle is a reusable, non-executable Java library; it has no `main` method. `TrueShuffleClient` owns Spotify client configuration, initialization, authorization-code flow, and the map of authorized `TrueShuffleUser` instances. Each user owns a `ShuffleApi` and a `UserLibrary`.

`ShuffleApi` is the Spotify-facing facade. It wraps the third-party `SpotifyApi`, translates common API operations into domain-friendly methods, and uses `RequestHandler` for retry, rate-limit backoff, authorization errors, and synchronized access-token refresh. `PageAggregator` and `SpotifyFuturePage` abstract Spotify's paginated responses.

`UserLibrary` maintains lazy-expiring data for liked-track URIs and a playlist index. The index maps playlist IDs and names to `ShufflePlaylist` objects; it can load playlists from Spotify and add newly created or discovered playlists. `ShufflePlaylist` lazily loads playlist metadata/tracks and performs mutation operations when the linked user owns the playlist.

`TrueShuffleJob` is the sealed immutable job-description base. `TrueShuffleLikedJob` shuffles liked tracks into a uniquely named target playlist, while `TrueShufflePlaylistJob` chooses in-place shuffling, a supplied target, or a generated `- TrueShuffle` target based on ownership and constructor arguments. `TrueShuffleJobExecutor` schedules jobs and returns `TrueShuffleJobExecution`, which owns lifecycle operations and status reporting. Jobs use `ShuffleUtil.shuffleInto` to diff playlist contents before shuffling.

## Repository-specific conventions

- Keep production code under the `nl.martderoos.trueshuffle` package hierarchy and preserve the existing package separation: API/domain models, jobs, paging, requests, exceptions, and utilities.
- Public stateful components are designed to be thread-safe. Preserve `synchronized` boundaries around credential updates, cached data access, playlist mutations, and client initialization; jobs themselves are immutable and schedule work through an injected `Executor`.
- Use the existing exception taxonomy. Spotify/request failures should pass through `RequestHandler` and become the appropriate `requests.exceptions` or domain exception rather than being silently swallowed.
- Spotify limits are encoded in the domain layer: pages generally request at most 50 items, playlist track changes are sent in batches of 100, and cached liked tracks and playlist tracks use hard limits of 2,000. Reuse these helpers/constants instead of duplicating limit logic.
- Playlist updates are diff-based and preserve duplicate-track counts through `ItemCounter`; removals happen before additions, and a successful content update invalidates the relevant lazy caches before subsequent reads.
- Ownership controls mutability. A `ShufflePlaylist` may represent visible but non-owned playlists, so check `isMutable()`/`UserLibrary.isOwner(...)` before mutation and preserve the job status behavior for invalid targets.
- Authorization scopes and callback behavior are defined by `TrueShuffleClient.getAuthorizationURI(...)`; callers provide and validate the OAuth `state`, while credentials are represented by `TrueShuffleUserCredentials`.
- Tests use JUnit Jupiter and Mockito, follow `*Test` class names, and commonly mock Spotify/domain collaborators to verify request counts, retry behavior, batching, cache invalidation, and shuffle diffs.
