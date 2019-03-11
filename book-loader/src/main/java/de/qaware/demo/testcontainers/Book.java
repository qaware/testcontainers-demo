package de.qaware.demo.testcontainers;

import lombok.Data;
import org.apache.solr.client.solrj.beans.Field;

/**
 * A book entity.
 */
@Data
public class Book {
    @Field
    private long id;
    @Field
    private String title;
    @Field
    private String author;
}
