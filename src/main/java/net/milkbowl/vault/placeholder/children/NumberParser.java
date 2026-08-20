package net.milkbowl.vault.placeholder.children;

final class NumberParser {

    private NumberParser() {
    }

    static int parseUnsignedInt(String value, int start, int end) {
        if (start >= end) return -1;

        int result = 0;
        final int limit = -Integer.MAX_VALUE;
        final int multMin = limit / 10;

        for (int i = start; i < end; i++) {
            final int digit = value.charAt(i) - '0';
            if (digit < 0 || digit > 9) return -1;
            if (result < multMin) return -1;

            result *= 10;
            if (result < limit + digit) return -1;

            result -= digit;
        }

        return -result;
    }
}
