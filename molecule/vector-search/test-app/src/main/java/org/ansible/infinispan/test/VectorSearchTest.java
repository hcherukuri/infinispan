package org.ansible.infinispan.test;

import java.util.List;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.api.query.Query;
import org.infinispan.protostream.SerializationContextInitializer;

public class VectorSearchTest {

    private static final String CACHE_NAME = "products";

    private static RemoteCacheManager cacheManager;
    private static RemoteCache<String, Product> cache;

    public static void main(String[] args) {
        try {
            String host = getEnvOrDefault("INFINISPAN_HOST", "localhost");
            int port = Integer.parseInt(getEnvOrDefault("INFINISPAN_PORT", "11222"));
            String username = getEnvOrDefault("INFINISPAN_USER", "admin");
            String password = getEnvOrDefault("INFINISPAN_PASSWORD", "password");

            System.out.println("=== Infinispan Vector Search Test ===");
            System.out.println("Connecting to " + host + ":" + port);

            connect(host, port, username, password);

            System.out.println("\nPopulating test data...");
            populateProducts();
            System.out.println("Loaded " + cache.size() + " products.");

            System.out.println("\n=== Running Vector Search Tests ===\n");
            testBasicKnnSearch();
            testHybridSearchWithCategory();
            testHybridSearchWithPriceRange();

            System.out.println("\n=== All Tests Passed ===");
            disconnect();
            System.exit(0);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void connect(String host, int port, String username, String password) {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer().host(host).port(port);
        builder.security().authentication().username(username).password(password);
        builder.addContextInitializer((SerializationContextInitializer) new ProductSchemaImpl());

        cacheManager = new RemoteCacheManager(builder.build());
        cacheManager.administration().schemas().createOrUpdate(new ProductSchemaImpl());
        cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            throw new RuntimeException(
                    "Cache 'products' not found. Ensure the cache is created by Ansible with vector search configuration.");
        }
        cache.clear();
    }

    private static void disconnect() {
        if (cacheManager != null) {
            cacheManager.stop();
        }
    }

    private static void populateProducts() {
        cache.put("p1", new Product("p1", "Laptop", "Electronics", 999.99,
                "High-performance laptop with fast processor and SSD storage",
                new float[] { 0.95f, 0.05f, 0.05f }));
        cache.put("p2", new Product("p2", "Smartphone", "Electronics", 699.99,
                "Latest smartphone with advanced camera and 5G connectivity",
                new float[] { 0.9f, 0.1f, 0.05f }));
        cache.put("p3", new Product("p3", "T-Shirt", "Clothing", 19.99,
                "Cotton t-shirt in various colors and sizes",
                new float[] { 0.05f, 0.95f, 0.05f }));
        cache.put("p4", new Product("p4", "Jeans", "Clothing", 49.99,
                "Denim jeans with comfortable fit",
                new float[] { 0.05f, 0.9f, 0.1f }));
        cache.put("p5", new Product("p5", "Coffee Beans", "Food", 12.99,
                "Premium arabica coffee beans, dark roast",
                new float[] { 0.05f, 0.05f, 0.95f }));
        cache.put("p6", new Product("p6", "Tablet", "Electronics", 499.99,
                "Portable tablet with high-resolution display",
                new float[] { 0.85f, 0.1f, 0.1f }));
    }

    private static void testBasicKnnSearch() {
        System.out.println("Test 1: Basic kNN Search");
        System.out.println("Query: 3 products closest to 'tech gadgets' [0.9, 0.1, 0.0]");

        Query query = cache.query("from ansible.test.Product p where p.embedding <-> [:v]~:k");
        query.setParameter("v", new float[] { 0.9f, 0.1f, 0.0f });
        query.setParameter("k", 3);

        @SuppressWarnings("unchecked")
        List<Product> results = query.list();
        if (results.size() != 3) {
            throw new RuntimeException("Expected 3 results, got " + results.size());
        }
        for (Product p : results) {
            System.out.printf("  %-15s %-15s $%.2f%n", p.name(), p.category(), p.price());
        }
        if (!"Electronics".equals(results.get(0).category())) {
            throw new RuntimeException(
                    "Expected top result to be Electronics, got " + results.get(0).category());
        }
        System.out.println("  PASSED\n");
    }

    private static void testHybridSearchWithCategory() {
        System.out.println("Test 2: Hybrid Search with Category Filter");
        System.out.println("Query: Electronics products closest to [0.9, 0.1, 0.0]");

        Query query = cache.query(
                "select p.name, p.category, p.price, score(p) from ansible.test.Product p "
                        + "where p.embedding <-> [:v]~:k filtering p.category = 'Electronics'");
        query.setParameter("v", new float[] { 0.9f, 0.1f, 0.0f });
        query.setParameter("k", 5);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.list();
        if (results.isEmpty()) {
            throw new RuntimeException("Expected results, got none");
        }
        for (Object[] row : results) {
            System.out.printf("  %-15s %-15s $%.2f  score=%.4f%n", row[0], row[1], row[2], row[3]);
            if (!"Electronics".equals(row[1])) {
                throw new RuntimeException("Expected all results to be Electronics, got " + row[1]);
            }
        }
        System.out.println("  PASSED\n");
    }

    private static void testHybridSearchWithPriceRange() {
        System.out.println("Test 3: Hybrid Search with Price Range");
        System.out.println("Query: Products under $50 closest to 'clothing' [0.0, 0.9, 0.1]");

        Query query = cache.query(
                "select p.name, p.category, p.price from ansible.test.Product p "
                        + "where p.embedding <-> [:v]~:k filtering p.price < 50.0");
        query.setParameter("v", new float[] { 0.0f, 0.9f, 0.1f });
        query.setParameter("k", 3);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.list();
        if (results.isEmpty()) {
            throw new RuntimeException("Expected results, got none");
        }
        for (Object[] row : results) {
            System.out.printf("  %-15s %-15s $%.2f%n", row[0], row[1], row[2]);
            Double price = (Double) row[2];
            if (price >= 50.0) {
                throw new RuntimeException("Expected price < $50, got $" + price);
            }
        }
        System.out.println("  PASSED\n");
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
