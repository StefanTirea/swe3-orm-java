package entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import orm.annotation.Id;
import orm.annotation.ManyToMany;
import orm.annotation.ManyToOne;
import orm.annotation.Table;

import java.util.List;

@Table("courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Course {

    @Id
    private Long id;
    private boolean active;
    private String name;

    @ManyToOne(foreignKeyName = "teacher_id")
    private Teacher teacher;

    @ManyToMany(tableName = "student_courses", foreignKeyName = "courses_id")
    private List<Student> students;
}
