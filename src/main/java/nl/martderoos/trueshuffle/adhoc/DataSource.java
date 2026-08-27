package nl.martderoos.trueshuffle.adhoc;

/**
 * Interface that describes a source of data that may be loaded at any time.
 * @param <T> the type of data this source may produce.
 * @param <E> the exception this source may throw upon attempting to load new data.
 */
public interface DataSource<T, E extends Exception> {
    T load() throws E;
}
