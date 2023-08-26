package rinha.models.valitador;

import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StackValidator implements ConstraintValidator<Stack, List<String>> {

    @Override
    public boolean isValid(List<String> stacks, ConstraintValidatorContext context) {
        if(stacks != null) {
            for (String stack : stacks) {
                if(stack != null && stack.length() > 32) {
                    return false;
                }
            }
        }
        
        return true;
    }


}
