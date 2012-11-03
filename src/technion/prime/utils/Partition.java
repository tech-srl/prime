package technion.prime.utils;

// Code taken from http://code.google.com/p/flexigraph/
// Originally licensed under Apache License, Version 2.0
// Reproduced here unmodified, except for this comment and package declaration

/**
 * Implementation of disjoint-set data structure, providing merge(merge)/find operations, in
 * order to be able to track whether two elements belong in the same set or not, and
 * to efficiently merge two sets.
 *
 * <p>The set that contains a particular element {@code e} (of type {@code Partition}
 * is accessed by {@code e.find()}.
 * Note that the returned set is merely an element itself, which is used as a representative of
 * the set. So, if two elements have the same representative, they belong to the same set.
 * Otherwise, they belong to different sets. Note that the elements of a particular set cannot be
 * queried directly. Thus, perhaps confusingly, a {@code Partition} represents both a single
 * element <em>and</em> potentially a set of elements, in the particular partition happens
 * to be chosen as the representative of its set.
 *
 * <p>A <em>partition</em> is initially {@link #singleton(Object) created as a singleton}.
 * It accepts an arbitrary (user-defined) value (of type {@code <E>})
 * that is permanently associated with the created partition, and can be accessed by {@link #value()}.
 * A created partition is independent from any other created partition. This means that the return value
 * of {@link #find()} is unique, and method {@link #areMerged(Partition, Partition)}
 * always returns false when given such a partition as an argument.
 *
 * <p>A partition {@code p1} may be <em>merged</em> with another partition {@code p2} by
 * {@code p1.merge(p2);} (or, equivalently, {@code p2.merge(p1);}). From that point on,
 * this invariant is created: {@code p1.find() == p2.find()} which remains true regardless of
 * other future merge operations. Similarly, {@code areMerged(p1, p2)} will always return {@code true}.
 *
 * <p>All methods throw {@code NullPointerException} if given {@code null} arguments.
 *
 * @param <E> the type of the arbitrary (user-defined) element associated with a partition
 * @author Andreou Dimitris, email: jim.andreou (at) gmail (dot) com
 * @see <a href="http://en.wikipedia.org/wiki/Disjoint-set_data_structure">
 * Disjoint-set data structure article on Wikipedia</a>
 */
public class Partition<E> {
    private Partition<?> parent;
    private int rank;
    private final E value;
    
    private Partition(E value) {
        this.value = value;
        this.parent = this;
    }

    /**
     * Creates a partition of a singleton set, containing only itself. The created
     * partition is permanently associated with the specified value, which can
     * be accessed by {@linkplain #value()}.
     *
     * @param <E> the type of the user-defined value
     * @param value the value to associate with the created partition
     * @return the created partition
     */
    public static <E> Partition<E> singleton(E value) {
        return new Partition<E>(value);
    }

    /**
     * Returns the value associated with this partition upon creation.
     *
     * @return the value associated with this partition upon creation
     */
    public E value() {
        return value;
    }

    /**
     * Merges this partition with another one. This has the following implications:
     * <ul>
     * <li>{@code this.find() == other.find()} will be true, forever
     * <li>{@code Partition.areMerged(this, other)} will return true, forever
     * </ul>
     * @param other the partition to merge this partition with
     * @return a partition representing the merged partitions. It will be either equal to either
     *      {@code this.find()} or {@code other.find()}, i.e. one representative of the partitions
     *      which will be elected to represent the union
     */
    public Partition<?> merge(Partition<?> other) {
        Partition<?> root1 = other.find();
        Partition<?> root2 = find();
        if (root1.rank < root2.rank) {
            root1.parent = root2;
            return root2;
        } else if (root1.rank > root2.rank) {
            root2.parent = root1;
            return root1;
        } else {
            root2.parent = root1;
            root1.rank++;
            return root1;
        }
    }

    /**
     * Returns the unique representative of the set that this element belongs to. The
     * returned instance can be compared with the representative of another element,
     * to check whether the two elements belong to the same set (when the two
     * representatives will be identical)
     * 
     * @return the unique representative of the set that this partition belongs to
     */
    public Partition<?> find() {
        //A single-pass path compressing algorithm
        Partition<?> current = this;
        Partition<?> last = this;
        while (current.parent != current) {
            last.parent = current.parent; //initially a no-op
            last = current;
            current = current.parent;
        }
        return current;
    }

    /**
     * Checks whether the two elements have been merged.
     *
     * <p>Equivalent to {@code partition1.find() == partition2.find()}.
     * @param partition1 the first element
     * @param partition2 the second element
     * @return whether the two partitions have been merged
     */
    public static boolean areMerged(Partition<?> partition1, Partition<?> partition2) {
        return partition1.find() == partition2.find();
    }
}