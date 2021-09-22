package orm.sample;

import lombok.Data;
import orm.annotation.AutoGenerated;
import orm.annotation.ForeignKey;
import orm.annotation.Id;
import orm.annotation.ManyToOne;
import orm.annotation.OneToMany;
import orm.annotation.Table;

import java.util.List;

@Table("logs")
@Data
public class LogEntity {

    @Id
    @AutoGenerated
    private Long id;

    @ForeignKey
    private Long user_id;

    private String entry;

    @ManyToOne
    private UserEntity user;
}
