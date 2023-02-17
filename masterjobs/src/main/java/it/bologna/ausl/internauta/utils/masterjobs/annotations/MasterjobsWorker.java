package it.bologna.ausl.internauta.utils.masterjobs.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 
 * @author gdm
 * 
 * Annotazione che identifica un Worker (sia un executor che un service)
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public @interface MasterjobsWorker {
    public String[] value() default {};
}