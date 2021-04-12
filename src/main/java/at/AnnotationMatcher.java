package at;

import cb1.AnnotationUtils;
import com.google.inject.matcher.AbstractMatcher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

public class AnnotationMatcher extends AbstractMatcher<Method> {
    private final Class<? extends Annotation> annotationType;

    public AnnotationMatcher(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public boolean matches(Method method) {
        Optional<? extends Annotation> annotation = AnnotationUtils.getAnnotation(method, this.annotationType);
        return annotation.isPresent();
    }
}
