package com.microsoft.azure.webjobs.script.handler;

import java.util.*;

import com.microsoft.azure.webjobs.script.broker.*;
import com.microsoft.azure.webjobs.script.rpc.messages.*;

public class InvocationRequestHandler extends MessageHandler<InvocationRequest, InvocationResponse.Builder> {
    public InvocationRequestHandler(JavaFunctionBroker broker) {
        super(StreamingMessage::getInvocationRequest,
              InvocationResponse::newBuilder,
              InvocationResponse.Builder::setResult,
              StreamingMessage.Builder::setInvocationResponse);
        this.broker = broker;
    }

    @Override
    String execute(InvocationRequest request, InvocationResponse.Builder response) throws Exception {
        final String functionId = request.getFunctionId();
        final String invocationId = request.getInvocationId();
        List<ParameterBinding> outputBindings = this.broker.invokeMethod(functionId, invocationId, request.getInputDataList());
        response.setInvocationId(invocationId).addAllOutputData(outputBindings);
        return "Function \"" + functionId + "\" executed";
    }

    private JavaFunctionBroker broker;
}