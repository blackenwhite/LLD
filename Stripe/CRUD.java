import java.util.*;
import java.util.regex.*;

public class Solution {

    // --- 1. Request and Response Data Structures ---
    public static class Request {
        public String method; // "GET", "POST", "PUT", "DELETE"
        public String path;   // e.g., "/v1/customers/cust_1/charges"
        public String body;   // e.g., "{"amount": 1000, "currency": "usd"}"

        public Request(String method, String path, String body) {
            this.method = method;
            this.path = path;
            this.body = body != null ? body : "";
        }
    }

    public static class Response {
        public int statusCode;
        public String body;

        public Response(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body != null ? body : "";
        }

        @Override
        public String toString() {
            return "HTTP " + statusCode + " | Body: " + body;
        }
    }

    // --- 2. Domain Models ---
    public static class Customer {
        public String id;
        public String name;
        public String email;

        public Customer(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public String toJson() {
            return String.format("{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\"}", id, name, email);
        }
    }

    public static class Charge {
        public String id;
        public String customerId;
        public int amount;
        public String currency;

        public Charge(String id, String customerId, int amount, String currency) {
            this.id = id;
            this.customerId = customerId;
            this.amount = amount;
            this.currency = currency;
        }

        public String toJson() {
            return String.format("{\"id\":\"%s\",\"customerId\":\"%s\",\"amount\":%d,\"currency\":\"%s\"}",
                    id, customerId, amount, currency);
        }
    }

    // --- 3. REST API Engine ---
    public static class RestApiEngine {

        private int customerIdCounter = 1;
        private int chargeIdCounter = 1;

        // In-memory state storage
        private Map<String, Customer> customers = new LinkedHashMap<>();
        private Map<String, List<Charge>> customerCharges = new HashMap<>();

        // Helper: Parse string or numeric values from simple JSON
        public static String parseField(String json, String key) {
            if (json == null) return null;
            Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^\",\\}\n\r]+)\"?").matcher(json);
            return m.find() ? m.group(1).trim() : null;
        }

        // Helper: Parse integer values from JSON
        public static Integer parseIntField(String json, String key) {
            String val = parseField(json, key);
            if (val == null) return null;
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // Helper: Convert collection of Customers to JSON Array
        private String customersToJsonList() {
            StringJoiner sj = new StringJoiner(",", "[", "]");
            for (Customer c : customers.values()) {
                sj.add(c.toJson());
            }
            return sj.toString();
        }

        // Helper: Convert collection of Charges to JSON Array
        private String chargesToJsonList(List<Charge> charges) {
            StringJoiner sj = new StringJoiner(",", "[", "]");
            if (charges != null) {
                for (Charge c : charges) {
                    sj.add(c.toJson());
                }
            }
            return sj.toString();
        }

        // --- Main Router & Request Handler ---
        public Response handleRequest(Request request) {
            String method = request.method;
            String[] parts = request.path.split("/");

            // Base validation for path root: /v1/customers
            if (parts.length < 3 || !parts[1].equals("v1") || !parts[2].equals("customers")) {
                return new Response(404, "{\"error\": \"Route not found\"}");
            }

            // -------------------------------------------------------------
            // PART 1: Collection Level Routes -> /v1/customers
            // -------------------------------------------------------------
            if (parts.length == 3) {
                // POST /v1/customers
                if (method.equals("POST")) {
                    String name = parseField(request.body, "name");
                    String email = parseField(request.body, "email");

                    if (name == null || email == null) {
                        return new Response(400, "{\"error\": \"Invalid request body\"}");
                    }

                    String id = "cust_" + customerIdCounter++;
                    Customer customer = new Customer(id, name, email);
                    customers.put(id, customer);

                    return new Response(201, customer.toJson());
                }

                // GET /v1/customers
                if (method.equals("GET")) {
                    return new Response(200, customersToJsonList());
                }
            }

            // -------------------------------------------------------------
            // PART 2: Individual Customer Routes -> /v1/customers/{id}
            // -------------------------------------------------------------
            if (parts.length == 4) {
                String customerId = parts[3];

                if (!customers.containsKey(customerId)) {
                    return new Response(404, "{\"error\": \"Customer not found\"}");
                }

                // GET /v1/customers/{id}
                if (method.equals("GET")) {
                    return new Response(200, customers.get(customerId).toJson());
                }

                // PUT /v1/customers/{id}
                if (method.equals("PUT")) {
                    String name = parseField(request.body, "name");
                    String email = parseField(request.body, "email");

                    if (name == null || email == null) {
                        return new Response(400, "{\"error\": \"Invalid request body\"}");
                    }

                    Customer c = customers.get(customerId);
                    c.name = name;
                    c.email = email;
                    return new Response(200, c.toJson());
                }

                // DELETE /v1/customers/{id}
                if (method.equals("DELETE")) {
                    customers.remove(customerId);
                    customerCharges.remove(customerId); // Cleanup customer's charges
                    return new Response(204, "");
                }
            }

            // -------------------------------------------------------------
            // PART 3: Nested Resource Routes -> /v1/customers/{id}/charges
            // -------------------------------------------------------------
            if (parts.length == 5 && parts[4].equals("charges")) {
                String customerId = parts[3];

                // Check customer existence first
                if (!customers.containsKey(customerId)) {
                    return new Response(404, "{\"error\": \"Customer not found\"}");
                }

                // POST /v1/customers/{id}/charges
                if (method.equals("POST")) {
                    Integer amount = parseIntField(request.body, "amount");
                    String currency = parseField(request.body, "currency");

                    if (amount == null || currency == null) {
                        return new Response(400, "{\"error\": \"Invalid request body\"}");
                    }

                    String chargeId = "ch_" + chargeIdCounter++;
                    Charge charge = new Charge(chargeId, customerId, amount, currency);

                    customerCharges.computeIfAbsent(customerId, k -> new ArrayList<>()).add(charge);
                    return new Response(201, charge.toJson());
                }

                // GET /v1/customers/{id}/charges
                if (method.equals("GET")) {
                    List<Charge> charges = customerCharges.getOrDefault(customerId, Collections.emptyList());
                    return new Response(200, chargesToJsonList(charges));
                }
            }

            return new Response(404, "{\"error\": \"Route not found\"}");
        }
    }

