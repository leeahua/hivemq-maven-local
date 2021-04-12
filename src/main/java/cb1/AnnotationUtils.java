package cb1;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public class AnnotationUtils {

    public static <T extends Annotation> Optional<T> getAnnotation(Method method, Class<T> type) {
        Class[] interfaces = method.getDeclaringClass().getInterfaces();
        return Arrays.stream(interfaces)
                .flatMap(interfaceType -> Arrays.stream(interfaceType.getMethods()))
                .filter(currentMethod -> same(method, currentMethod))
                .map(currentMethod -> currentMethod.getAnnotation(type))
                .findFirst();
    }

    private static boolean same(Method method1, Method method2) {
        return method1.getReturnType().equals(method2.getReturnType()) &&
                method1.getName().equals(method2.getName()) &&
                Arrays.equals(method1.getParameterTypes(), method2.getParameterTypes());
    }
}
