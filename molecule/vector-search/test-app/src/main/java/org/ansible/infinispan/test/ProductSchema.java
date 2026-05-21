package org.ansible.infinispan.test;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
        includeClasses = { Product.class },
        schemaPackageName = "ansible.test",
        schemaFileName = "product.proto")
public interface ProductSchema extends GeneratedSchema {
}
