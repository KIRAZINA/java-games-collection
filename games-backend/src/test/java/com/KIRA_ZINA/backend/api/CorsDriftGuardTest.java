package com.KIRA_ZINA.backend.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Step 5b Task 1 - CORS drift guard")
@SpringBootTest(properties = "logging.level.root=WARN")
@AutoConfigureMockMvc(addFilters = true)
class CorsDriftGuardTest {

    private static final Set<String> STANDARD_BROWSER_HEADERS = Set.of(
            "content-type", "accept", "origin", "referer", "user-agent", "host");

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("every header read via @RequestHeader by a controller is in the CORS allow-list")
    void everyControllerRequestHeaderIsInCorsAllowList() throws Exception {
        Map<String, String> required = collectRequiredHeaders();
        assertFalse(required.isEmpty(),
                "collector found no @RequestHeader parameters - reflection is broken, not the CORS list");

        String allowHeaders = preflightAllowHeaders();
        assertNotNull(allowHeaders,
                "RateLimitFilter emitted no Access-Control-Allow-Headers for an OPTIONS preflight");

        Set<String> allowed = Arrays.stream(allowHeaders.split(","))
                .map(String::trim)
                .map(h -> h.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());

        List<String> drifts = new ArrayList<>();
        for (Map.Entry<String, String> e : new TreeMap<>(required).entrySet()) {
            if (!allowed.contains(e.getKey())) {
                drifts.add("'" + e.getKey() + "' read by " + e.getValue());
            }
        }
        assertTrue(drifts.isEmpty(),
                "CORS drift - header(s) missing from Access-Control-Allow-Headers: " + drifts);
    }

    private Map<String, String> collectRequiredHeaders() {
        Map<String, String> required = new LinkedHashMap<>();
        for (Class<?> controller : controllerClasses()) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!isMappingMethod(method)) {
                    continue;
                }
                for (Parameter parameter : method.getParameters()) {
                    RequestHeader requestHeader = parameter.getAnnotation(RequestHeader.class);
                    if (requestHeader == null) {
                        continue;
                    }
                    String name = !requestHeader.value().isEmpty()
                            ? requestHeader.value() : requestHeader.name();
                    if (name.isEmpty() || STANDARD_BROWSER_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                        continue;
                    }
                    required.putIfAbsent(name.toLowerCase(Locale.ROOT),
                            controller.getSimpleName() + "." + method.getName() + "(...)");
                }
            }
        }
        return required;
    }

    private List<Class<?>> controllerClasses() {
        List<Class<?>> controllers = new ArrayList<>();
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Class<?> beanClass = applicationContext.getType(beanName);
            if (beanClass == null) {
                continue;
            }
            Class<?> userClass = AopUtils.getTargetClass(applicationContext.getBean(beanName));
            if (userClass.isAnnotationPresent(RestController.class)
                    || userClass.isAnnotationPresent(Controller.class)) {
                if (!controllers.contains(userClass)) {
                    controllers.add(userClass);
                }
            }
        }
        return controllers;
    }

    private boolean isMappingMethod(Method method) {
        return method.isAnnotationPresent(RequestMapping.class)
                || method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class)
                || method.isAnnotationPresent(PatchMapping.class);
    }

    private String preflightAllowHeaders() throws Exception {
        var result = mockMvc.perform(options("/api/rooms")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "x-player-token, idempotency-key"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getHeader("Access-Control-Allow-Headers");
    }
}
