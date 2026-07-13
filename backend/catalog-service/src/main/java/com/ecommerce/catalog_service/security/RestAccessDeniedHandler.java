package com.ecommerce.catalog_service.security;
import com.ecommerce.catalog_service.exception.ErrorCode;
import com.ecommerce.catalog_service.service.AuditOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.*;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;import java.time.Instant;import java.util.Map;
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
 private final ObjectMapper mapper; private final AuditOutboxService audit;
 public RestAccessDeniedHandler(ObjectMapper mapper,AuditOutboxService audit){this.mapper=mapper;this.audit=audit;}
 public void handle(HttpServletRequest req,HttpServletResponse res,AccessDeniedException ex)throws IOException{Authentication a=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();try{audit.enqueueRequiresNew(a!=null&&a.getPrincipal() instanceof Long l?l:null,a!=null&&a.getDetails() instanceof String s?s:null,"ACCESS_DENIED","HTTP",req.getRequestURI(),"FAILURE",req.getRemoteAddr(),req.getMethod());}catch(RuntimeException ignored){}res.setStatus(403);res.setContentType(MediaType.APPLICATION_JSON_VALUE);res.setCharacterEncoding("UTF-8");mapper.writeValue(res.getOutputStream(),new SecurityError(Instant.now(),403,"Forbidden",ErrorCode.ACCESS_DENIED.name(),"No tiene permisos para realizar esta operación",req.getRequestURI(),MDC.get("correlationId"),Map.of()));}
 private record SecurityError(Instant timestamp,int status,String error,String code,String message,String path,String correlationId,Map<String,String> validationErrors){}
}
