# https://github.com/payara/docker-payaramicro
FROM payara/micro
ADD --chown=payara https://jdbc.postgresql.org/download/postgresql-42.2.5.jar /opt/payara/
COPY target/book-loader-*.war /opt/payara/deployments/
CMD ["--deploymentDir", "/opt/payara/deployments", "--addJars", "postgresql-42.2.5.jar"]