package entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import orm.annotation.Id;
import orm.annotation.ManyToOne;
import orm.annotation.OneToMany;
import orm.annotation.Table;

import java.util.List;

@Table("class_room")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "students")
@Builder(toBuilder = true)
public class ClassRoom {

    @Id
    private Long id;

    private String name;

    @ManyToOne(foreignKeyName = "teacher_id")
    private Teacher teacher;

    @OneToMany(foreignKeyName = "class_room_id")
    private List<Student> students;
}
