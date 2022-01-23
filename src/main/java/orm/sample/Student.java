package orm.sample;

import orm.annotation.Id;
import orm.annotation.ManyToMany;
import orm.annotation.Table;

import java.util.List;

@Table("student")
public class Student {

    @Id
    private Long id;
    private String firstname;

    @ManyToMany(foreignKey = "student_id", lazy = true)
    private List<Subject> subjects;
}
