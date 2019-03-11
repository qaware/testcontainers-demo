package de.qaware.demo.testcontainers;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

/**
 * A sample Java EE micro service importing book data from a PostgresSQL database into a Solr search index.
 */
@Path("book-loader")
@ApplicationScoped
@Slf4j
public class BookLoader {

    private static final String POSTGRES_JDBC_URL = "jdbc:postgresql://testcontainers-demo-postgres:5432/postgres";
    private static final String SOLR_URL = "http://testcontainers-demo-solr:8983/solr/books/";

    private final Jdbi jdbi;

    public BookLoader() throws ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        jdbi = Jdbi.create(POSTGRES_JDBC_URL, "postgres", "123");
    }

    @GET
    @Path("start")
    @Produces(MediaType.TEXT_PLAIN)
    public Response start() throws IOException, SolrServerException {
        log.info("Starting loader with jdbcUrl = {} and solrUrl = {}", POSTGRES_JDBC_URL, SOLR_URL);

        // Load books from source database
        List<Book> books = jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM books;")
                    .registerRowMapper(BeanMapper.factory(Book.class))
                    .mapTo(Book.class)
                    .list()
        );

        // Write books into target database
        try (SolrClient solrClient = new HttpSolrClient.Builder(SOLR_URL).build()) {
            solrClient.addBeans(books);
            solrClient.commit();
        }

        // Return successful response
        log.info("Finished loader");
        return Response.ok("Loaded " + books.size() + " books").build();
    }
}
