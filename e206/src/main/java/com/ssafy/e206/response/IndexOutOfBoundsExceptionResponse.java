package com.ssafy.e206.response;

import java.util.HashMap;
import java.util.Map;

public class IndexOutOfBoundsExceptionResponse{

    private final Map<String, Object> details;

    public Map<String, Object> getDetails() {
        return details;
    }

    private IndexOutOfBoundsExceptionResponse(final Map<String, Object> map) {
        this.details = map;
    }


    public static IndexOutOfBoundsExceptionResponse of(final IndexOutOfBoundsException e) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("errorMessage", e.getMessage());
        map.put("location", new HashMap<String, Object>() {
            {
                put("fileName", e.getStackTrace()[0].getFileName());
                put("className", e.getStackTrace()[0].getClassName());
                put("lineNumber", e.getStackTrace()[0].getLineNumber());
                put("methodName", e.getStackTrace()[0].getMethodName());
            }
        });

        map.put("stackTrace", e.getStackTrace());
        return new IndexOutOfBoundsExceptionResponse(map);
    }

}

