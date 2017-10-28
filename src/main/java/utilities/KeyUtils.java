package utilities;

import java.util.Random;

public class KeyUtils {

    private static String keySource = "abcdefghijklmnopqrstuvwxyz123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String makeKey64() {

        Random random = new Random();

        StringBuilder result = new StringBuilder();

        for (int counter = 0; counter < 64; counter++) {
            result.append(KeyUtils.keySource.charAt(random.nextInt(KeyUtils.keySource.length() - 1)));
        }

        return new String(result);
    }
}