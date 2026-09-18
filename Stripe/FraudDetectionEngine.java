package org.stripe.screen;

import java.util.*;

public class FraudDetectionEngine {

    static class Rule{
        String action;
        String attribute;
        String value;
        String operator;
        List<String> groupList;

        Rule(String action, String attribute, String value, String operator) {
            this.action = action;
            this.attribute = attribute;
            this.value = value;
            this.operator = operator;
            this.groupList = new ArrayList<>();
        }

        Rule() {
            this.groupList = new ArrayList<>();
        }
    }

    // Common rule parser // Supports: // DECLINE:merchant_name=CryptoEscrow // DECLINE:merchant_total_spend>5000
    private static Rule parseRule(String rule) {
        String[] parts = rule.split(":", 2);

        String action = parts[0];
        String condition = parts[1];

        if(action.equals("DEFINE_GROUP")) {
            Rule r = new Rule();
            String[] conditionParts = condition.split("=", 2);
            r.attribute= conditionParts[0];
            r.action = action;
            String[] merchants = conditionParts[1].split(",");
            r.groupList.addAll(Arrays.asList(merchants));
            return r;
        }

        if(condition.contains(">")) {
            String[] conditionParts = condition.split(">", 2);

            return new Rule(action, conditionParts[0], conditionParts[1], ">");

        } else {
            String[] conditionParts = condition.split("=", 2);
            return new Rule(action, conditionParts[0], conditionParts[1], "=");
        }
    }

    public static List<String> evaluateTransactionsPart1(
            List<Map<String, String>> transactions,
            List<String> rules) {

        List<String> results = new ArrayList<>();

        for (Map<String, String> transaction : transactions) {

            String decision = "ALLOW";

            for (String rule : rules) {

                Rule parsedRule = parseRule(rule);

                String actualValue = transaction.get(parsedRule.attribute);

                if(parsedRule.value.equals(actualValue)) {
                    decision = parsedRule.action;
                    break;
                }
            }

            results.add(decision);
        }

        return results;
    }

    public static List<String> evaluateTransactionsPart2(List<Map<String,String>> transactions, List<String> rules) {
        List<String> results = new ArrayList<>();

        Map<String, Long> runningTotal = new HashMap<>();

        for(Map<String,String> transaction : transactions) {
            String decision = "ALLOW";
            String merchantName = transaction.get("merchant_name");

            long transactionAmount = Long.parseLong(transaction.get("amount"));

            long priorTotal = runningTotal.getOrDefault(merchantName, 0L);

            for(String rule:rules) {
                Rule parsedRule = parseRule(rule);
                boolean matches;

                if(parsedRule.attribute.equals("merchant_total_spend")) {
                    long threshold = Long.parseLong(parsedRule.value);
                    matches = priorTotal > threshold;
                } else {
                    String actualValue = transaction.get(parsedRule.attribute);
                    matches = parsedRule.value.equals(actualValue);
                }

                if(matches) {
                    decision = parsedRule.action;
                    break;
                }
            }
            results.add(decision);
            if(decision.equals("ALLOW")) {
                runningTotal.put(merchantName, priorTotal + transactionAmount);
            }
        }
        return results;
    }

    public static List<String> evaluateTransactionsPart3(List<Map<String,String>> transactions, List<String> rules) {
        // pass 1 -> get all the rule definitions and store them
        Map<String, List<String>> groups = new HashMap<>(); // key = "HighRiskMerchants" , value = ["CryptoEscrow", "CasinoX"]
        List<String> results = new ArrayList<>();
        for(String rule : rules) {
            Rule parsedRule = parseRule(rule);
            if(parsedRule.action.equals("DEFINE_GROUP")) {
                groups.put(parsedRule.attribute, parsedRule.groupList);
            }
        }

        for(Map<String,String> transaction : transactions) {
            String decision = "ALLOW";
            String merchantName = transaction.get("merchant_name");
            for(String rule:rules) {
                Rule parsedRule = parseRule(rule);
                if(parsedRule.action.equals("DEFINE_GROUP")) {
                    continue;
                }
                boolean matches = false;

                if(parsedRule.attribute.equals("merchant_nameINGroup")) {
                    String groupName = parsedRule.value;
                    List<String> groupList = groups.getOrDefault(groupName, Collections.emptyList());
                    for(String name: groupList) {
                        if(name.equals(merchantName)) {
                            matches = true;
                            break;
                        }
                    }
                }

                if(matches) {
                    decision = parsedRule.action;
                    break;
                }
            }
            results.add(decision);

        }
        return results;

    }

    static class Event {
        String type;
        Map<String, String> data;

