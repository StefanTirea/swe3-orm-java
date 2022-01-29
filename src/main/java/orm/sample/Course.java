package orm.sample;

import lombok.Data;
import lombok.NoArgsConstructor;
import orm.annotation.Id;
import orm.annotation.ManyToMany;
import orm.annotation.Table;

import java.util.List;

@Table("course")
@Data
@NoArgsConstructor
public class Course {

    @Id
    private Long id;
    private String name;

    @ManyToMany(tableName = "student_courses", foreignKeyName = "c_id", lazy = true)
    private List<Student> students;
}
