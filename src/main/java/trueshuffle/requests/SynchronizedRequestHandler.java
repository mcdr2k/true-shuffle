package trueshuffle.requests;

import se.michaelthelin.spotify.requests.IRequest;
import trueshuffle.requests.exceptions.FatalRequestResponse;

/**
 * Synchronizes the {@link #handleRequest(IRequest)} function of {@link RequestHandler}.
 */
public class SynchronizedRequestHandler extends RequestHandler {

    public SynchronizedRequestHandler(TokenRefresher refresher) {
        super(refresher);
    }

    @Override
    public synchronized <T> T handleRequest(IRequest<T> request) throws FatalRequestResponse {
        return super.handleRequest(request);
    }
}
