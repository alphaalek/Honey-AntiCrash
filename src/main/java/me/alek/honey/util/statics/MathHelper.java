package me.alek.honey.util.statics;

import java.util.Random;
import java.util.UUID;

public class MathHelper {
    public static final float a = c(2.0F);

    public static float c(float paramFloat) {
        return (float)Math.sqrt(paramFloat);
    }

    public static float sqrt(double paramDouble) {
        return (float)Math.sqrt(paramDouble);
    }

    public static int d(float paramFloat) {
        int i = (int)paramFloat;
        return (paramFloat < i) ? (i - 1) : i;
    }

    public static int floor(double paramDouble) {
        int i = (int)paramDouble;
        return (paramDouble < i) ? (i - 1) : i;
    }

    public static long d(double paramDouble) {
        long l = (long)paramDouble;
        return (paramDouble < l) ? (l - 1L) : l;
    }

    public static float e(float paramFloat) {
        return (paramFloat >= 0.0F) ? paramFloat : -paramFloat;
    }

    public static int a(int paramInt) {
        return (paramInt >= 0) ? paramInt : -paramInt;
    }

    public static int f(float paramFloat) {
        int i = (int)paramFloat;
        return (paramFloat > i) ? (i + 1) : i;
    }

    public static int f(double paramDouble) {
        int i = (int)paramDouble;
        return (paramDouble > i) ? (i + 1) : i;
    }

    public static int clamp(int paramInt1, int paramInt2, int paramInt3) {
        if (paramInt1 < paramInt2)
            return paramInt2;
        return Math.min(paramInt1, paramInt3);
    }

    public static float a(float paramFloat1, float paramFloat2, float paramFloat3) {
        if (paramFloat1 < paramFloat2)
            return paramFloat2;
        return Math.min(paramFloat1, paramFloat3);
    }

    public static double a(double paramDouble1, double paramDouble2, double paramDouble3) {
        if (paramDouble1 < paramDouble2)
            return paramDouble2;
        return Math.min(paramDouble1, paramDouble3);
    }

    public static double b(double paramDouble1, double paramDouble2, double paramDouble3) {
        if (paramDouble3 < 0.0D)
            return paramDouble1;
        if (paramDouble3 > 1.0D)
            return paramDouble2;
        return paramDouble1 + (paramDouble2 - paramDouble1) * paramDouble3;
    }

    public static double a(double paramDouble1, double paramDouble2) {
        if (paramDouble1 < 0.0D)
            paramDouble1 = -paramDouble1;
        if (paramDouble2 < 0.0D)
            paramDouble2 = -paramDouble2;
        return Math.max(paramDouble1, paramDouble2);
    }

    public static int nextInt(Random paramRandom, int paramInt1, int paramInt2) {
        if (paramInt1 >= paramInt2)
            return paramInt1;
        return paramRandom.nextInt(paramInt2 - paramInt1 + 1) + paramInt1;
    }

    public static float a(Random paramRandom, float paramFloat1, float paramFloat2) {
        if (paramFloat1 >= paramFloat2)
            return paramFloat1;
        return paramRandom.nextFloat() * (paramFloat2 - paramFloat1) + paramFloat1;
    }

    public static double a(Random paramRandom, double paramDouble1, double paramDouble2) {
        if (paramDouble1 >= paramDouble2)
            return paramDouble1;
        return paramRandom.nextDouble() * (paramDouble2 - paramDouble1) + paramDouble1;
    }

    public static double a(long[] paramArrayOflong) {
        long l = 0L;
        for (long l1 : paramArrayOflong)
            l += l1;
        return (double) l / paramArrayOflong.length;
    }

    public static float g(float paramFloat) {
        paramFloat %= 360.0F;
        if (paramFloat >= 180.0F)
            paramFloat -= 360.0F;
        if (paramFloat < -180.0F)
            paramFloat += 360.0F;
        return paramFloat;
    }

    public static double g(double paramDouble) {
        paramDouble %= 360.0D;
        if (paramDouble >= 180.0D)
            paramDouble -= 360.0D;
        if (paramDouble < -180.0D)
            paramDouble += 360.0D;
        return paramDouble;
    }

    public static int a(String paramString, int paramInt) {
        try {
            return Integer.parseInt(paramString);
        } catch (Throwable throwable) {
            return paramInt;
        }
    }

