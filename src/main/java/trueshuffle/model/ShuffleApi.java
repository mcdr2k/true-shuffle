package trueshuffle.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.miscellaneous.PlaylistTracksInformation;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.IRequest;
import trueshuffle.paging.PageAggregator;
import trueshuffle.paging.SpotifyFuturePage;
import trueshuffle.requests.RequestHandler;
import trueshuffle.requests.exceptions.FatalRequestResponse;

import java.beans.Transient;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ShuffleApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShuffleApi.class);

    private final SpotifyApi api;
    private final User user;
    private final RequestHandler requestHandler;

    private final UserLibrary userLibrary;

    public ShuffleApi(final SpotifyApi api, User user) {
        Objects.requireNonNull(api);
        this.api = Objects.requireNonNull(api);
        this.requestHandler = new RequestHandler(this::refreshAccessToken);
        this.user = Objects.requireNonNull(user);
        userLibrary = new UserLibrary(this);
    }

    public synchronized List<PlaylistSimplified> streamUserPlaylists(int hardLimit) throws FatalRequestResponse {
        return PageAggregator.aggregate(new SpotifyFuturePage<>(
                (offset, limit) -> apiRequest(getApi().getListOfCurrentUsersPlaylists().offset(offset).limit(limit).build())
        ), hardLimit);
    }

    public synchronized Playlist streamPlaylist(String playlistId) throws FatalRequestResponse {
        return apiRequest(getApi().getPlaylist(playlistId).build());
    }

    public synchronized PlaylistSimplified streamPlaylistSimplified(String playlistId) throws FatalRequestResponse {
        return toSimplifiedPlaylist(streamPlaylist(playlistId));
    }

    public synchronized List<PlaylistSimplified> searchPlaylistByExactName(String playlistName, int hardLimit) throws FatalRequestResponse {
        return PageAggregator.aggregate(new SpotifyFuturePage<>(
                        (offset, limit) -> apiRequest(getApi().searchPlaylists(String.format("\"%s\"", playlistName)).offset(offset).limit(limit).build())
                ),
                hardLimit
        );
    }

    public static PlaylistSimplified toSimplifiedPlaylist(Playlist playlist) {
        return new PlaylistSimplified.Builder()
                .setCollaborative(playlist.getIsCollaborative())
                .setExternalUrls(playlist.getExternalUrls())
                .setTracks(new PlaylistTracksInformation.Builder().setTotal(playlist.getTracks().getTotal()).setHref(playlist.getTracks().getHref()).build())
                .setHref(playlist.getHref())
                .setId(playlist.getId())
                .setImages(playlist.getImages())
                .setName(playlist.getName())
                .setOwner(playlist.getOwner())
                .setPublicAccess(playlist.getIsPublicAccess())
                .setSnapshotId(playlist.getSnapshotId())
                .setType(playlist.getType())
                .setUri(playlist.getUri())
                .build();
    }

    public synchronized List<String> streamUserSavedTracksUris() throws FatalRequestResponse {
        return PageAggregator.aggregate(new SpotifyFuturePage<>(
                        (offset, limit) -> apiRequest(getApi().getUsersSavedTracks().offset(offset).limit(limit).build())
                ),
                2048
        ).stream().map(x -> x.getTrack().getUri()).collect(Collectors.toList());
    }

    // todo: maximum spotify playlist size is 10000
    // there is no limit on liked songs...
    public synchronized List<String> streamPlaylistTracksUris(String playlistId) throws FatalRequestResponse {
        return PageAggregator.aggregate(new SpotifyFuturePage<>(
                        (offset, limit) -> apiRequest(getApi().getPlaylistsItems(playlistId).offset(offset).limit(limit).build())
                ),
                2048
        ).stream().map(x -> x.getTrack().getUri()).collect(Collectors.toList());
    }

    public synchronized void addTracks(ShufflePlaylist playlist, List<String> tracks) throws FatalRequestResponse {
        var id = playlist.getPlaylistId();
        performTrackIteration(
                tracks,
                (nextTracks) -> apiRequest(getApi().addItemsToPlaylist(id, nextTracks).build())
        );
//        playlist.setTracksUris(tracks);
    }

    /**
     * @param id            The id of the playlist you wish to modify.
     * @param range_start   The index of the first song you wish to reorder.
     * @param insert_before The index at which you want to insert the tracks to reorder.
     * @param snapshot      The current snapshot you wish to modify.
     * @return The new snapshot id returned by Spotify.
     */
    public synchronized String reorderTrack(String id, int range_start, int insert_before, String snapshot) throws FatalRequestResponse {
        return apiRequest(getApi().reorderPlaylistsItems(id, range_start, insert_before)
                .range_length(1)
                .snapshot_id(snapshot)
                .build()).getSnapshotId();
    }

    public synchronized void removeTracks(ShufflePlaylist playlist, List<String> tracks) throws FatalRequestResponse {
        int removed = 0;
        var id = playlist.getPlaylistId();
        var snapshot = playlist.getSnapshotId();
        while (removed < tracks.size()) {
            int capacity = Math.min(100, tracks.size() - removed);
            JsonArray nextTracks = new JsonArray(capacity);

            for (int i = 0; i < capacity && i < tracks.size(); i++) {
                var jsonObject = new JsonObject();
                jsonObject.addProperty("uri", tracks.get(removed + i));
                nextTracks.add(jsonObject);
            }

            snapshot = apiRequest(getApi().removeItemsFromPlaylist(
                            id,
                            nextTracks)
                    .snapshotId(snapshot)
                    .build()).getSnapshotId();

            removed += capacity;
        }
    }

    private void performTrackIteration(List<String> items, TrackIterationPerformer performer) throws FatalRequestResponse {
        int consumed = 0;
        while (consumed < items.size()) {
            int capacity = Math.min(100, items.size() - consumed);
            JsonArray nextTracks = new JsonArray(capacity);

            for (int i = 0; i < capacity && i < items.size(); i++) {
                nextTracks.add(items.get(consumed + i));
            }

            performer.perform(nextTracks);
            consumed += capacity;
        }
    }

    private interface TrackIterationPerformer {
        void perform(JsonArray tracks) throws FatalRequestResponse;
    }

    public synchronized Playlist uploadPlaylist(String name, String description) throws FatalRequestResponse {
        return apiRequest(getApi()
                .createPlaylist(getUserId(), name)
                .collaborative(false)
                .public_(true)
                .description(description)
                .build());
    }

    public String getUserId() {
        return user.getId();
    }

    public String getDisplayName() {
        return user.getDisplayName();
    }

    public UserLibrary getUserLibrary() {
        return userLibrary;
    }

    // note: because this is synchronized, it means that it is impossible to send two api requests at the same time!
    private synchronized <T> T apiRequest(IRequest<T> request) throws FatalRequestResponse {
        return requestHandler.handleRequest(request);
    }

    private SpotifyApi getApi() {
        return api;
    }

    private void refreshAccessToken() throws FatalRequestResponse {
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
