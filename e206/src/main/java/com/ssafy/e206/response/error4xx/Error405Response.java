package com.ssafy.e206.response.error4xx;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import com.ssafy.e206.response.ErrorResponse;

public class Error405Response extends ErrorResponse {
  private Map<String, String> errors;

  public Map<String, String> getErrors() {
    return this.errors;
  }

  private Error405Response(final HttpStatus code, final Map<String, String> map) {
    super(code.toString(), code.value());
    this.errors = map;
  }

  public static Error405Response of(final HttpStatus code, final HttpRequestMethodNotSupportedException e) {
    return new Error405Response(code, FieldError.of(e));
  }

  public static class FieldError {
    private String message;
    private String requestedMethod;
    private String supportedMethod;

    public String getRequestURL() {
      return this.message;
    }

    public String getMethod() {
      return this.requestedMethod;
    }

    public String getSupportedMethod() {
      return this.supportedMethod;
    }

    private FieldError(final String message, final String requestedMethod, final String supportedMethod) {
      this.message = message;
      this.requestedMethod = requestedMethod;
      this.supportedMethod = supportedMethod;
    }

    private static Map<String, String> of(final HttpRequestMethodNotSupportedException e) {
      return new HashMap<String, String>() {
        {
          put("message", e.getMessage());
          put("requestedMethod", e.getMethod());
          put("supportedMethod", Arrays.toString(e.getSupportedMethods()));

          put("possibleSolution",
              "you use " + e.getMethod() + " method, but you can use " + Arrays.toString(e.getSupportedMethods())
                  + " method(s) for this request. Please check the request method and try again.");
        }
      };
    }
  }

  @Override
  public String toString() {
    return "{" + super.toString() +
        " errors='" + getErrors() + "'" +
        "}";
  }

}
