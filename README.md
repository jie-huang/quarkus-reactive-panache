# quarkus-reactive-panache-demo

1. update application.properties

```
quarkus.scheduler.enabled=false
```

run `quarkus dev`

run `curl http://localhost:8080/cronjob`, `curl http://localhost:8080/count`, it works well.

2. update application.properties

```
quarkus.scheduler.enabled=true
``` 

wait for a while, errors happen and DB connection will not work anymore.
