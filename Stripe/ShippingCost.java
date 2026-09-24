package org.stripe.screen.shipping;

import java.util.*;

public class Main {

    // =========================================================================
    // 1. YOUR SOLUTION LOGIC
    // =========================================================================
    enum CalculationType {
        INCREMENTAL,
        FIXED
    }

    record CostingTier(CalculationType calculationType, Integer minQuantity, Integer maxQuantity, long cost) {}

    public static long calculateShippingCost(Map<String, Object> order, Map<String, Object> shippingCosts) {
        String country = (String) order.get("country");
        List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");

        Map<String, List<CostingTier>> productTiers = parseShippingCosts(shippingCosts, country);

        long totalCost = 0;
        for (Map<String, Object> item : items) {
            String product =  (String) item.get("product");
            int quantity = (Integer) item.get("quantity");

            List<CostingTier> tiers = productTiers.get(product);
            if(tiers!=null) {
                totalCost += calculateItemCost(quantity, tiers);
            }
        }

        return totalCost;
    }

    private static long calculateItemCost(int quantity, List<CostingTier> tiers) {
        long totalCost = 0;

        for(CostingTier tier : tiers) {
            // for each tier we will calculate how many items
            // falls in that tier
            int start, end;
            if(tier.minQuantity == null) {
                start = 0;
            } else {
                start = Math.max(0,tier.minQuantity-1);
            }

            if(tier.maxQuantity == null) {
                end = Integer.MAX_VALUE;
            }else{
                end = tier.maxQuantity;
            }

            end = Math.min(end, quantity) - 1;

            if(quantity >= start) {
                if(tier.calculationType.equals(CalculationType.FIXED)) {
                    totalCost += tier.cost;
                } else {
                    int num = end - start + 1;
                    totalCost+= num*tier.cost;
                }

            } else {
                continue;
            }
        }

        return totalCost;
    }

    private static Map<String, List<CostingTier>> parseShippingCosts(Map<String, Object> shippingCosts, String country) {
        Map<String, List<CostingTier>> productTiers = new HashMap<>();
        List<Map<String, Object>> countryCosts = (List<Map<String, Object>>) shippingCosts.get(country);

        for(Map<String, Object> entry: countryCosts) {
            String productname = (String) entry.get("product");
            List<CostingTier> costingTiers = new ArrayList<>();

            if(entry.containsKey("cost")) {
                long cost = ((Number) entry.get("cost")).longValue();
                costingTiers.add(new CostingTier(CalculationType.INCREMENTAL, null, null, cost));
            } else if (entry.containsKey("costs")) {
                List<Map<String, Object>> costs = (List<Map<String, Object>>) entry.get("costs");
                for(Map<String, Object> cost: costs) {
                    CalculationType calculationType = CalculationType.INCREMENTAL;
                    if(cost.containsKey("type")) {
                        String value = (String) cost.get("type");
                        if(value.equals("fixed")) {
                            calculationType = CalculationType.FIXED;
                        }
                    }

                    int minQuantity = ((Number) cost.get("minQuantity")).intValue();
                    Integer maxQuantity = null;
                    if(cost.containsKey("maxQuantity")) {
                        Integer value = (cost.get("maxQuantity")!=null)? ((Number) cost.get("maxQuantity")).intValue() : null;
                        if(value!=null) {
                            maxQuantity = value;
                        }
                    }

                    long cost1 = ((Number) cost.get("cost")).longValue();

                    costingTiers.add(new CostingTier(calculationType, minQuantity, maxQuantity, cost1));
                }

            }
            productTiers.put(productname, costingTiers);
        }
        return productTiers;
    }


    // =========================================================================
    // 2. HARDCODED INPUT DATA & SUITE RUNNER
    // =========================================================================

