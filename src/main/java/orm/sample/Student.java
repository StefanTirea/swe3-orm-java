package orm.sample;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import orm.annotation.Id;
import orm.annotation.ManyToMany;
import orm.annotation.Table;

import java.util.List;

@Table("student")
@Data
@ToString(exclude = "courses")
@NoArgsConstructor
public class Student {

    private String firstname;
    @Id
    private Long id;

    @ManyToMany(tableName = "student_courses", foreignKeyName = "s_id", lazy = true)
    private List<Course> courses;
}
