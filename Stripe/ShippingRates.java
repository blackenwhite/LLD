package org.stripe.screen.shipping;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*
* Input:
order = {"items": [{"name": "Mouse", "unit_price": 550, "quantity": 1}, {"name": "Laptop", "unit_price": 1000, "quantity": 1}], "country": "US"}
shipping_rates = {"US": 16000, "Canada": 15000}

Output:
17550.0
*
* Total item price = (550 * 1) + (1000 * 1) = 1550.
Flat shipping for US = 16000.
Total price = 1550 + 16000 = 17550.0
*
*
* Input:
order = {"items": [{"name": "Laptop", "quantity": 3}, {"name": "Keyboard", "quantity": 15}]}
product_shipping_rates = {
  "Laptop": [
    {"min_quantity": 1, "max_quantity": 5, "type": "incremental", "unit_shipping_price": 100},
    {"min_quantity": 6, "max_quantity": 10, "type": "incremental", "unit_shipping_price": 80},
    {"min_quantity": 11, "max_quantity": null, "type": "incremental", "unit_shipping_price": 60}
  ],
  "Keyboard": [
    {"min_quantity": 1, "max_quantity": 10, "type": "incremental", "unit_shipping_price": 20},
    {"min_quantity": 11, "max_quantity": null, "type": "incremental", "unit_shipping_price": 15}
  ]
}

Output:
525.0

Explanation:
For Laptop (quantity 3): falls into [1, 5] bracket. Shipping = 3 * 100 = 300.
For Keyboard (quantity 15): falls into [11, null] bracket. Shipping = 15 * 15 = 225.
Total shipping = 300 + 225 = 525.0
*
* order = {"items": [{"name": "Laptop", "quantity": 3}, {"name": "Keyboard", "quantity": 15}]}
product_shipping_rates = {"Laptop": [{"min_quantity": 1, "max_quantity": 5, "type": "incremental", "unit_shipping_price": 100}, {"min_quantity": 6, "max_quantity": 10, "type": "incremental", "unit_shipping_price": 80}, {"min_quantity": 11, "max_quantity": null, "type": "incremental", "unit_shipping_price": 60}], "Keyboard": [{"min_quantity": 1, "max_quantity": 10, "type": "incremental", "unit_shipping_price": 20}, {"min_quantity": 11, "max_quantity": null, "type": "incremental", "unit_shipping_price": 15}]}
525
*
* ----Part 3 ----
*
*
* Input:
order = {"items": [{"name": "Desktop", "quantity": 2}, {"name": "Monitor", "quantity": 7}]}
product_shipping_rates = {
  "Desktop": [
    {"min_quantity": 1, "max_quantity": 5, "type": "fixed", "fixed_shipping_price": 1000},
    {"min_quantity": 6, "max_quantity": null, "type": "incremental", "unit_shipping_price": 150}
  ],
  "Monitor": [
    {"min_quantity": 1, "max_quantity": 3, "type": "incremental", "unit_shipping_price": 50},
    {"min_quantity": 4, "max_quantity": 10, "type": "fixed", "fixed_shipping_price": 200},
    {"min_quantity": 11, "max_quantity": null, "type": "incremental", "unit_shipping_price": 40}
  ]
}

Output:
1200.0

Explanation:
For Desktop (quantity 2): falls into [1, 5] bracket, type 'fixed'. Shipping = 1000.
For Monitor (quantity 7): falls into [4, 10] bracket, type 'fixed'. Shipping = 200.
Total shipping = 1000 + 200 = 1200.0

* */


public class Solution2 {
    private static ObjectMapper objectMapper;
    public static void main(String[] args) throws JsonProcessingException {
        objectMapper = new ObjectMapper();
        Scanner sc = new Scanner(System.in);

        String orderInput = sc.nextLine();
        String[] parts = orderInput.split("=");
        String orderJson = parts[1].trim();
        Order order = objectMapper.readValue(orderJson, Order.class);

        String shippingRatesString = sc.nextLine().split("=")[1].trim();
        JsonNode shippingRates = objectMapper.readTree(shippingRatesString);

        long total = getTotalPrice(shippingRates, order);
        System.out.println(total);
    }

    private static long getTotalPrice(JsonNode shippingRates, Order order) throws JsonProcessingException {
        List<Item> items = order.items();
        long ans = 0;
        for(Item item: items) {
            String itemName  = item.name();
            long quantity = item.quantity();
            ans+= getTieredPrice(itemName, quantity, shippingRates);
        }
        return ans;
    }

    private static long getTieredPrice(String itemName, long quantity, JsonNode shippingRates) throws JsonProcessingException {
        JsonNode tiersNode = shippingRates.get(itemName);
        if(tiersNode == null) {
            return 0L;
        }
        PriceTier[] tiers = objectMapper.treeToValue(tiersNode, PriceTier[].class);
        for(PriceTier p: tiers) {
            if(p.type().equals("fixed")) {
                return p.fixedShippingPrice();
            } else if(p.type().equals("incremental")) {
                if (p.minq() != null && quantity >= p.minq() && (p.maxq() == null || quantity <= p.maxq())) {
                    return quantity * p.unitShippingPrice();
                }
            }
        }
        return 0L;
    }
}

record Item(String name, long quantity) {}
record Order(List<Item> items) {}
record PriceTier(
        @JsonProperty("min_quantity") Integer minq,
        @JsonProperty("max_quantity") Integer maxq,
        String type,
        @JsonProperty("unit_shipping_price") long unitShippingPrice,
        @JsonProperty("fixed_shipping_price") long fixedShippingPrice
) {};