    public static void main(String[] args) {
        // --- ORDERS ---
        Map<String, Object> orderUS = Map.of(
                "country", "US",
                "items", List.of(
                        Map.of("product", "mouse", "quantity", 20),
                        Map.of("product", "laptop", "quantity", 5)
                )
        );

        Map<String, Object> orderCA = Map.of(
                "country", "CA",
                "items", List.of(
                        Map.of("product", "mouse", "quantity", 20),
                        Map.of("product", "laptop", "quantity", 5)
                )
        );

        // --- PART 1 RAW INPUT ---
        Map<String, Object> shippingCostPart1 = Map.of(
                "US", List.of(
                        Map.of("product", "mouse", "cost", 550),
                        Map.of("product", "laptop", "cost", 1000)
                ),
                "CA", List.of(
                        Map.of("product", "mouse", "cost", 750),
                        Map.of("product", "laptop", "cost", 1100)
                )
        );

        // --- PART 2 RAW INPUT ---
        Map<String, Object> shippingCostPart2 = Map.of(
                "US", List.of(
                        Map.of("product", "mouse", "costs", List.of(
                                tier(null, 0, null, 550)
                        )),
                        Map.of("product", "laptop", "costs", List.of(
                                tier(null, 0, 2, 1000),
                                tier(null, 3, null, 900)
                        ))
                ),
                "CA", List.of(
                        Map.of("product", "mouse", "costs", List.of(
                                tier(null, 0, null, 750)
                        )),
                        Map.of("product", "laptop", "costs", List.of(
                                tier(null, 0, 2, 1100),
                                tier(null, 3, null, 1000)
                        ))
                )
        );

        // --- PART 3 RAW INPUT ---
        Map<String, Object> shippingCostPart3 = Map.of(
                "US", List.of(
                        Map.of("product", "mouse", "costs", List.of(
                                tier("incremental", 0, null, 550)
                        )),
                        Map.of("product", "laptop", "costs", List.of(
                                tier("fixed", 0, 2, 1000),
                                tier("incremental", 3, null, 900)
                        ))
                ),
                "CA", List.of(
                        Map.of("product", "mouse", "costs", List.of(
                                tier("incremental", 0, null, 750)
                        )),
                        Map.of("product", "laptop", "costs", List.of(
                                tier("fixed", 0, 2, 1100),
                                tier("incremental", 3, null, 1000)
                        ))
                )
        );

        // --- RUNNING TESTS ---
        /*System.out.println("=== PART 1 TESTS ===");
        runTest("Part 1 - US Order", calculateShippingCost(orderUS, shippingCostPart1), 16000);
        runTest("Part 1 - CA Order", calculateShippingCost(orderCA, shippingCostPart1), 20500);*/

        System.out.println("\n=== PART 2 TESTS ===");
        runTest("Part 2 - US Order", calculateShippingCost(orderUS, shippingCostPart2), 15700);
        runTest("Part 2 - CA Order", calculateShippingCost(orderCA, shippingCostPart2), 20200);

        System.out.println("\n=== PART 3 TESTS ===");
        runTest("Part 3 - US Order", calculateShippingCost(orderUS, shippingCostPart3), 14700);
        runTest("Part 3 - CA Order", calculateShippingCost(orderCA, shippingCostPart3), 19100);
    }

    // Helper method to construct maps with null values safely (Java's Map.of throws NPE if a value is null)
    private static Map<String, Object> tier(String type, Integer minQty, Integer maxQty, int cost) {
        Map<String, Object> map = new HashMap<>();
        if (type != null) map.put("type", type);
        if (minQty != null) map.put("minQuantity", minQty);
        map.put("maxQuantity", maxQty); // Allows null value
        map.put("cost", cost);
        return map;
    }

    private static void runTest(String testName, long actual, long expected) {
        boolean passed = actual == expected;
        String result = passed ? "PASSED" : "FAILED (Expected " + expected + ", got " + actual + ")";
        System.out.printf("[%s] %s: %d\n", result, testName, actual);
    }
}

class CostingRule {
    String product;
    long cost;
}