    public static int a(String paramString, int paramInt1, int paramInt2) {
        return Math.max(paramInt2, a(paramString, paramInt1));
    }

    public static double a(String paramString, double paramDouble) {
        try {
            return Double.parseDouble(paramString);
        } catch (Throwable throwable) {
            return paramDouble;
        }
    }

    public static double a(String paramString, double paramDouble1, double paramDouble2) {
        return Math.max(paramDouble2, a(paramString, paramDouble1));
    }

    public static int b(int paramInt) {
        int i = paramInt - 1;
        i |= i >> 1;
        i |= i >> 2;
        i |= i >> 4;
        i |= i >> 8;
        i |= i >> 16;
        return i + 1;
    }

    private static boolean d(int paramInt) {
        return (paramInt != 0 && (paramInt & paramInt - 1) == 0);
    }

    private static final int[] c = new int[] {
            0, 1, 28, 2, 29, 14, 24, 3, 30, 22,
            20, 15, 25, 17, 4, 8, 31, 27, 13, 23,
            21, 19, 16, 7, 26, 12, 18, 6, 11, 5,
            10, 9 };

    private static int e(int paramInt) {
        paramInt = d(paramInt) ? paramInt : b(paramInt);
        return c[(int)(paramInt * 125613361L >> 27L) & 0x1F];
    }

    public static int c(int paramInt) {
        return e(paramInt) - (d(paramInt) ? 0 : 1);
    }

    public static int c(int paramInt1, int paramInt2) {
        if (paramInt2 == 0)
            return 0;
        if (paramInt1 == 0)
            return paramInt2;
        if (paramInt1 < 0)
            paramInt2 *= -1;
        int i = paramInt1 % paramInt2;
        if (i == 0)
            return paramInt1;
        return paramInt1 + paramInt2 - i;
    }

    public static UUID a(Random paramRandom) {
        long l1 = paramRandom.nextLong() & 0xFFFFFFFFFFFF0FFFL | 0x4000L;
        long l2 = paramRandom.nextLong() & 0x3FFFFFFFFFFFFFFFL | Long.MIN_VALUE;
        return new UUID(l1, l2);
    }

    public static double c(double paramDouble1, double paramDouble2, double paramDouble3) {
        return (paramDouble1 - paramDouble2) / (paramDouble3 - paramDouble2);
    }

    public static double b(double paramDouble1, double paramDouble2) {
        double d1 = paramDouble2 * paramDouble2 + paramDouble1 * paramDouble1;
        if (Double.isNaN(d1))
            return Double.NaN;
        boolean bool1 = paramDouble1 < 0.0D;
        if (bool1)
            paramDouble1 = -paramDouble1;
        boolean bool2 = paramDouble2 < 0.0D;
        if (bool2)
            paramDouble2 = -paramDouble2;
        boolean bool3 = paramDouble1 > paramDouble2;
        if (bool3) {
            double d = paramDouble2;
            paramDouble2 = paramDouble1;
            paramDouble1 = d;
        }
        double d2 = i(d1);
        paramDouble2 *= d2;
        paramDouble1 *= d2;
        double d3 = d + paramDouble1;
        int i = (int)Double.doubleToRawLongBits(d3);
        double d4 = e[i];
        double d5 = f[i];
        double d6 = d3 - d;
        double d7 = paramDouble1 * d5 - paramDouble2 * d6;
        double d8 = (6.0D + d7 * d7) * d7 * 0.16666666666666666D;
        double d9 = d4 + d8;
        if (bool3)
            d9 = 1.5707963267948966D - d9;
        if (bool2)
            d9 = Math.PI - d9;
        if (bool1)
            d9 = -d9;
        return d9;
    }

    public static double i(double paramDouble) {
        double d = 0.5D * paramDouble;
        long l = Double.doubleToRawLongBits(paramDouble);
        l = 6910469410427058090L - (l >> 1L);
        paramDouble = Double.longBitsToDouble(l);
        paramDouble *= 1.5D - d * paramDouble * paramDouble;
        return paramDouble;
    }

    private static final double d = Double.longBitsToDouble(4805340802404319232L);

    private static final double[] e = new double[257];

    private static final double[] f = new double[257];
}


