/**
 * Created by lrebscher on 28.01.17.
 */
public class CoOccurrence<T> {

    private final T leftToken;

    private final T rightToken;

    public float score;

    CoOccurrence(final T leftToken, final T rightToken) {
        this.leftToken = leftToken;
        this.rightToken = rightToken;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (!CoOccurrence.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final CoOccurrence other = (CoOccurrence) obj;
        return other.leftToken.equals(leftToken) && other.rightToken.equals(rightToken);
    }

    public T getLeftToken() {
        return leftToken;
    }

    public T getRightToken() {
        return rightToken;
    }

    @Override
    public int hashCode() {
        return leftToken.hashCode() + rightToken.hashCode();
    }
}
