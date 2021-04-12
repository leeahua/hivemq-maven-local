package at;

import cb1.AnnotationUtils;
import com.hivemq.spi.services.configuration.validation.ValidationError;
import com.hivemq.spi.services.configuration.validation.Validator;
import com.hivemq.spi.services.configuration.validation.annotation.Validate;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MethodValidateInterceptor implements MethodInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodValidateInterceptor.class);

    public Object invoke(MethodInvocation invocation) throws Throwable {
        Optional<Validate> mayValidate = AnnotationUtils.getAnnotation(invocation.getMethod(), Validate.class);
        if (!mayValidate.isPresent()) {
            LOGGER.warn("{} does not have a @Validate annotation in the interface definition. Proceeding without validation", invocation.getMethod().toString());
            return invocation.proceed();
        }
        Validator validator = newInstance(mayValidate);
        Object[] arguments = invocation.getArguments();
        if (arguments.length != 1) {
            LOGGER.warn("Validators are only applicable for methods with 1 argument. The method {} has {} arguments. Ignoring validation",
                    invocation.getMethod().toString(), arguments.length);
            return invocation.proceed();
        }
        Object argument = arguments[0];
        List<ValidationError> validationErrors = Collections.emptyList();
        try {
            String validateName = mayValidate.get().name();
            if (validateName.trim().isEmpty()) {
                validateName = invocation.getMethod().toGenericString();
            }
            validationErrors = validator.validate(argument, validateName);
        } catch (Exception localException) {
            LOGGER.error("Validator {} threw an exception. Ignoring validation results.", validator.getClass().getCanonicalName(), localException);
        }
        if (validationErrors.isEmpty()) {
            return invocation.proceed();
        }
        validationErrors.forEach(validationError ->
                LOGGER.error(validationError.getMessage())
        );
        return null;
    }

    private Validator newInstance(Optional<Validate> mayValidate) throws IllegalAccessException, InstantiationException {
        Validate validate = mayValidate.get();
        Class<? extends Validator> validator = validate.value();
        return validator.newInstance();
    }
}
