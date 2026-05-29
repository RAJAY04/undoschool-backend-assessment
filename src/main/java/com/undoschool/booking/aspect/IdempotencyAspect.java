package com.undoschool.booking.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undoschool.booking.dto.response.IdempotencyResultResponse;
import com.undoschool.booking.enums.IdempotencyStatus;
import com.undoschool.booking.exception.IdempotencyConflictException;
import com.undoschool.booking.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.undoschool.booking.annotation.Idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String key = request.getHeader("Idempotency-Key");

        if (key == null || key.isBlank()) {
            return joinPoint.proceed();
        }

        IdempotencyResultResponse result = idempotencyService.startRequest(key);

        if (IdempotencyStatus.PENDING == result.state()) {
            throw new IdempotencyConflictException("An identical request is currently in progress. Please retry shortly.");
        }

        if (IdempotencyStatus.SUCCESS == result.state()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Class<?> targetClass = getGenericReturnType(signature);

            Object deserializedBody = objectMapper.readValue(result.responseBody(), targetClass);

            return ResponseEntity
                    .status(result.responseStatus())
                    .header("X-Cache-Idempotency", "true")
                    .body(deserializedBody);
        }

        try {
            Object responseObj = joinPoint.proceed();

            if (responseObj instanceof ResponseEntity<?> responseEntity) {
                HttpStatusCode status = responseEntity.getStatusCode();
                if (status.is2xxSuccessful()) {
                    idempotencyService.completeRequest(key, status.value(), responseEntity.getBody());
                } else {
                    idempotencyService.failRequest(key);
                }
            } else {
                idempotencyService.completeRequest(key, 200, responseObj);
            }

            return responseObj;
        } catch (Throwable t) {
            idempotencyService.failRequest(key);
            throw t;
        }
    }

    private Class<?> getGenericReturnType(MethodSignature signature) {
        Method method = signature.getMethod();
        ResolvableType resolvableType = ResolvableType.forMethodReturnType(method);
        if (ResponseEntity.class.isAssignableFrom(resolvableType.resolve(Object.class))) {
            Class<?> genericType = resolvableType.getGeneric(0).resolve();
            if (genericType != null) {
                return genericType;
            }
        }
        return resolvableType.resolve(method.getReturnType());
    }
}
