package com.logmonitor.audit;

import com.logmonitor.util.SecurityUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * AOP aspect that records audit entries for methods annotated with {@link Audited}.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final String SYSTEM_USER = "SYSTEM";

    private final AuditService auditService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Records an audit entry after a successfully completed {@link Audited} method.
     *
     * @param joinPoint the intercepted join point
     * @param audited   the audit annotation metadata
     */
    @AfterReturning("@annotation(audited)")
    public void afterAuditedMethod(JoinPoint joinPoint, Audited audited) {
        try {
            String username = SecurityUtils.getCurrentUsernameOrDefault(SYSTEM_USER);
            String resource = resolveResource(joinPoint, audited);
            auditService.log(username, audited.action(), resource);
        } catch (Exception ex) {
            log.error("Failed to record audit for method {}: {}",
                    joinPoint.getSignature().toShortString(), ex.getMessage());
        }
    }

    private String resolveResource(JoinPoint joinPoint, Audited audited) {
        if (!StringUtils.hasText(audited.resourceSpel())) {
            return audited.action().getDescription();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        context.setVariable("method", method.getName());

        Object value = expressionParser.parseExpression(audited.resourceSpel()).getValue(context);
        return value != null ? value.toString() : audited.action().getDescription();
    }
}
