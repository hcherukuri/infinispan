# Vector search Molecule test client

Maven project used by the `vector-search` Molecule scenario. It builds a shaded JAR that
exercises kNN and hybrid vector queries against the `products` cache.

## Build

```bash
mvn -q package -DskipTests
```

The fat JAR is written to `target/vector-search-test-1.0.0.jar`. The scenario `prepare.yml`
builds this artifact on the controller with Java 25 (Maven required) and copies it to `/tmp`
on test hosts, matching the Infinispan 16+ runtime requirement.

## Sources

- `Product.java` — ProtoStream/indexed entity (`ansible.test.Product`)
- `ProductSchema.java` — schema definition processed by `protostream-processor`
- `VectorSearchTest.java` — Hot Rod client test entry point (`Main-Class`)
