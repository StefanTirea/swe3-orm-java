package orm.sample;

import lombok.Builder;
import orm.annotation.Id;
import orm.annotation.ManyToMany;
import orm.annotation.ManyToOne;
import orm.annotation.Table;

import java.util.List;

@Table("subject")
@Builder
public class Subject {

    @Id
    private Long id;
    private String name;

    @ManyToMany(foreignKey = "subject_id", lazy = true)
    private List<Student> students;
}
