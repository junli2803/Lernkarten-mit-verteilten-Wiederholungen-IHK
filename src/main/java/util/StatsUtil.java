package util;

import java.util.Arrays;

public final class StatsUtil {
    private StatsUtil() {}

    public static String prettyDuration(int ms) {
        int totalSec = ms / 1000;
        int m = totalSec / 60;
        int s = totalSec % 60;
        return String.format("%d:%02d", m, s);
    }

    /** 简单移动平均 */
    public static double[] movingAverage(double[] arr, int window) {
        if (arr == null || arr.length == 0 || window <= 1) return arr == null ? new double[0] : Arrays.copyOf(arr, arr.length);
        double[] out = new double[arr.length];
        double sum = 0;
        for (int i = 0; i < arr.length; i++) {
            sum += arr[i];
            if (i >= window) sum -= arr[i - window];
            out[i] = (i + 1 >= window) ? sum / window : sum / (i + 1);
        }
        return out;
    }
}
