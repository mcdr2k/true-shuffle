package nl.martderoos.trueshuffle.requests;

import nl.martderoos.trueshuffle.requests.exceptions.AuthorizationRevokedException;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import org.apache.hc.core5.http.ParseException;
import org.junit.jupiter.api.Test;
import se.michaelthelin.spotify.exceptions.detailed.*;
import se.michaelthelin.spotify.requests.IRequest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RequestHandlerTest {
    @Test
    public void testSucceedingRequest() throws Exception {
        var handler = new RequestHandler(null);
        var result = handler.handleRequest(forgeRequest(new GigaSupplier(5)));
        assertEquals(5, result);
    }

    @Test
    public void testRetryWhenServiceUnavailable() throws Exception {
        var handler = new RequestHandler(null);
        var result = handler.handleRequest(forgeRequest(new GigaSupplier(new ServiceUnavailableException(), 5)));
        assertEquals(5, result);
    }

    @Test
    public void testRetryWhenBadGateway() throws Exception {
        var handler = new RequestHandler(null);
        var result = handler.handleRequest(forgeRequest(new GigaSupplier(new BadGatewayException(), 5)));
        assertEquals(5, result);
    }

    @Test
    public void testRetryWhenTooManyRequestsException() throws Exception {
        var handler = new RequestHandler(null);
        var currentTime = System.currentTimeMillis();
        var result = handler.handleRequest(forgeRequest(new GigaSupplier(new TooManyRequestsException("wait at least 2 seconds my guy", 2), 5)));
        var endTime = System.currentTimeMillis();
        var difference = endTime - currentTime;
        assertTrue(TimeUnit.SECONDS.toMillis(2) <= difference);
        assertEquals(5, result);
    }

    @Test
    public void testRetryMultipleTimes() throws Exception {
        var handler = new RequestHandler(null);
        var result = handler.handleRequest(forgeRequest(new GigaSupplier(
                        new ServiceUnavailableException(),
                        new BadGatewayException(),
                        new TooManyRequestsException("no wait this time (other than the exponential backoff), get it done", 0), 5)
        ));
        assertEquals(5, result);
    }

    @Test
    public void testRefreshTokenOnUnauthorizedException() throws Exception {
        AccessTokenRefresher refresher = mock(AccessTokenRefresher.class);
        var handler = new RequestHandler(refresher);
        var request = forgeRequest(new UnauthorizedException(), 25);
        var result = handler.handleRequest(request);
        assertEquals(25, result);
        verify(request, times(2)).execute();
        verify(refresher, times(1)).refreshAccessToken();
    }

    @Test
    public void testFatalRequestResponseOnRefreshingAccessToken() throws Exception {
        var handler = new RequestHandler(null);
        var request = forgeRequest(new UnauthorizedException());
        assertThrows(FatalRequestResponseException.class, () -> handler.handleRequest(request));
        verify(request, times(1)).execute();
    }

    @Test
    public void testIOExceptionMapsToFatalRequest() throws Exception {
        var handler = new RequestHandler(null);
        var request = forgeRequest(new IOException());
        assertThrows(FatalRequestResponseException.class, () -> handler.handleRequest(request));
        verify(request, times(1)).execute();
    }

    @Test
    public void testParseExceptionMapsToFatalRequest() throws Exception {
        var handler = new RequestHandler(null);
        var request = forgeRequest(new ParseException());
        assertThrows(FatalRequestResponseException.class, () -> handler.handleRequest(request));
        verify(request, times(1)).execute();
    }

    @Test
    public void testBadRequestMapsToFatalRequest() throws Exception {
        var handler = new RequestHandler(null);
        var request = forgeRequest(new BadRequestException());
        assertThrows(FatalRequestResponseException.class, () -> handler.handleRequest(request));
        verify(request, times(1)).execute();
    }

    @Test
    public void testForbiddenExceptionMapsToAuthorizationRevoked() throws Exception {
        var handler = new RequestHandler(null);
        var request = forgeRequest(new ForbiddenException());
        assertThrows(AuthorizationRevokedException.class, () -> handler.handleRequest(request));
        verify(request, times(1)).execute();
    }

    @Test
    public void testInternalServerErrorMapsToFatalRequest() throws Exception {
        var handler = new RequestHandler(null);
        var request = forgeRequest(new InternalServerErrorException());
        assertThrows(FatalRequestResponseException.class, () -> handler.handleRequest(request));
        verify(request, times(1)).execute();
    }

    @Test
    public void testNotFoundMapsToFatalRequest() throws Exception {
        var handler = new RequestHandler(null);
        var request = forgeRequest(new NotFoundException());
        assertThrows(FatalRequestResponseException.class, () -> handler.handleRequest(request));
        verify(request, times(1)).execute();
    }

    @Test
    public void testUnknownExceptionMapsToFatalRequest() throws Exception {
        var handler = new RequestHandler(null);
        var request = forgeRequest(new UnknownException());
        assertThrows(FatalRequestResponseException.class, () -> handler.handleRequest(request));
        verify(request, times(1)).execute();
    }

    private static class UnknownException extends Exception {

    }

    private static IRequest<Object> forgeRequest(Object... data) throws Exception {
        return forgeRequest(new GigaSupplier(data));
    }

    @SuppressWarnings("unchecked")
    private static IRequest<Object> forgeRequest(GigaSupplier requestResults) throws Exception {
        var request = mock(IRequest.class);
        when(request.execute()).thenAnswer(invocation -> requestResults.get());
        return request;
    }

    private static class GigaSupplier {
        private final Object[] data;
        private int invocations = 0;

        private GigaSupplier(Object... data) {
            this.data = data;
        }

        public Object get() throws Throwable {
            if (invocations > data.length) return null;
            var result = data[invocations++];
            if (result instanceof Throwable) throw (Throwable) result;
            return result;
        }
    }
}
