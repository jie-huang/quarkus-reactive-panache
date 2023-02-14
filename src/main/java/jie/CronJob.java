package jie;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.jboss.logging.Logger;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class CronJob {
  @Inject
  Logger logger;

  int n = 0;

  @Scheduled(cron = "0 * * * * ? *", concurrentExecution = ConcurrentExecution.SKIP)
  Uni<Void> cronJob() {
    return cronJobImpl().replaceWithVoid();
  }

  Uni<Long> cronJobImpl() {
    logger.info("start cron job");
    FooEntity foo = new FooEntity();
    foo.value = 10000;
    return Panache.withTransaction(foo::persistAndFlush).onItemOrFailure()
        .transformToUni(new BiFunction<PanacheEntityBase, Throwable, Uni<? extends Long>>() {
          @Override
          public Uni<? extends Long> apply(PanacheEntityBase entity, Throwable t) {
            if (t != null) {
              StringWriter sw = new StringWriter();
              PrintWriter pw = new PrintWriter(sw);
              t.printStackTrace(pw);
              logger.info("failed to persistAndFlush: " + sw.toString());
              return Uni.createFrom().item(0L);
            }
            return FooEntity.count().onItem()
                .transformToUni(new Function<Long, Uni<? extends Long>>() {
                  @Override
                  public Uni<? extends Long> apply(Long n) {
                    if (n % 2 == 0) {
                      logger.info("total even number records");
                      return Uni.createFrom().item(n);
                    } else {
                      FooEntity fooCount = new FooEntity();
                      fooCount.value = n;
                      return Panache.withTransaction(fooCount::persistAndFlush).onItem()
                          .transform(new Function<PanacheEntityBase, Long>() {
                            @Override
                            public Long apply(PanacheEntityBase fe) {
                              return n + 1;
                            }
                          });
                    }
                  }
                });
          }
        });
  }
}
