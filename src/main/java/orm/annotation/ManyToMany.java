package orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Relation
public @interface ManyToMany {

    /**
     * @return foreign key to join on
     */
    String foreignKey();

    boolean nullable() default false;

    boolean lazy() default false;
}
