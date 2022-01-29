package orm.sample;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import orm.annotation.AutoGenerated;
import orm.annotation.Id;
import orm.annotation.OneToMany;
import orm.annotation.Table;

import java.util.List;

@Table("user_entity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class UserEntity {

    @Id
    @AutoGenerated
    private Long id;

    private String firstname;

    private String lastname;

    private Integer age;

    @OneToMany(foreignKeyName = "user_id", lazy = true)
    private List<LogEntity> logs;
}
