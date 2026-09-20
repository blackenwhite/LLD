package org.stripe.screen;

import java.util.*;

/**
 * Stripe KYC CSV Verification
 *
 * Parts:
 * 1. Basic field validation
 * 2. Statement descriptor validation
 * 3. Forbidden business-name words
 * 4. Business-name word matching
 * 5. Specific error codes
 */
public class KycVerification {

    // ================= PART 2 =================

    private static final Set<String> BLOCKED_DESCRIPTORS = new HashSet<>(
            Arrays.asList(
                    "test",
                    "testing",
                    "unknown",
                    "company",
                    "business",
                    "administrator",
                    "admin"
            )
    );

    // ================= PART 3 =================

    private static final Set<String> FORBIDDEN_NAME_WORDS = new HashSet<>(
            Arrays.asList(
                    "company",
                    "firm",
                    "co.",
                    "corporation",
                    "group"
            )
    );

    private static final Set<String> IGNORED_MATCH_WORDS = new HashSet<>(
            Arrays.asList(
                    "llc",
                    "inc"
            )
    );

    // ============================================================
    // PART 2
    // ============================================================

    private static boolean validateStatementDescriptor(String s) {

        if (s.length() < 5 || s.length() > 31) {
            return false;
        }

        return !BLOCKED_DESCRIPTORS.contains(s.toLowerCase());
    }

    // ============================================================
    // PART 3
    // ============================================================

    private static boolean validBusinessName(String s) {

        String[] parts = s.toLowerCase().split("\\s+");

        for (String part : parts) {
            if (FORBIDDEN_NAME_WORDS.contains(part)) {
                return false;
            }
        }

        return true;
    }

    // ============================================================
    // PART 4
    //
    // At least 50% of business_name words must occur in either
    // statement_descriptor OR registration_number.
    //
    // "llc" and "inc" are ignored.
    // ============================================================

    private static boolean validNameMatch(
            String businessName,
            String statementDescriptor,
            String registrationNumber) {

        String[] nameWords = businessName.toLowerCase().split("\\s+");

        Set<String> targetWords = new HashSet<>();

        for (String word : statementDescriptor.toLowerCase().split("\\s+")) {
            targetWords.add(word);
        }

        for (String word : registrationNumber.toLowerCase().split("\\s+")) {
            targetWords.add(word);
        }

        int totalWords = 0;
        int matchingWords = 0;

        for (String word : nameWords) {

            if (IGNORED_MATCH_WORDS.contains(word)) {
                continue;
            }

            totalWords++;

            if (targetWords.contains(word)) {
                matchingWords++;
            }
        }

        // If all business-name words were ignored.
        if (totalWords == 0) {
            return true;
        }

        return matchingWords * 2 >= totalWords;
    }

    // ============================================================
    // PART 5
    // ============================================================

    private static String getValidationError(String[] fields) {

        // 1. Missing field
        for (String field : fields) {
            if (field.isEmpty()) {
                return "MISSING_FIELD";
            }
        }

        // 2. Invalid descriptor length
        String descriptor = fields[4];

        if (descriptor.length() < 5 || descriptor.length() > 31) {
            return "INVALID_DESCRIPTOR_LENGTH";
        }

        // 3. Blocked descriptor
        if (BLOCKED_DESCRIPTORS.contains(descriptor.toLowerCase())) {
            return "BLOCKED_DESCRIPTOR";
        }

        // 4. Forbidden business name
        String businessName = fields[1];

        if (!validBusinessName(businessName)) {
            return "FORBIDDEN_BUSINESS_NAME";
        }

        // 5. Insufficient name match
        if (!validNameMatch(
                businessName,
                fields[4],
                fields[3])) {

            return "INSUFFICIENT_NAME_MATCH";
        }

        return "VERIFIED";
    }

    // ============================================================
    // MAIN VALIDATION
    // ============================================================

    public static List<String> validate(String csvData) {

        List<String> result = new ArrayList<>();

        String[] lines = csvData.split("\\r?\\n");

        // Skip header
        for (int i = 1; i < lines.length; i++) {

            String[] fields = lines[i].split(",", -1);

            // Business requirement says exactly six columns.
            // Defensive handling in case malformed input appears.
            if (fields.length < 6) {
                continue;
            }

            // Trim every field
            for (int j = 0; j < fields.length; j++) {
                fields[j] = fields[j].trim();
            }

            // Business name empty => completely exclude row
            if (fields[1].isEmpty()) {
                continue;
            }

            String error = getValidationError(fields);

            result.add(fields[1] + ": " + error);
        }

        return result;
    }

    // ============================================================
    // TEST HARNESS
    // ============================================================

    public static void main(String[] args) {

        String csv =
                "business_id,business_name,country,registration_number,statement_descriptor,contact_email\n" +

                // VERIFIED
                "1001,Acme Payments,US,REG12345,Acme Payments,contact@acme.com\n" +

                // BLOCKED_DESCRIPTOR
                "1002,Blue Ocean,GB,GB99881,test,hello@blueocean.com\n" +

                // FORBIDDEN_BUSINESS_NAME
                "1003,Ocean Group,US,REG55555,Ocean Group,hello@ocean.com\n" +

                // INVALID_DESCRIPTOR_LENGTH
                "1004,Small Shop,US,REG77777,ABC,a@shop.com\n" +

                // MISSING_FIELD
                "1005,Acme Tools,US,,Acme Tools,tools@acme.com\n" +

                // INSUFFICIENT_NAME_MATCH
                "1006,Acme Services,US,REG88888,Acme Online,service@acme.com\n" +

                // Excluded
                "1007,,US,REG99999,Valid Descriptor,empty@example.com";

        List<String> actual = validate(csv);

        for (String line : actual) {
            System.out.println(line);
        }
    }
}
