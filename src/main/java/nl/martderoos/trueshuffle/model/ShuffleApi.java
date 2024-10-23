package nl.martderoos.trueshuffle.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import nl.martderoos.trueshuffle.paging.PageAggregator;
import nl.martderoos.trueshuffle.paging.SpotifyFuturePage;
import nl.martderoos.trueshuffle.requests.RequestHandler;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.User;
import se.michaelthelin.spotify.requests.IRequest;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static nl.martderoos.trueshuffle.utility.PlaylistUtil.toSimplifiedPlaylist;

/**
 * Encapsulates the {@link SpotifyApi} to supply functions necessary for TrueShuffle operations. This class is
 * thread-safe because it is (mostly) stateless. The only state that is kept is the access token of the {@link SpotifyApi}.
 * In the case that a new access token is required, this class will make sure to only refresh the access token once
 * when necessary.
 * <br><br>
 * All operations that require leveraging the {@link SpotifyApi} may result in {@link FatalRequestResponse} due to
 * unrecoverable errors from Spotify. Some operations require us to send multiple requests to Spotify because
 * they have constraints on the amount of data you can send and receive within a single request.
 */
public class ShuffleApi {
    /**
     * The maximum number of tracks a regular Spotify playlist can have. It is currently 10_000.
     */
    public static final int MAXIMUM_PLAYLIST_SIZE = 10_000;
    /**
     * The maximum number of tracks a user can save (like). Currently, there exists no limit.
     */
    public static final int MAXIMUM_LIKED_SONGS_SIZE = Integer.MAX_VALUE;

    private static final Logger LOGGER = LoggerFactory.getLogger(ShuffleApi.class);

    private final SpotifyApi api;
    private final User user;
    private final RequestHandler requestHandler;
    private volatile long accessTokenValidUntilAtLeast;

    public ShuffleApi(final SpotifyApi api, User user) {
        Objects.requireNonNull(api);
        this.api = Objects.requireNonNull(api);
        this.requestHandler = new RequestHandler(this::refreshAccessToken);
        this.user = Objects.requireNonNull(user);
    }

    /**
     * Stream a uniquely identifiable {@link Playlist} from Spotify
     *
     * @param playlistId the unique identifier of the playlist to stream
     */
    public Playlist streamPlaylist(String playlistId) throws FatalRequestResponse {
        return apiRequest(getApi().getPlaylist(playlistId).build());
    }

    /**
     * Stream a uniquely identifiable {@link Playlist} from Spotify and convert it to a {@link PlaylistSimplified}
     *
     * @param playlistId the unique identifier of the playlist to stream
     */
    public PlaylistSimplified streamPlaylistSimplified(String playlistId) throws FatalRequestResponse {
        return toSimplifiedPlaylist(streamPlaylist(playlistId));
    }

    /**
     * Stream all playlists this user has in their library. The order by which the playlists are returned by Spotify
     * is not specified. Note that this operation may require multiple requests if the hard limit exceeds 50 due to
     * Spotify's constraints.
     *
     * @param hardLimit the hard limit on the amount of playlists to stream
     */
    public List<PlaylistSimplified> streamUserPlaylists(int hardLimit) throws FatalRequestResponse {
        return PageAggregator.aggregate(
                new SpotifyFuturePage<>(
                        (offset, limit) -> apiRequest(getApi()
                                .getListOfCurrentUsersPlaylists()
                                .offset(offset)
                                .limit(limit)
                                .build()
                        )),
                hardLimit);
    }

    /**
     * Search for a playlist by its exact name.
     *
     * @param playlistName the exact name of the playlist to search for
     * @param hardLimit    the hard limit on the amount of search results
     * @return the search results
     */
    public List<PlaylistSimplified> searchPlaylistByExactName(String playlistName, int hardLimit) throws FatalRequestResponse {
        return PageAggregator.aggregate(
                new SpotifyFuturePage<>(
                        (offset, limit) -> apiRequest(getApi()
                                .searchPlaylists(String.format("\"%s\"", playlistName))
                                .offset(offset)
                                .limit(limit)
                                .build()
                        )),
                hardLimit
        );
    }

    /**
     * Stream all user's saved (liked) tracks. Note that Spotify has no limit on the maximum number of tracks
     * a user may save.
     *
     * @param hardLimit the hard limit on the amount of tracks to stream
     * @return a list of track URIs
     */
    public List<String> streamUserLikedTracksUris(int hardLimit) throws FatalRequestResponse {
        return PageAggregator.aggregate(
                        new SpotifyFuturePage<>(
                                (offset, limit) -> apiRequest(getApi()
                                        .getUsersSavedTracks()
                                        .offset(offset)
                                        .limit(limit)
                                        .build()
                                )),
                        hardLimit
                ).stream()
                .map(x -> x.getTrack().getUri())
                .collect(Collectors.toList());
    }

    /**
     * Stream a specific playlist's tracks. Note that Spotify imposes a limit on the maximum number of tracks a playlist
     * can have, namely 10_000.
     *
     * @param playlistId the playlists unique identifier
     * @param hardLimit  the hard limit on the number of tracks to retrieve for the playlist
     * @return a list of track URIs
     */
    public List<String> streamPlaylistTracksUris(String playlistId, int hardLimit) throws FatalRequestResponse {
        if (hardLimit > MAXIMUM_PLAYLIST_SIZE) hardLimit = MAXIMUM_PLAYLIST_SIZE;
        return PageAggregator.aggregate(
                        new SpotifyFuturePage<>(
                                (offset, limit) -> apiRequest(getApi()
                                        .getPlaylistsItems(playlistId)
                                        .offset(offset)
                                        .limit(limit)
                                        .build()
                                )),
                        hardLimit
                ).stream()
                .map(x -> x.getTrack().getUri())
                .collect(Collectors.toList());
    }

