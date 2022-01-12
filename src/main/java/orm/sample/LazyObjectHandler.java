package orm.sample;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class LazyObjectHandler implements InvocationHandler {

    private final Supplier<?> lazy;
    private final Consumer<Object> callSetMethod;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object o = lazy.get();
        callSetMethod.accept(o);
        return method.invoke(o, args);
    }
}
