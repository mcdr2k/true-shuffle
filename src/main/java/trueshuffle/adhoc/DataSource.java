package trueshuffle.adhoc;

/**
 * To be able to throw exceptions, this interface is used over Java's {@link java.util.function.Supplier} interface.
 */
public interface DataSource<T, E extends Exception> {
    T load() throws E;
}