    /**
     * Add tracks to a playlist. Note that this method may send multiple requests because Spotify imposes a size limit
     * of 100 on the amount of tracks to add in one request.
     *
     * @param playlistId the unique identifier of the playlist to add tracks to
     * @param snapshot   the current snapshot identifier of the playlist
     * @param tracks     the URIs of the tracks to add
     * @return the new snapshot identifier
     */
    public String addTracks(String playlistId, String snapshot, List<String> tracks) throws FatalRequestResponse {
        int consumed = 0;

        while (consumed < tracks.size()) {
            int capacity = Math.min(100, tracks.size() - consumed);
            JsonArray nextTracks = new JsonArray(capacity);

            for (int i = 0; i < capacity && i < tracks.size(); i++) {
                nextTracks.add(tracks.get(consumed + i));
            }

            snapshot = apiRequest(getApi()
                    .addItemsToPlaylist(playlistId, nextTracks)
                    .build()
            ).getSnapshotId();

            consumed += capacity;
        }
        return snapshot;
    }

    /**
     * Remove tracks from a playlist. Note that this method may send multiple requests because Spotify imposes a size limit
     * of 100 on the amount of tracks to remove in one request.
     *
     * @param playlistId the unique identifier of the playlist to remove tracks from
     * @param snapshot   the current snapshot identifier of the playlist
     * @param tracks     the URIs of the tracks to remove
     * @return the new snapshot identifier
     */
    public String removeTracks(String playlistId, String snapshot, List<String> tracks) throws FatalRequestResponse {
        int removed = 0;

        while (removed < tracks.size()) {
            int capacity = Math.min(100, tracks.size() - removed);
            JsonArray nextTracks = new JsonArray(capacity);

            for (int i = 0; i < capacity && i < tracks.size(); i++) {
                var jsonObject = new JsonObject();
                jsonObject.addProperty("uri", tracks.get(removed + i));
                nextTracks.add(jsonObject);
            }

            snapshot = apiRequest(getApi().removeItemsFromPlaylist(
                            playlistId,
                            nextTracks)
                    .snapshotId(snapshot)
                    .build()).getSnapshotId();

            removed += capacity;
        }
        return snapshot;
    }

    /**
     * Reorder a single track. Note that Spotify does not allow multiple single track reorders in a single request, it
     * only allows reordering ranges of tracks.
     *
     * @param playlistId    The unique identifier of the playlist
     * @param range_start   the current index of the song you wish to reorder
     * @param insert_before the new index of the track to reorder
     * @param snapshot      the current snapshot identifier of the playlist
     * @return the new snapshot identifier
     */
    public String reorderTrack(String playlistId, int range_start, int insert_before, String snapshot) throws FatalRequestResponse {
        return apiRequest(getApi()
                .reorderPlaylistsItems(playlistId, range_start, insert_before)
                .range_length(1)
                .snapshot_id(snapshot)
                .build()
        ).getSnapshotId();
    }

    /**
     * Create a new playlist for the user bound to this {@link ShuffleApi}. The playlist will be public and
     * non-collaborative.
     *
     * @param playlistName        the name of the new playlist (does not have to be unique)
     * @param playlistDescription the description of the new playlist
     * @return the newly created playlist's details
     */
    public Playlist uploadPlaylist(String playlistName, String playlistDescription) throws FatalRequestResponse {
        return apiRequest(getApi()
                .createPlaylist(getUserId(), playlistName)
                .collaborative(false)
                .public_(true)
                .description(playlistDescription)
                .build());
    }

    /**
     * @return the unique user identifier
     */
    public String getUserId() {
        return user.getId();
    }

    /**
     * @return the user's display name (non-unique)
     */
    public String getDisplayName() {
        return user.getDisplayName();
    }

    private <T> T apiRequest(IRequest<T> request) throws FatalRequestResponse {
        return requestHandler.handleRequest(request);
    }

    private SpotifyApi getApi() {
        return api;
    }

    private synchronized void refreshAccessToken() throws FatalRequestResponse {
        // prevent another refresh if it was recently refreshed
        if (System.currentTimeMillis() < accessTokenValidUntilAtLeast) {
            return;
        }
        if (api.getRefreshToken() == null) {
            LOGGER.error("An attempt was made to refresh credentials for which we do not have a refresh token");
            throw new FatalRequestResponse(String.format("Could not refresh credentials for '%s', there was no refresh token", user.getDisplayName()));
        }

        var credentials = requestHandler.handleRequest(getApi().authorizationCodeRefresh().build());
        assignCredentials(credentials);
    }

    /**
     * Update the credentials of this api. Note that this function should only be called from TrueShuffle.
     * Any attempt to assign (likely invalid) credentials will lead to undefined behavior.
     */
    public synchronized void assignCredentials(AuthorizationCodeCredentials credentials) {
        if (credentials.getAccessToken() != null)
            api.setAccessToken(credentials.getAccessToken());

        if (credentials.getRefreshToken() != null)
            api.setRefreshToken(credentials.getRefreshToken());

        var minimumSeconds = Math.min(300, credentials.getExpiresIn());
        accessTokenValidUntilAtLeast = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(minimumSeconds);
    }
}
