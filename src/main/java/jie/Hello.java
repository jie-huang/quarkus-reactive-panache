package jie;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import io.smallrye.mutiny.Uni;

@Path("/")
public class Hello {
  @Inject
  CronJob cronJob;

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/count")
  public Uni<Long> hello1() {
    return FooEntity.count();
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/cronjob")
  public Uni<Long> hello2() {
    return cronJob.cronJobImpl();
  }
}
