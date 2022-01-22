package orm.sample;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.lang.reflect.Method;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class LazyLoadingInterceptor {

    private final Supplier<?> lazy;
    private Object loadedEntity;

    @RuntimeType
    @SneakyThrows
    public Object intercept(@AllArguments Object[] allArguments,
                            @Origin Method method) {
        if (loadedEntity == null) {
            loadedEntity = lazy.get();
        }
        return method.invoke(loadedEntity, allArguments);
    }
}
