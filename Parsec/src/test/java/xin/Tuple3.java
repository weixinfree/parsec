package xin;

import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class Tuple3<F, S, T> {
    final F first;
    final S second;
    final T third;

    public Tuple3(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple3<?, ?, ?> tuple3 = (Tuple3<?, ?, ?>) o;
        return Objects.equals(first, tuple3.first) &&
                Objects.equals(second, tuple3.second) &&
                Objects.equals(third, tuple3.third);
    }

    @Override
    public int hashCode() {

        return Objects.hash(first, second, third);
    }

    @Override
    public String toString() {
        return "Tuple3{" +
                "first=" + first +
                ", second=" + second +
                ", third=" + third +
                '}';
    }
}
