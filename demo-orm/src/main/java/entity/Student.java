package entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import orm.annotation.Id;
import orm.annotation.ManyToMany;
import orm.annotation.ManyToOne;
import orm.annotation.Table;

import java.time.LocalDate;
import java.util.List;

@Table("student")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "courses")
@Builder(toBuilder = true)
public class Student {

    @Id
    private Long id;
    private String lastname;
    private String firstname;
    private int gender;
    private LocalDate birthday;
    private int grade;

    @ManyToOne(foreignKeyName = "class_room_id")
    private ClassRoom classRoom;

    @ManyToMany(tableName = "student_courses", foreignKeyName = "student_id")
    @Singular
    private List<Course> courses;
}