        Event(String type, Map<String,String> data) {
            this.type = type;
            this.data = data;
        }
    }

    public static List<String> evaluateEventsPart4(List<Event> events, List<String> rules) {
        List<String> results = new ArrayList<> ();
        Map<String, List<String>> groups = new HashMap<>();

        Map<String, Long> runningTotal = new HashMap<>();

        // txn id -> txn
        Map<String, Map<String,String>> transactionsById = new HashMap<>();

        // txn id -> current decision
        Map<String, String> decisionsById = new HashMap<>();

        for(String rule: rules) {
            Rule parsedRule = parseRule(rule);

            if(parsedRule.action.equals("DEFINE_GROUP")) {
                groups.put(parsedRule.attribute, parsedRule.groupList);
            }
        }

        for(Event e: events) {
            // transaction
            if(e.type.equals("TRANSACTION")) {
                Map<String, String> transaction  = e.data;
                String txId = transaction.get("id");
                String merchantName = transaction.get("merchant_name");
                long amount = Long.parseLong(transaction.get("amount"));

                long priorTotal = runningTotal.getOrDefault(merchantName, 0L);

                String decision = "ALLOW";

                for(String rule: rules) {
                    Rule parsedRule = parseRule(rule);
                    if(parsedRule.action.equals("DEFINE_GROUP")) {
                        continue;
                    }

                    boolean matches = false;

                    if(parsedRule.attribute.equals("merchant_total_spend")) {
                        long threshold = Long.parseLong(parsedRule.value);
                        matches = priorTotal > threshold;
                    } else if(parsedRule.attribute.equals("merchant_nameInGroup")) {
                        String groupName = parsedRule.value;
                        List<String> groupList = groups.getOrDefault(groupName, Collections.emptyList());
                        matches = groupList.contains(merchantName);
                    } else {
                        String actualValue = transaction.get(parsedRule.attribute);
                        matches = parsedRule.value.equals(actualValue);
                    }

                    if(matches) {
                        decision = parsedRule.action;
                        break;
                    }
                }

                // store txn and decision
                transactionsById.put(txId, transaction);
                decisionsById.put(txId, decision);

                results.add(decision);

                if(decision.equals("ALLOW")) {
                    runningTotal.put(merchantName, priorTotal+amount);
                }
            }
            // Exemption GRANTEd
            else if (e.type.equals("EXEMPTION_GRANTED")) {
                String txnId = e.data.get("target_txn_id");
                String currentDecision = decisionsById.get(txnId);

                if("DECLINE".equals(currentDecision)) {
                    Map<String,String> transaction = transactionsById.get(txnId);

                    String merchantName = transaction.get("merchant_name");
                    long amount = Long.parseLong(transaction.get("amount"));

                    decisionsById.put(txnId, "ALLOW");

                    long currentTotal = runningTotal.getOrDefault(merchantName, 0L);
                    runningTotal.put(merchantName, currentTotal+amount);
                }
            }
        }

        return results;

    }

    // ====== QUICK LOCAL TEST RUNNER ======
    public static void main(String[] args) {
        System.out.println("--- Running Part 1 Test ---");
        List<Map<String, String>> txns1 = List.of(
                Map.of("id", "t1", "merchant_name", "Nike", "card_brand", "Visa"),
                Map.of("id", "t2", "merchant_name", "CryptoEscrow", "card_brand", "Mastercard")
        );
        List<String> rules1 = List.of(
                "DECLINE:merchant_name=CryptoEscrow",
                "ALLOW:card_brand=Visa"
        );
        System.out.println("Result P1: " + evaluateTransactionsPart1(txns1, rules1));
        // Expected: [ALLOW, DECLINE]

        List<Map<String, String>> txns = List.of(
                Map.of("id", "t1", "merchant_name", "Nike", "amount", "3000"),
                Map.of("id", "t2", "merchant_name", "Nike", "amount", "2500"),
                Map.of("id", "t3", "merchant_name", "Nike", "amount", "1000")
        );
        List<String> rules = List.of(
                "DECLINE:merchant_total_spend>5000"
        );

        System.out.println("Result P2: " + evaluateTransactionsPart2(txns, rules));

        List<Map<String, String>> txns3 = List.of(
                Map.of("id", "t1", "merchant_name", "CasinoX"),
                Map.of("id", "t2", "merchant_name", "Target")
        );
        List<String> rules3= List.of(
                "DECLINE:merchant_nameINGroup=HighRiskMerchants",
                "DEFINE_GROUP:HighRiskMerchants=CryptoEscrow,CasinoX"
        );

        System.out.println("Result P3: " + evaluateTransactionsPart3(txns3, rules3));


    }


}


