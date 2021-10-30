package orm;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import orm.annotation.Repository;
import orm.sample.LogEntity;
import orm.sample.UserEntity;
import orm.sample.UserService;
import orm.sql.ConnectionPool;
import orm.sql.Query;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        ConnectionPool connectionPool = new ConnectionPool();

        ConfigGenerator configGenerator = new ConfigGenerator();
        List<LogEntity> logEntities = Query.fetchAllFrom(LogEntity.class);
        List<UserEntity> userEntities = Query.fetchAllFrom(UserEntity.class);


        List<UserEntity> filtered = Query.where("firstname", "=", "Stefan")
                .fetchAll(UserEntity.class);

        // Proxy Interface
        // orm.sample.UserRepository repo = (orm.sample.UserRepository) Proxy.newProxyInstance(orm.sample.UserRepository.class.getClassLoader(), new Class[]{orm.sample.UserRepository.class}, new DynamicInvocationHandler());

        // Proxy Class
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(UserService.class);
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

        UserService proxy = (UserService) enhancer.create();
        proxy.fetchAll();

        connectionPool.releaseConnection(false);
    }
}
