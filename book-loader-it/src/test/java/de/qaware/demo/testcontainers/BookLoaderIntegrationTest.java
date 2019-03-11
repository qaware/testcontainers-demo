package de.qaware.demo.testcontainers;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * An integration test for {@link BookLoader} with Docker containers using the Testcontainers library.
 */
@Slf4j
public class BookLoaderIntegrationTest {

    private static final String DOCKER_IMAGE_POSTGRES = "postgres:11.2";
    private static final String DOCKER_IMAGE_SOLR = "solr:7.7.1";
    private static final String DOCKER_IMAGE_LOADER = "testcontainers/book-loader:latest";

    private static final int PORT_SOLR = 8983;
    private static final int PORT_PAYARA_MICRO = 8080;
    private static final int PORT_DEBUGGER = 5005;

    private static GenericContainer solrContainer;
    private static GenericContainer loaderContainer;

    private static Client httpClient = ClientBuilder.newClient();

    @BeforeClass
    public static void setUp() {
        Network network = Network.newNetwork();

        // Postgres database
        new PostgreSQLContainer(DOCKER_IMAGE_POSTGRES)
                .withInitScript("insert-test-data.sql") // create schema and add test data
                .withDatabaseName("postgres")
                .withUsername("postgres")
                .withPassword("123")
                .withNetwork(network)
                .withNetworkAliases("testcontainers-demo-postgres")
                .start();

        // Solr search index
        solrContainer = new GenericContainer(DOCKER_IMAGE_SOLR)
                .withCommand("solr-create -c books") // create empty "books" core
                .withExposedPorts(PORT_SOLR)
                .withNetwork(network)
                .withNetworkAliases("testcontainers-demo-solr")
                .waitingFor(Wait.forHttp("/solr/books/select?q=*:*"));
        solrContainer.start();

        // Book-Loader micro service
        loaderContainer = new GenericContainer(DOCKER_IMAGE_LOADER)
                .withExposedPorts(PORT_PAYARA_MICRO, PORT_DEBUGGER)
                .withNetwork(network)
                .withNetworkAliases("testcontainers-demo-book-loader")
                .withLogConsumer(new Slf4jLogConsumer(log))
                .waitingFor(Wait.forLogMessage(".*Payara Micro .* ready.*\\n", 1));
        loaderContainer.start();
    }

    @Test
    public void testLoader() throws Exception {
        String baseSolrUrl = "http://" + solrContainer.getContainerIpAddress()
                + ":" + solrContainer.getMappedPort(PORT_SOLR) + "/solr/books";

        try (SolrClient solrClient = new HttpSolrClient.Builder(baseSolrUrl).build()) {
            // Given an empty Solr search index
            assertThat(solrClient.query(new SolrQuery("*:*")).getResults().size(), is(0));

            // When the Loader is executed
            String loaderUrl = "http://" + loaderContainer.getContainerIpAddress() + ":"
                    + loaderContainer.getMappedPort(PORT_PAYARA_MICRO) + "/book-loader/start";
            Response loaderResponse = httpClient.target(loaderUrl).request().get();
            int status = loaderResponse.getStatus();
            String message = loaderResponse.readEntity(String.class);
            log.info("Loader response has status '{}' and message '{}'", status, message);

            // Then we should receive a successful response and Solr should be filled with data
            assertThat(status, is(Response.Status.OK.getStatusCode()));
            assertThat(message, is("Loaded 3 books"));
            assertThat(solrClient.query(new SolrQuery("*:*")).getResults().size(), is(3));
        }
    }
}