package com.ssafy.e206.configuration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import com.ssafy.e206.util.GetAnnotationData;
import com.ssafy.e206.util.ResponseAttribute;

@Component
@SuppressWarnings({ "null", "unchecked" })
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErrorShiftAttributes implements ImportAware, ErrorAttributes, HandlerExceptionResolver, Ordered {
  private static final String ERROR_INTERNAL_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";
  private AnnotationAttributes[] annotationAttributes;
  private AnnotationAttributes annotationAttribute;

  private void setAnnotationAttributes(AnnotationAttributes[] annotationAttributes) {
    this.annotationAttribute = null;
    this.annotationAttributes = annotationAttributes;
  }

  private void setAnnotationAttributes(AnnotationAttributes annotationAttribute) {
    this.annotationAttribute = annotationAttribute;
    this.annotationAttributes = null;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
      Exception ex) {
    storeErrorAttributes(request, ex);
    return null;
  }

  private void storeErrorAttributes(HttpServletRequest request, Exception ex) {
    request.setAttribute(ERROR_INTERNAL_ATTRIBUTE, ex);
  }

  @Override
  public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
    Throwable exception = getError(webRequest);
    Map<String, Object> errorAttributes = getErrorAttributes(webRequest,
        options.isIncluded(Include.STACK_TRACE));

    AnnotationAttributes myAnnotationAttributes = null;
    Class<? extends Throwable> myHandleException = null;

    if (this.annotationAttributes != null && this.annotationAttribute == null) {
      for (AnnotationAttributes annotationAttribute : this.annotationAttributes) {
        if (annotationAttribute.getClass("exception").isInstance(exception)) {
          myHandleException = annotationAttribute.getClass("exception");
          myAnnotationAttributes = annotationAttribute;
          break;
        }
      }
    } else if (this.annotationAttributes == null && this.annotationAttribute != null) {
      myHandleException = this.annotationAttribute.getClass("exception");
      myAnnotationAttributes = this.annotationAttribute;
    }

    if (myHandleException == null || myAnnotationAttributes == null) {
      return errorAttributes;
    }

    boolean prettyRes = myAnnotationAttributes.getBoolean("prettyRes");
    boolean logging = myAnnotationAttributes.getBoolean("logging");

    if (myHandleException.isInstance(exception)) {

      errorAttributes = removeErrorAttributes(errorAttributes, myAnnotationAttributes, options);
      errorAttributes = ResponseAttribute.getResponseAttribute(errorAttributes, myAnnotationAttributes, exception,
          myHandleException, prettyRes);

      if (logging) {
        errorLogging(errorAttributes, myAnnotationAttributes, myHandleException);
      }
      errorAttributes.remove("path");
      errorAttributes.remove("timestamp");
      errorAttributes.remove("message");
      errorAttributes.remove("error");
    } else {
      removeErrorAttributes(errorAttributes, webRequest, options);
    }
    return errorAttributes;
  }

  private void errorLogging(Map<String, Object> errorAttributes, AnnotationAttributes myAnnotationAttributes,
      Class<? extends Throwable> myHandleException) {
    String message = myAnnotationAttributes.getString("message");
    String language = myAnnotationAttributes.getString("language");
    Map<String, Object> details = null;
    String summary = null;

    String errorMsg = null;

    String logMessage = null;
    switch (language) {
      case "en":
        details = (Map<String, Object>) errorAttributes.get("Details");
        errorMsg = (String) details.get("Error Message");
        summary = errorAttributes.get("Summary").toString();
        logMessage = "\nsummary \t------>\t " +
            summary +
            "\nstatus \t\t------>\t "
            + errorAttributes.get("status")
            + "\nerror \t\t------>\t "
            + errorAttributes.get("error")
            + (!message.equals("") ? "\nmessage \t------>\t "
                + myAnnotationAttributes.getString("message") : "")
            + "\npath \t\t------>\t "
            + errorAttributes.get("path")
            + (errorMsg != null ? "\nerrorMessage \t------>\t "
                + errorMsg : "");
        break;
      case "ko":
        details = (Map<String, Object>) errorAttributes.get("상세");
        errorMsg = (String) details.get("에러 메시지");
        summary = errorAttributes.get("요약").toString();
        logMessage = "\n요약 \t\t------>\t " +
            summary +
            "\n상태 \t\t------>\t "
            + errorAttributes.get("status")
            + "\n에러 \t\t------>\t "
            + errorAttributes.get("error")
            + (!message.equals("") ? "\n메시지 \t------>\t "
                + myAnnotationAttributes.getString("message") : "")
            + "\n경로 \t\t------>\t "
            + errorAttributes.get("path")
            + (errorMsg != null ? "\n에러메시지 \t------>\t "
                + errorMsg : "");
        break;
    }

    Logger logger = LoggerFactory.getLogger(myHandleException);

    logger.error(logMessage);
  }

  private Map<String, Object> getErrorAttributes(WebRequest webRequest, boolean includeStackTrace) {
    Map<String, Object> errorAttributes = new LinkedHashMap<>();
    errorAttributes.put("timestamp", new Date());
    addStatus(errorAttributes, webRequest);
    addErrorDetails(errorAttributes, webRequest, includeStackTrace);
    addPath(errorAttributes, webRequest);
    return errorAttributes;
  }

  private Map<String, Object> removeErrorAttributes(Map<String, Object> errorAttributes,
      AnnotationAttributes annotationAttribute, ErrorAttributeOptions options) {
    if (!options.isIncluded(Include.EXCEPTION)) {
      errorAttributes.remove("exception");
    }
    if (!annotationAttribute.getBoolean("trace") || !options.isIncluded(Include.STACK_TRACE)) {
      errorAttributes.remove("trace");
    }
    if (!options.isIncluded(Include.MESSAGE) && errorAttributes.get("message") != null) {
      errorAttributes.remove("message");
    }
    if (!options.isIncluded(Include.BINDING_ERRORS)) {
      errorAttributes.remove("errors");
    }
    return errorAttributes;
  }

  private Map<String, Object> removeErrorAttributes(Map<String, Object> errorAttributes, WebRequest webRequest,
      ErrorAttributeOptions options) {

    if (!options.isIncluded(Include.EXCEPTION)) {
      errorAttributes.remove("exception");
    }
    if (!options.isIncluded(Include.STACK_TRACE)) {
      errorAttributes.remove("trace");
    }
    if (!options.isIncluded(Include.MESSAGE) && errorAttributes.get("message") != null) {
      errorAttributes.remove("message");
    }
    if (!options.isIncluded(Include.BINDING_ERRORS)) {
      errorAttributes.remove("errors");
    }
    return errorAttributes;
  }

  private void addStatus(Map<String, Object> errorAttributes, RequestAttributes requestAttributes) {
    Integer status = getAttribute(requestAttributes, RequestDispatcher.ERROR_STATUS_CODE);
    if (status == null) {
      errorAttributes.put("status", 999);
      errorAttributes.put("error", "None");
      return;
    }
    errorAttributes.put("status", status);
    try {
      errorAttributes.put("error", HttpStatus.valueOf(status).getReasonPhrase());
    } catch (Exception ex) {
      errorAttributes.put("error", "Http Status " + status);
    }
  }

  private void addErrorDetails(Map<String, Object> errorAttributes, WebRequest webRequest,
      boolean includeStackTrace) {
    Throwable error = getError(webRequest);
    if (error != null) {
      while (error instanceof ServletException && error.getCause() != null) {
        error = error.getCause();
      }
      errorAttributes.put("exception", error.getClass().getName());
      if (includeStackTrace) {
        addStackTrace(errorAttributes, error);
      }
    }
    addErrorMessage(errorAttributes, webRequest, error);
  }

  private void addErrorMessage(Map<String, Object> errorAttributes, WebRequest webRequest, Throwable error) {
    BindingResult result = extractBindingResult(error);
    if (result == null) {
      addExceptionErrorMessage(errorAttributes, webRequest, error);
    } else {
      addBindingResultErrorMessage(errorAttributes, result);
    }
  }

  private void addExceptionErrorMessage(Map<String, Object> errorAttributes, WebRequest webRequest, Throwable error) {
    errorAttributes.put("message", getMessage(webRequest, error));
  }

  protected String getMessage(WebRequest webRequest, Throwable error) {
    Object message = getAttribute(webRequest, RequestDispatcher.ERROR_MESSAGE);
    if (!ObjectUtils.isEmpty(message)) {
      return message.toString();
    }
    if (error != null && StringUtils.hasLength(error.getMessage())) {
      return error.getMessage();
    }
    return "No message available";
  }

  private void addBindingResultErrorMessage(Map<String, Object> errorAttributes, BindingResult result) {
    errorAttributes.put("message", "Validation failed for object='" + result.getObjectName() + "'. "
        + "Error count: " + result.getErrorCount());
    errorAttributes.put("errors", result.getAllErrors());
  }

  private BindingResult extractBindingResult(Throwable error) {
    if (error instanceof BindingResult) {
      return (BindingResult) error;
    }
    return null;
  }

  private void addStackTrace(Map<String, Object> errorAttributes, Throwable error) {
    StringWriter stackTrace = new StringWriter();
    error.printStackTrace(new PrintWriter(stackTrace));
    stackTrace.flush();
    errorAttributes.put("trace", stackTrace.toString());
  }

  private void addPath(Map<String, Object> errorAttributes, RequestAttributes requestAttributes) {
    String path = getAttribute(requestAttributes, RequestDispatcher.ERROR_REQUEST_URI);
    if (path != null) {
      errorAttributes.put("path", path);
    }
  }

  @Override
  public Throwable getError(WebRequest webRequest) {
    Throwable exception = getAttribute(webRequest, ERROR_INTERNAL_ATTRIBUTE);
    if (exception == null) {
      exception = getAttribute(webRequest, RequestDispatcher.ERROR_EXCEPTION);
    }
    webRequest.setAttribute(ErrorAttributes.ERROR_ATTRIBUTE, exception, WebRequest.SCOPE_REQUEST);
    return exception;
  }

  private <T> T getAttribute(RequestAttributes requestAttributes, String name) {
    return (T) requestAttributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
  }

  @Override
  public void setImportMetadata(AnnotationMetadata importMetadata) {
    AnnotationAttributes[] annotationAttributes = GetAnnotationData.getAnnotations(importMetadata);
    if (annotationAttributes == null) {
      setAnnotationAttributes(GetAnnotationData.getAnnotation(importMetadata));
    } else {
      setAnnotationAttributes(annotationAttributes);
    }
  }
}
