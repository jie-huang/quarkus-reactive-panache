package jie;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  int jobIndex = 0;

  @Scheduled(cron = "0 * * * * ? *", concurrentExecution = ConcurrentExecution.SKIP)
  Uni<Void> cronJob() {
    ++jobIndex;
    return cronJobImpl().replaceWithVoid();
  }

  Uni<Long> cronJobImpl() {
    logger.info("start cronjob: " + jobIndex);
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
                    if (n % 2 == 1) {
                      logger.info("total number of records: " + n);
                      return Uni.createFrom().item(n);
                    } else {
                      FooEntity fooCount = new FooEntity();
                      fooCount.value = n;
                      return Panache.withTransaction(fooCount::persistAndFlush).onItem()
                          .transform(new Function<PanacheEntityBase, Long>() {
                            @Override
                            public Long apply(PanacheEntityBase fe) {
                              backgroundJob().subscribe().with(sum -> logger.info("result: " + sum),
                                  t -> logger.error("exception ", t));
                              logger.info("cronjob is done: " + jobIndex);
                              return n + 1;
                            }
                          });
                    }
                  }
                });
          }
        });
  }

  Uni<Long> backgroundJob() {
    int ji = jobIndex;
    logger.info("background job is starting: " + ji);
    List<FooEntity> tasks = new ArrayList<FooEntity>(10);
    for (int i = 0; i < 10; ++i) {
      FooEntity entity = new FooEntity();
      entity.value = ji * 100 + i;
      tasks.add(entity);
    }
    List<Uni<Long>> result = tasks.stream().map(new Function<FooEntity, Uni<Long>>() {
      @Override
      public Uni<Long> apply(FooEntity foo) {
        return Panache.withTransaction(foo::persistAndFlush).onItem()
            .transform(new Function<PanacheEntityBase, Long>() {
              @Override
              public Long apply(PanacheEntityBase t) {
                return ((FooEntity) t).value;
              }
            });
      }
    }).collect(Collectors.toList());
    return Uni.combine().all().unis(result).combinedWith(Long.class, Function.identity()).onItem()
        .transform(new Function<List<Long>, Long>() {
          @Override
          public Long apply(List<Long> t) {
            logger.info("background job is done: " + ji);
            return t.stream().mapToLong(l -> l).sum();
          }
        });
  }
}
