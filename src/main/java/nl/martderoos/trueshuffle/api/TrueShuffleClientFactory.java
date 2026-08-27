package nl.martderoos.trueshuffle.api;

import nl.martderoos.trueshuffle.api.model.TrueShuffleClient;
import nl.martderoos.trueshuffle.internal.InternalTrueShuffleClient;

public class TrueShuffleClientFactory {
    private TrueShuffleClientFactory() {

    }

    public static TrueShuffleClient create(String cid, String secret, String redirectUri) {
        return InternalTrueShuffleClient.create(cid, secret, redirectUri);
    }
}
