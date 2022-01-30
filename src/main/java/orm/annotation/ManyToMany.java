package orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToMany {

    /**
     * @return foreign key to join on
     */
    String foreignKeyName();

    /**
     * @return table name to join on
     */
    String tableName();

    boolean lazy() default true;
}
