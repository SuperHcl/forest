package com.dtflys.forest.lifecycles.method;

import com.dtflys.forest.annotation.JSONPathResult;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.dtflys.forest.interceptor.ResponseResult;
import com.dtflys.forest.lifecycles.MethodAnnotationLifeCycle;
import com.dtflys.forest.mapping.MappingTemplate;
import com.dtflys.forest.reflection.ForestMethod;

import java.lang.reflect.Type;
import java.util.Optional;

public class JSONPathResultLifeCycle implements MethodAnnotationLifeCycle<JSONPathResult, Object> {
    
    @Override
    public void onMethodInitialized(ForestMethod method, JSONPathResult annotation) {
        
    }

    @Override
    public ResponseResult onResponse(ForestRequest request, ForestResponse response) {
        try {
            if (response.isError()) {
                return ResponseResult.error(response.getException());
            }
            final String pathStr = Optional.ofNullable(getAttributeAsString(request, "path")).orElse("");
            final MappingTemplate pathTemplate = request.getMethod().makeTemplate(JSONPathResult.class, "path", pathStr);
            final String path = pathTemplate.render(request.getArguments());
            final Type type = request.getMethod().getResultType();
            return ResponseResult.success(response.getByPath(path, type));
        } catch (Throwable th) {
            return ResponseResult.error(th);
        }
    }
}
