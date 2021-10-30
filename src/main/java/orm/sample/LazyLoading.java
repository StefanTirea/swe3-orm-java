package orm.sample;

import lombok.RequiredArgsConstructor;
import net.sf.cglib.proxy.LazyLoader;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class LazyLoading<T> implements LazyLoader {

    private final Supplier<T> lazy;

    @Override
    public Object loadObject() {
        return lazy.get();
    }
}
