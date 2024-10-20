package nl.martderoos.trueshuffle.utility;

import java.util.HashMap;
import java.util.Map;

public class ItemCounter<K> {
    private final Map<K, Integer> counter;

    public ItemCounter() {
        counter = new HashMap<>();
    }

    public ItemCounter(Iterable<K> values) {
        this();
        addAll(values);
    }

    public boolean contains(K value) {
        return counter.containsKey(value);
    }

    public void add(K value) {
        counter.merge(value, 1, Integer::sum);
    }

    public void addAll(Iterable<K> values) {
        values.forEach(this::add);
    }

    /**
     * Reduces the count of the associated value by 1 if it exists.
     *
     * @param value The key of the counter to look for.
     * @return True if the counter of the associated value was <strong>at least</strong> 1 before this call was made,
     * false otherwise.
     */
    public boolean remove(K value) {
        Integer count = counter.get(value);
        if (count == null)
            return false;

        if (count == 1) {
            counter.remove(value);
        } else {
            counter.put(value, count - 1);
        }
        return true;
    }
}