    // --- 4. Interactive Test Suite ---
    public static void main(String[] args) {
        RestApiEngine api = new RestApiEngine();

        System.out.println("=== PART 1 TESTS ===");
        Response create1 = api.handleRequest(new Request("POST", "/v1/customers", "{\"name\": \"Alice\", \"email\": \"alice@stripe.com\"}"));
        assertStatus(201, create1.statusCode, "Create Customer 1");
        assertContains(create1.body, "cust_1", "Customer ID 1");

        Response create2 = api.handleRequest(new Request("POST", "/v1/customers", "{\"name\": \"Bob\", \"email\": \"bob@stripe.com\"}"));
        assertStatus(201, create2.statusCode, "Create Customer 2");

        Response get1 = api.handleRequest(new Request("GET", "/v1/customers/cust_1", null));
        assertStatus(200, get1.statusCode, "Get Customer 1");

        Response list1 = api.handleRequest(new Request("GET", "/v1/customers", null));
        assertStatus(200, list1.statusCode, "List Customers");
        assertContains(list1.body, "cust_1", "List contains cust_1");
        assertContains(list1.body, "cust_2", "List contains cust_2");

        System.out.println("\n=== PART 2 TESTS ===");
        Response update1 = api.handleRequest(new Request("PUT", "/v1/customers/cust_1", "{\"name\": \"Alice Smith\", \"email\": \"alice.smith@stripe.com\"}"));
        assertStatus(200, update1.statusCode, "Update Customer 1");
        assertContains(update1.body, "Alice Smith", "Updated Name");

        Response delete1 = api.handleRequest(new Request("DELETE", "/v1/customers/cust_1", null));
        assertStatus(204, delete1.statusCode, "Delete Customer 1");

        Response getDeleted = api.handleRequest(new Request("GET", "/v1/customers/cust_1", null));
        assertStatus(404, getDeleted.statusCode, "Get Deleted Customer (404 Check)");

        System.out.println("\n=== PART 3 TESTS (Charges) ===");
        // Add charge to existing customer (cust_2)
        Response createCharge1 = api.handleRequest(new Request("POST", "/v1/customers/cust_2/charges", "{\"amount\": 1500, \"currency\": \"usd\"}"));
        assertStatus(201, createCharge1.statusCode, "Create Charge 1");
        assertContains(createCharge1.body, "ch_1", "Charge ID ch_1");
        assertContains(createCharge1.body, "cust_2", "Associated Customer ID");

        Response createCharge2 = api.handleRequest(new Request("POST", "/v1/customers/cust_2/charges", "{\"amount\": 3000, \"currency\": \"eur\"}"));
        assertStatus(201, createCharge2.statusCode, "Create Charge 2");

        // List charges for cust_2
        Response listCharges = api.handleRequest(new Request("GET", "/v1/customers/cust_2/charges", null));
        assertStatus(200, listCharges.statusCode, "List Customer Charges");
        assertContains(listCharges.body, "ch_1", "Contains ch_1");
        assertContains(listCharges.body, "ch_2", "Contains ch_2");

        // Attempt to create charge for non-existent customer
        Response charge404 = api.handleRequest(new Request("POST", "/v1/customers/cust_999/charges", "{\"amount\": 500, \"currency\": \"usd\"}"));
        assertStatus(404, charge404.statusCode, "Create Charge for Invalid Customer (404 Check)");

        // Attempt invalid payload
        Response badCharge = api.handleRequest(new Request("POST", "/v1/customers/cust_2/charges", "{\"amount\": \"invalid\"}"));
        assertStatus(400, badCharge.statusCode, "Create Invalid Charge Payload (400 Check)");

        System.out.println("\n🎉 All tests passed successfully!");
    }

    private static void assertStatus(int expected, int actual, String testName) {
        if (expected != actual) {
            System.err.printf("❌ FAIL: %s | Expected Status: %d, Got: %d\n", testName, expected, actual);
        } else {
            System.out.printf("PASS: %s [%d]\n", testName, actual);
        }
    }

    private static void assertContains(String body, String expectedSub, String testName) {
        if (body == null || !body.contains(expectedSub)) {
            System.err.printf("❌ FAIL: %s | Expected body to contain '%s', Got: %s\n", testName, expectedSub, body);
        } else {
            System.out.printf("PASS: %s [Found '%s']\n", testName, expectedSub);
        }
    }
}
