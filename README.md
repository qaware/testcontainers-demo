# Testcontainers demo

This demo project shows how to write an integration test with Docker containers for a Java EE micro service using the [Testcontainers](https://www.testcontainers.org/) library.

## Overview

**book-loader** is a sample Java EE micro service importing book data from a PostgresSQL database into a Solr search index. It runs in a Payara Micro App server and offers a simple JAX-RS Web service to trigger a load.

**book-loader-it** is an integration test for book-loader using the Testcontainers library. It starts the book-loader (system under test) as well as the two databases as Docker containers.

## Prerequisites

* Docker, preferably on Linux
    * see [Testcontainers Docker requirements](https://www.testcontainers.org/supported_docker_environment/)
    * the demo was also tested successfully on [Windows 10](https://docs.microsoft.com/en-us/virtualization/windowscontainers/quick-start/quick-start-windows-10-linux)
* Java JDK 8
* Maven 3

Make sure to clone the repository and navigate to the directory.

## Run the integration test

A normal Maven build will compile the app, build a Docker image and run the integration test. The test will automatically start Docker containers for Postgres, Solr and the book-loader, execute the book-loader and clean up the containers afterwards.

`mvn clean install`

## Run the application manually

If you want to manually run the application to get a feeling for what we are automating, you can use the following commands.

Create a named Docker network.

`docker network create testcontainers-demo`

Start a Postgres database container in our network.

`docker run -d -p 5432:5432 --env POSTGRES_PASSWORD=123 --name testcontainers-demo-postgres --network testcontainers-demo postgres:11.2`

Fill the Postgres database with some test data. For example, execute the prepared script with the Postgres command line client.

`psql postgresql://postgres:123@localhost:5432/postgres -f book-loader-it/src/test/resources/insert-test-data.sql`

Start a Solr search index container in our network and create an empty "books" core.

`docker run -d -p 8983:8983 --name testcontainers-demo-solr --network testcontainers-demo solr:7.7.1 solr-create -c books`

You can now open the Solr admin UI by opening `http://localhost:8983` in a browser and check that there is an empty "books" core. The same can be checked with curl. It should return an successful, but empty search result.

`curl http://localhost:8983/solr/books/select?q=*:*`

Build the book-loader with Maven. This also creates the Docker image.

`mvn clean install -DskipTests`

Start a book-loader container in our network.

`docker run -d -p 8080:8080 --name testcontainers-demo-book-loader --network testcontainers-demo testcontainers/book-loader:latest`

Trigger a book-loader load by calling the Web service.

`curl http://localhost:8080/book-loader/start`

Check that the book data was loaded into Solr.

`curl http://localhost:8983/solr/books/select?q=*:*`