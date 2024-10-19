package trueshuffle.paging;

import se.michaelthelin.spotify.model_objects.specification.Paging;

public class SpotifyPage<T> {
    private final Paging<T> paging;

    private final SpotifyFuturePage<T> previous;
    private final SpotifyFuturePage<T> next;

    public SpotifyPage(SpotifyPageConsumer<T> consumer, Paging<T> paging) {
        this.paging = paging;

        if (paging.getPrevious() == null) {
            this.previous = null;
        } else {
            int count = getItems().length;
            this.previous = new SpotifyFuturePage<>(consumer, getOffset() - count, count);
        }

        if (paging.getNext() == null) {
            this.next = null;
        } else {
            this.next = new SpotifyFuturePage<>(consumer, getOffset() + getItems().length, getLimit());
        }
    }

    public T[] getItems() {
        return paging.getItems();
    }

    public int getTotal() {
        return paging.getTotal();
    }

    public int getOffset() {
        return paging.getOffset();
    }

    public int getLimit() {
        return paging.getLimit();
    }

    public SpotifyFuturePage<T> getPrevious() {
        return previous;
    }

    public SpotifyFuturePage<T> getNext() {
        return next;
    }
}
