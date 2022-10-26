package com.ssafy.e206.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.ssafy.e206.configuration.TestAnnotationSelector;
import com.ssafy.e206.errorCode.ErrorCode;

@Repeatable(TestAnnotations.class)
@ControllerAdvice
public @interface TestAnnotation {
  Class<? extends Throwable> exception();

  ErrorCode errorCode() default ErrorCode.INTERNAL_SERVER_ERROR;

  String message() default "Internal Server Error";

  int status() default 500;
}