package orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link ManyToOne} is used on the entity were the foreignKey is actually located
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToOne {

    /**
     * @return foreign key to join on
     */
    String foreignKeyName();

    boolean lazy() default true;
}
