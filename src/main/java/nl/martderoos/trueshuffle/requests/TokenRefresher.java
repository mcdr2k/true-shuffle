package nl.martderoos.trueshuffle.requests;

public interface TokenRefresher {
    void refreshToken() throws Exception;
}
