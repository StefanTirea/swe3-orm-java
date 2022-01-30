package entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import orm.annotation.Id;
import orm.annotation.SubEntity;

import java.time.LocalDate;

@SubEntity
@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder(toBuilder = true)
public class Person {

    @Id
    private Long id;
    private String lastname;
    private String firstname;
    private int gender;
    private LocalDate birthday;
}
