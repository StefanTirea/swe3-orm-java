package orm;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import orm.annotation.Repository;
import orm.sample.DynamicInvocationHandler;
import orm.sample.LogEntity;
import orm.sample.UserEntity;
import orm.sql.ConnectionPool;
import orm.sql.UserRepository;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Function;

public class Main {

    public static void main(String[] args) {
        ConnectionPool connectionPool = new ConnectionPool();
        connectionPool.setConnection();

        ConfigGenerator configGenerator = new ConfigGenerator();
        UserRepository userRepository = new UserRepository();
        List<UserEntity> userEntities = userRepository.selectAll();
        List<UserEntity> ids = userRepository.select("id", "entry")
                .from(UserEntity.class)
                .join(LogEntity.class)
                .fetchAll();
        // Proxy Interface
        orm.sample.UserRepository repo = (orm.sample.UserRepository) Proxy.newProxyInstance(orm.sample.UserRepository.class.getClassLoader(), new Class[]{orm.sample.UserRepository.class}, new DynamicInvocationHandler());

        // Proxy Class
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(UserRepository.class);
        enhancer.setCallbacks(new Callback[]{
                (MethodInterceptor) (obj, method, argss, proxy) -> {
                    System.out.println("Proxy test " + method.getName());
                    return proxy.invokeSuper(obj, argss);
                },
                (MethodInterceptor) (obj, method, argss, proxy) -> {
                    System.out.println("Proxy test2 " + method.getName());
                    return proxy.invokeSuper(obj, argss);
                }
        });
        enhancer.setCallbackFilter(method -> method.isAnnotationPresent(Repository.class) || method.getDeclaringClass().isAnnotationPresent(Repository.class) ? 0 : 1);

        UserRepository proxy = (UserRepository) enhancer.create();
        proxy.selectAll();

        connectionPool.releaseConnection(false);
    }
}
