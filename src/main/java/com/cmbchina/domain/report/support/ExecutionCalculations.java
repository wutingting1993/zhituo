package com.cmbchina.domain.report.support;
import java.math.BigDecimal;
import java.math.RoundingMode;
/** 比率比较先交叉相乘，展示时才舍入。 */
public final class ExecutionCalculations {
    private ExecutionCalculations() {
    }

    /** 百分比，无分母时不可计算。 */
    public static BigDecimal rate(long count, long total) {
        return total == 0 ? null : quotient(BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100)), BigDecimal.valueOf(total), 2);
    }

    /** 按指定精度返回商。 */
    public static BigDecimal divide(long count, long total, int scale) {
        return total == 0 ? null : quotient(BigDecimal.valueOf(count), BigDecimal.valueOf(total), scale);
    }

    private static BigDecimal quotient(BigDecimal numerator, BigDecimal denominator, int scale) {
        return numerator.divide(denominator, scale, RoundingMode.HALF_UP);
    }

    /** 无法计算的比率排最后，其余精确比较。 */
    public static int compare(long left, long leftTotal, long right, long rightTotal) {
        if (leftTotal == 0 || rightTotal == 0) {
            return Integer.compare(leftTotal == 0 ? 1 : 0, rightTotal == 0 ? 1 : 0);
        }
        return BigDecimal.valueOf(left).multiply(BigDecimal.valueOf(rightTotal))
                .compareTo(BigDecimal.valueOf(right).multiply(BigDecimal.valueOf(leftTotal)));
    }

    /** 两项百分比之差，仅在最终展示时舍入。 */
    public static BigDecimal difference(long count, long total, long baseline, long baselineTotal) {
        if (total == 0 || baselineTotal == 0) {
            return null;
        }
        return quotient(delta(count, total, baseline, baselineTotal), BigDecimal.valueOf(total).multiply(BigDecimal.valueOf(baselineTotal)), 2);
    }

    /** 按未舍入差值判断正负三个百分点评价。 */
    public static String status(long count, long total, long baseline, long baselineTotal) {
        if (total == 0 || baselineTotal == 0) {
            return null;
        }
        BigDecimal numerator = delta(count, total, baseline, baselineTotal);
        BigDecimal threshold = BigDecimal.valueOf(total).multiply(BigDecimal.valueOf(baselineTotal)).multiply(BigDecimal.valueOf(3));
        if (numerator.compareTo(threshold) >= 0) {
            return "high";
        }
        return numerator.compareTo(threshold.negate()) <= 0 ? "low" : "normal";
    }

    private static BigDecimal delta(long count, long total, long baseline, long baselineTotal) {
        return BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(baselineTotal))
                .subtract(BigDecimal.valueOf(baseline).multiply(BigDecimal.valueOf(total))).multiply(BigDecimal.valueOf(100));
    }
}
