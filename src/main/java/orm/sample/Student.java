package orm.sample;

import lombok.Data;
import lombok.NoArgsConstructor;
import orm.annotation.Id;
import orm.annotation.ManyToMany;
import orm.annotation.Table;

import java.util.List;

@Table("student")
@Data
@NoArgsConstructor
public class Student {

    @Id
    private Long id;
    private String firstname;

    @ManyToMany(tableName = "student_courses", foreignKeyName = "s_id", lazy = false)
    private List<Course> courses;
}
