package nl.martderoos.trueshuffle;

import nl.martderoos.trueshuffle.api.TrueShuffleClient;

public class TrueShuffleClientFactory {
    private TrueShuffleClientFactory() {

    }

    public static TrueShuffleClient create(String cid, String secret, String redirectUri) {
        return InternalTrueShuffleClient.create(cid, secret, redirectUri);
    }
}
