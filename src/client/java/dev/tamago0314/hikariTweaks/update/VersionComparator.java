package dev.tamago0314.hikariTweaks.update;

/**
 * Simple semantic-like version comparator.
 * Supports examples like: v1.2.0, 1.2.0, 1.2.0-beta.1
 */
public final class VersionComparator {
    private VersionComparator() {}

    public static boolean isNewer(String candidate, String current) {
        return compare(candidate, current) > 0;
    }

    private static int compare(String a, String b) {
        ParsedVersion va = parse(a);
        ParsedVersion vb = parse(b);

        int max = Math.max(va.numbers.length, vb.numbers.length);
        for (int i = 0; i < max; i++) {
            int na = i < va.numbers.length ? va.numbers[i] : 0;
            int nb = i < vb.numbers.length ? vb.numbers[i] : 0;
            if (na != nb) {
                return Integer.compare(na, nb);
            }
        }

        if (va.preRelease == null && vb.preRelease == null) {
            return 0;
        }
        if (va.preRelease == null) {
            return 1;
        }
        if (vb.preRelease == null) {
            return -1;
        }
        return va.preRelease.compareToIgnoreCase(vb.preRelease);
    }

    private static ParsedVersion parse(String raw) {
        if (raw == null) {
            return new ParsedVersion(new int[]{0}, null);
        }

        String normalized = raw.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }

        String[] plusParts = normalized.split("\\+", 2);
        String baseAndPre = plusParts[0];
        String[] dashParts = baseAndPre.split("-", 2);
        String base = dashParts[0];
        String pre = dashParts.length > 1 ? dashParts[1] : null;

        String[] parts = base.split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            numbers[i] = parseIntSafe(parts[i]);
        }

        return new ParsedVersion(numbers, pre);
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private record ParsedVersion(int[] numbers, String preRelease) {}
}
