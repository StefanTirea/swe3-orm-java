package orm.entity;

import lombok.Getter;
import lombok.Setter;
import orm.annotation.Id;
import orm.annotation.Table;

@Table("teacher")
@Getter
@Setter
public class TestEntity {

    @Id
    private Long id;
    private String name;
}
