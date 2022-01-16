package com.p2p.fileshare.dto;

import java.util.function.Function;

public class MethodInvokeHelper
{
    private final String methodName;
    private final Class<?>[] params;
    private final Function<String, Integer> parser;

    public MethodInvokeHelper(String methodName, Class<?>[] params, Function<String, Integer> parser)
    {
        this.methodName = methodName;
        this.params = params;
        this.parser = parser;
    }

    public String getMethodName()
    {
        return methodName;
    }

    public Class<?>[] getParams()
    {
        return params;
    }

    public Function<String, Integer> getParser()
    {
        return parser;
    }
}
