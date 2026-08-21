# Code Quality Improvements

The repository is already fairly well-structured, but these improvements would materially raise code quality.

1. **Fix API boundary mutability**
   - `UserLibrary.getUserLikedTracksUris()` and `ShufflePlaylist.getPlaylistTracksUris()` expose cached mutable lists.
   - `TrueShuffleUser.getImages()` and `SpotifyResultPage.getItems()` expose arrays directly.
   - Return defensive copies or unmodifiable collections so callers cannot corrupt cached state.

2. **Strengthen input validation**
   - Validate `hardLimit`, playlist IDs, names, offsets, and page objects consistently.
   - `PageAggregator.aggregate(..., hardLimit < 0)` can produce an invalid `ArrayList` capacity.
   - Reject invalid timeout values in `LazyExpiringData`.

3. **Improve retry behavior**
   - `RequestHandler` treats `MAX_RETRIES` as retries after the initial attempt.
   - Interrupted retry sleeps restore the thread flag and terminate the request explicitly.
   - Exponential backoff includes bounded jitter to reduce synchronized retry spikes.
   - Wrapped fatal failures preserve the original exception as the cause.

4. **Reduce playlist shuffle API traffic**
   - `ShufflePlaylist.shuffleInPlace()` performs one Spotify reorder request per track.
   - Large playlists can generate thousands of requests and quickly hit rate limits. Consider a more efficient reorder strategy or document/enforce a practical maximum.

5. **Improve cache correctness**
   - `UserLibrary`'s playlist name index can become stale after playlist renames or external changes.
   - Add explicit refresh/invalidation methods and clarify when callers should use them.
   - Return copies from cached collections so cache internals cannot be modified externally.

6. **Expand edge-case tests**
   Add tests for:
   - Empty and negative limits
   - Null or empty Spotify pages
   - Pagination with short or inconsistent pages
   - Interrupted retry sleeps
   - Token refresh failures and concurrent refreshes
   - Cache invalidation after playlist updates
   - Duplicate tracks and playlists exceeding configured hard limits

7. **Modernize and automate quality checks**
   - Replace the obsolete `mockito-all:1.10.19` dependency with current Mockito artifacts compatible with JUnit 5.
   - Add Maven compiler warnings, dependency convergence or vulnerability checks, and a formatter or Checkstyle plugin.
   - Add CI to run `mvn verify`, tests, Javadocs, and dependency checks on every change.

8. **Improve error modeling**
   - Avoid broad `catch (Exception)` and especially `catch (Throwable)` where possible.
   - Preserve error causes and distinguish interruption, programmer errors, API failures, and recoverable request failures more explicitly.

The highest-value first steps are defensive copies at public boundaries, retry and interruption fixes, stronger validation, and tests around cache and pagination behavior.
