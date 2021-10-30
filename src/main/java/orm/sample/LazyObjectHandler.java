package orm.sample;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class LazyObjectHandler<T> implements InvocationHandler {

    private final Supplier<T> lazy;
    private final Consumer<T> callSetMethod;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        T o = lazy.get();
        callSetMethod.accept(o);
        return method.invoke(o, args);
    }
}
