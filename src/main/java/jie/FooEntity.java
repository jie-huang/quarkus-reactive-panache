package jie;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@Table(name = "foo", schema = "jie")
public class FooEntity extends PanacheEntityBase {
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  public Integer id;

  public long value;
}
