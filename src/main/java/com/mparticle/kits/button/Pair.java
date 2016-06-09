package com.mparticle.kits.button;

public class Pair<First, Second> {
    private final First mFirst;
    private final Second mSecond;

    public Pair(First first, Second second) {
        mFirst = first;
        mSecond = second;
    }

    public First first() {
        return mFirst;
    }

    public Second second() {
        return mSecond;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Pair<?, ?> pair = (Pair<?, ?>) o;

        if (mFirst != null ? !mFirst.equals(pair.mFirst) : pair.mFirst != null) return false;
        return !(mSecond != null ? !mSecond.equals(pair.mSecond) : pair.mSecond != null);

    }

    @Override
    public int hashCode() {
        int result = mFirst != null ? mFirst.hashCode() : 0;
        result = 31 * result + (mSecond != null ? mSecond.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("Pair{mFirst=%s, mSecond=%s}", mFirst, mSecond);
    }
}
