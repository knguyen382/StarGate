@Override
public CharSequence filter(CharSequence charSequence, int i, int i1,
                           Spanned spanned, int i2, int i3) {

    CharSequence retval = null;

    // INSERT ONLY
    if (i2 == i3) {

        String original = charSequence.subSequence(i, i1).toString();

        // 1) Strip invalid characters using regex
        String cleaned = original.replaceAll("[^0-9a-fA-F:.]", "");
        boolean changed = !original.equals(cleaned);

        // 2) Build candidate string
        String candidate =
                spanned.subSequence(0, i2).toString()
                + cleaned
                + spanned.subSequence(i3, spanned.length()).toString();

        boolean ok = true;

        // Typed only invalid chars → block
        if (cleaned.isEmpty() && !original.isEmpty()) {
            ok = false;
        }

        // Always allow clearing
        if (ok && candidate.isEmpty()) {
            retval = changed ? "" : null;
        } else if (ok) {

            // 3) Auto-detect IPv4 vs IPv6
            boolean hasColon = candidate.indexOf(':') >= 0;
            boolean hasDot   = candidate.indexOf('.') >= 0;

            if (hasColon && hasDot) {
                ok = false;
            } else if (hasColon) {
                ok = isValidPartialIPv6(candidate);
            } else if (hasDot) {
                ok = isValidPartialIPv4(candidate);
            } else {
                // ambiguous early typing
                ok = isValidPartialIPv4(candidate) || isValidPartialIPv6(candidate);
            }

            if (!ok) {
                retval = "";
            } else if (changed) {
                retval = cleaned;
            } else {
                retval = null;
            }
        }

        if (!ok && retval == null) {
            retval = "";
        }
    }

    // delete / replace → retval == null → allowed
    return retval;
}
