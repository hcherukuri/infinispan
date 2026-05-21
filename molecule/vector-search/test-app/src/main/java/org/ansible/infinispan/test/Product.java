package org.ansible.infinispan.test;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.api.annotations.indexing.Keyword;
import org.infinispan.api.annotations.indexing.Text;
import org.infinispan.api.annotations.indexing.Vector;
import org.infinispan.api.annotations.indexing.option.VectorSimilarity;
@Indexed
public record Product(
        @Keyword(projectable = true, sortable = true) String id,
        @Keyword(projectable = true, sortable = true) String name,
        @Keyword(projectable = true, normalizer = "lowercase") String category,
        @Basic(projectable = true, sortable = true) Double price,
        @Text String description,
        @Vector(dimension = 3, similarity = VectorSimilarity.COSINE) float[] embedding) {
}
