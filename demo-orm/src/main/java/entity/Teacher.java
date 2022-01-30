package entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import orm.annotation.Id;
import orm.annotation.OneToMany;
import orm.annotation.Table;

import java.time.LocalDate;
import java.util.List;

@Table("teacher")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"classRooms", "courses"})
@Builder(toBuilder = true)
public class Teacher {

    @Id
    private Long id;
    private String lastname;
    private String firstname;
    private int gender;
    private LocalDate birthday;
    private LocalDate hireDate;

    @OneToMany(foreignKeyName = "teacher_id")
    private List<ClassRoom> classRooms;

    @OneToMany(foreignKeyName = "teacher_id")
    private List<Course> courses;
}
