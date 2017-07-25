package com.microsoft.azure.webjobs.script.broker;

import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import javax.annotation.*;

import com.microsoft.azure.webjobs.script.binding.*;

class JavaMethodExecutor {
    JavaMethodExecutor(String jar, String fullMethodName)
            throws MalformedURLException, ClassNotFoundException, IllegalAccessException {
        this.jarPath = jar;
        this.overloadResolver = new OverloadResolver();
        this.splitFullMethodName(fullMethodName);
        this.retrieveCandidates();
    }

    void execute(InputDataStore inputs, OutputDataStore outputs) throws Exception {
        this.overloadResolver.resolve(inputs, outputs)
            .orElseThrow(() -> new NoSuchMethodException("Cannot locate the method signature with the given input"))
            .invoke(() -> this.containingClass.newInstance(), outputs);
    }

    @PostConstruct
    private void splitFullMethodName(String fullMethodName) {
        int lastPeriodIndex = fullMethodName.lastIndexOf('.');
        if (lastPeriodIndex < 0) {
            throw new IllegalArgumentException("\"" + fullMethodName + "\" is not a qualified method name");
        }
        this.fullClassName = fullMethodName.substring(0, lastPeriodIndex);
        this.methodName = fullMethodName.substring(lastPeriodIndex + 1);
        if (this.fullClassName.isEmpty() || this.methodName.isEmpty()) {
            throw new IllegalArgumentException("\"" + fullMethodName + "\" is not a qualified method name");
        }
    }

    @PostConstruct
    private void retrieveCandidates() throws MalformedURLException, ClassNotFoundException, IllegalAccessException {
        URL jarUrl = Paths.get(this.jarPath).toUri().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[] { jarUrl });
        this.containingClass = Class.forName(this.fullClassName, true, classLoader);
        for (Method method : this.containingClass.getMethods()) {
            if (method.getName().equals(this.methodName)) {
                this.overloadResolver.addCandidate(method);
            }
        }
    }

    private String jarPath;
    private String fullClassName;
    private String methodName;
    private Class<?> containingClass;
    private OverloadResolver overloadResolver;
}