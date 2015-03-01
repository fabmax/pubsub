package de.fabmax.pubsub.util;

import java.lang.annotation.*;

/**
 * Created by Max on 25.02.2015.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Inherited
public @interface EndpointParameter {

    String name();

}
