/**
 * Created by lrebscher on 13.11.16.
 */
public final class Utils {

    private Utils() {
    }

    public static Long parseNumber(final String number) {
        return Long.valueOf(number.replaceAll("[^\\d.]+", ""));
    }
}
