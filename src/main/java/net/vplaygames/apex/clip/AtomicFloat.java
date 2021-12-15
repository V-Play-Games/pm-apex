package net.vplaygames.apex.clip;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicFloat extends Number {
    protected final AtomicInteger base;

    /**
     * Creates a new AtomicFloat with the given initial value.
     *
     * @param initialValue the initial value
     */
    public AtomicFloat(float initialValue) {
        base = new AtomicInteger(i(initialValue));
    }

    /**
     * Creates a new AtomicFloat with initial value {@code 0}.
     */
    public AtomicFloat() {
        this(0);
    }

    private static int i(float f) {
        return Float.floatToIntBits(f);
    }

    private static float f(int i) {
        return Float.intBitsToFloat(i);
    }

    /**
     * Returns the current value,
     *
     * @return the current value
     */
    public float get() {
        return f(base.get());
    }

    /**
     * Sets the value to {@code update},
     *
     * @param update the new value
     */
    public void set(float update) {
        base.set(i(update));
    }

    /**
     * Sets the value to {@code update},
     *
     * @param update the new value
     */
    public void lazySet(float update) {
        base.lazySet(i(update));
    }

    /**
     * Atomically sets the value to {@code update} and returns the old value,
     *
     * @param update the new value
     * @return the previous value
     */
    public float getAndSet(float update) {
        return base.getAndSet(i(update));
    }

    /**
     * Atomically sets the value to {@code update}
     * if the current value {@code == expected},
     *
     * @param expected the expected value
     * @param update   the new value
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    public boolean compareAndSet(float expected, float update) {
        return base.compareAndSet(i(expected), i(update));
    }

    /**
     * Atomically increments the current value,
     *
     * <p>Equivalent to {@code getAndAdd(1)}.
     *
     * @return the previous value
     */
    public float getAndIncrement() {
        return getAndAdd(1);
    }

    /**
     * Atomically decrements the current value,
     *
     * <p>Equivalent to {@code getAndAdd(-1)}.
     *
     * @return the previous value
     */
    public float getAndDecrement() {
        return getAndAdd(-1);
    }

    /**
     * Atomically adds the given value to the current value,
     *
     * @param delta the value to add
     * @return the previous value
     */
    public float getAndAdd(float delta) {
        return base.getAndAdd(i(delta));
    }

    /**
     * Atomically increments the current value,
     *
     * <p>Equivalent to {@code addAndGet(1)}.
     *
     * @return the updated value
     */
    public float incrementAndGet() {
        return addAndGet(1);
    }

    /**
     * Atomically decrements the current value,
     *
     * <p>Equivalent to {@code addAndGet(-1)}.
     *
     * @return the updated value
     */
    public float decrementAndGet() {
        return addAndGet(-1);
    }

    /**
     * Atomically adds the given value to the current value,
     *
     * @param delta the value to add
     * @return the updated value
     */
    public float addAndGet(float delta) {
        return base.getAndAdd((i(delta))) + delta;
    }

    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    public String toString() {
        return Float.toString(get());
    }

    /**
     * Returns the current value of this {@code AtomicFloat} as an
     * {@code int},
     * <p>
     * Equivalent to {@link #get()}.
     */
    public int intValue() {
        return (int) get();
    }

    /**
     * Returns the current value of this {@code AtomicFloat} as a
     * {@code long} after a widening primitive conversion,
     */
    public long longValue() {
        return (long) get();
    }

    /**
     * Returns the current value of this {@code AtomicFloat} as a
     * {@code float} after a widening primitive conversion,
     */
    public float floatValue() {
        return get();
    }

    /**
     * Returns the current value of this {@code AtomicFloat} as a
     * {@code double} after a widening primitive conversion,
     */
    public double doubleValue() {
        return get();
    }
}
