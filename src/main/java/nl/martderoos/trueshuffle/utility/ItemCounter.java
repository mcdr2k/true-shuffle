package nl.martderoos.trueshuffle.utility;

import java.util.HashMap;
import java.util.Map;

/**
 * Counts the amount of duplicate items.
 *
 * @param <K> the type of the data that it may hold.
 */
public class ItemCounter<K> {
    private final Map<K, Integer> counter;

    /**
     * Create a counter without any items.
     */
    public ItemCounter() {
        counter = new HashMap<>();
    }

    /**
     * Create a counter with the provided items.
     */
    public ItemCounter(Iterable<K> items) {
        this();
        addAll(items);
    }

    /**
     * Check if the counter contains any such item.
     *
     * @param item the item to look for.
     * @return true if there exists at least 1 such item.
     */
    public boolean contains(K item) {
        return counter.containsKey(item);
    }

    /**
     * Add an item to the counter.
     *
     * @param item the item to add.
     */
    public void add(K item) {
        counter.merge(item, 1, Integer::sum);
    }

    /**
     * Adds all items to this counter. It will call {@link #add(Object)} for each item in the provided list of items.
     *
     * @param items the items to add.
     */
    public void addAll(Iterable<K> items) {
        items.forEach(this::add);
    }

    /**
     * Reduces the count of the associated item by 1 if it exists.
     *
     * @param item The item to remove.
     * @return True if the counter of the associated value was <strong>at least</strong> 1 before this call was made,
     * false otherwise.
     */
    public boolean remove(K item) {
        Integer count = counter.get(item);
        if (count == null)
            return false;

        if (count == 1) {
            counter.remove(item);
        } else {
            counter.put(item, count - 1);
        }
        return true;
    }
}
