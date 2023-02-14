package jie;

import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.sql.DataSource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;
import org.jboss.logging.Logger;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.flyway.runtime.FlywayContainer;
import io.quarkus.flyway.runtime.FlywayContainerProducer;
import io.quarkus.flyway.runtime.QuarkusPathLocationScanner;
import io.quarkus.runtime.StartupEvent;

// https://github.com/quarkusio/quarkus/issues/10716

@ApplicationScoped
public class MigrationService {
  @Inject
  Logger logger;

  @ConfigProperty(name = "quarkus.datasource.reactive.url")
  String datasourceUrl;

  @ConfigProperty(name = "quarkus.datasource.username")
  String datasourceUsername;

  @ConfigProperty(name = "quarkus.datasource.password")
  String datasourcePassword;

  @ConfigProperty(name = "todo.migration.files")
  List<String> files;

  public void runFlywayMigration(@Observes StartupEvent event) {
    logger.info("Initializing flyway...");
    QuarkusPathLocationScanner.setApplicationMigrationFiles(
        files.stream().map(file -> "db/migration/" + file).collect(Collectors.toList()));
    DataSource datasource = Flyway.configure()
        .dataSource(getDatasourceUrl(), datasourceUsername, datasourcePassword).getDataSource();
    try (InstanceHandle<FlywayContainerProducer> instance =
        Arc.container().instance(FlywayContainerProducer.class)) {
      FlywayContainerProducer flywayProducer = instance.get();
      FlywayContainer flywayContainer =
          flywayProducer.createFlyway(datasource, "<default>", true, true);
      Flyway flyway = flywayContainer.getFlyway();
      flyway.migrate();
    }
  }

  private String getDatasourceUrl() {
    if (datasourceUrl.startsWith("vertx-reactive:"))
      return "jdbc:" + datasourceUrl.substring("vertx-reactive:".length());
    else
      return "jdbc:" + datasourceUrl;
  }
}
