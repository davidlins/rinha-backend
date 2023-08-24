package rinha.models.valitador;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = StackValidator.class)
@Target({ FIELD })
@Retention(RUNTIME)
@Documented
public @interface Stack {

    String message() default "Stack[x] dever ser menor que 32 caracters.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
