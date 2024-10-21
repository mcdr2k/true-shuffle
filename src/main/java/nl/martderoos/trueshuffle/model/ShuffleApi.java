package nl.martderoos.trueshuffle.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.IRequest;
import nl.martderoos.trueshuffle.paging.PageAggregator;
import nl.martderoos.trueshuffle.paging.SpotifyFuturePage;
import nl.martderoos.trueshuffle.requests.RequestHandler;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponse;

import java.beans.Transient;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static nl.martderoos.trueshuffle.utility.PlaylistUtil.toSimplifiedPlaylist;

public class ShuffleApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShuffleApi.class);

    private final SpotifyApi api;
    private final User user;
    private final RequestHandler requestHandler;

    public ShuffleApi(final SpotifyApi api, User user) {
        Objects.requireNonNull(api);
        this.api = Objects.requireNonNull(api);
        this.requestHandler = new RequestHandler(this::refreshAccessToken);
        this.user = Objects.requireNonNull(user);
    }

    public Playlist streamPlaylist(String playlistId) throws FatalRequestResponse {
        return apiRequest(getApi().getPlaylist(playlistId).build());
    }

    public PlaylistSimplified streamPlaylistSimplified(String playlistId) throws FatalRequestResponse {
        return toSimplifiedPlaylist(streamPlaylist(playlistId));
    }

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

    public List<String> streamUserLikedTracksUris() throws FatalRequestResponse {
        return PageAggregator.aggregate(
                        new SpotifyFuturePage<>(
                                (offset, limit) -> apiRequest(getApi()
                                        .getUsersSavedTracks()
                                        .offset(offset)
                                        .limit(limit)
                                        .build()
                                )),
                        2048
                ).stream()
                .map(x -> x.getTrack().getUri())
                .collect(Collectors.toList());
    }

    // todo: maximum spotify playlist size is 10000
    // there is no limit on liked songs...
    public List<String> streamPlaylistTracksUris(String playlistId) throws FatalRequestResponse {
        return PageAggregator.aggregate(
                        new SpotifyFuturePage<>(
                                (offset, limit) -> apiRequest(getApi()
                                        .getPlaylistsItems(playlistId)
                                        .offset(offset)
                                        .limit(limit)
                                        .build()
                                )),
                        2048
                ).stream()
                .map(x -> x.getTrack().getUri())
                .collect(Collectors.toList());
    }

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
     * @param id            The id of the playlist you wish to modify.
     * @param range_start   The index of the first song you wish to reorder.
     * @param insert_before The index at which you want to insert the tracks to reorder.
     * @param snapshot      The current snapshot you wish to modify.
     * @return The new snapshot id returned by Spotify.
     */
    public String reorderTrack(String id, int range_start, int insert_before, String snapshot) throws FatalRequestResponse {
        return apiRequest(getApi()
                .reorderPlaylistsItems(id, range_start, insert_before)
                .range_length(1)
                .snapshot_id(snapshot)
                .build()
        ).getSnapshotId();
    }

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

    public Playlist uploadPlaylist(String playlistName, String playlistDescription) throws FatalRequestResponse {
        return apiRequest(getApi()
                .createPlaylist(getUserId(), playlistName)
                .collaborative(false)
                .public_(true)
                .description(playlistDescription)
                .build());
    }

    public String getUserId() {
        return user.getId();
    }

    public String getDisplayName() {
        return user.getDisplayName();
    }

    // note: because this is synchronized, it means that it is impossible to send two api requests at the same time!
    private synchronized <T> T apiRequest(IRequest<T> request) throws FatalRequestResponse {
        return requestHandler.handleRequest(request);
    }

    private SpotifyApi getApi() {
        return api;
    }

    private synchronized void refreshAccessToken() throws FatalRequestResponse {
        if (getApi().getRefreshToken() == null) {
            LOGGER.error("An attempt was made to refresh credentials for which we do not have a refresh token");
            throw new FatalRequestResponse(String.format("Could not refresh credentials for '%s', there was no refresh token", user.getDisplayName()));
        }

        var credentials = requestHandler.handleRequest(getApi().authorizationCodeRefresh().build());
        assignCredentials(credentials);
    }

    public void assignCredentials(AuthorizationCodeCredentials credentials) {
        assignCredentials(getApi(), credentials);
    }

    private static void assignCredentials(SpotifyApi api, AuthorizationCodeCredentials credentials) {
        if (credentials.getAccessToken() != null)
            api.setAccessToken(credentials.getAccessToken());

        if (credentials.getRefreshToken() != null)
            api.setRefreshToken(credentials.getRefreshToken());
    }

    @Transient
    public String getRefreshToken() {
        return api.getRefreshToken();
    }

    @Transient
    public String getAccessToken() {
        return api.getAccessToken();
    }
}
