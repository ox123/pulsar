/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.broker.admin.impl;

import io.swagger.annotations.*;
import org.apache.pulsar.broker.admin.AdminResource;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.common.functions.FunctionConfig;
import org.apache.pulsar.common.functions.FunctionState;
import org.apache.pulsar.common.functions.UpdateOptions;
import org.apache.pulsar.common.io.ConnectorDefinition;
import org.apache.pulsar.common.policies.data.FunctionStats;
import org.apache.pulsar.common.policies.data.FunctionStatus;
import org.apache.pulsar.functions.worker.WorkerService;
import org.apache.pulsar.functions.worker.rest.api.FunctionsImpl;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

public class FunctionsBase extends AdminResource implements Supplier<WorkerService> {

    private final FunctionsImpl functions;

    public FunctionsBase() {
        this.functions = new FunctionsImpl(this);
    }

    @Override
    public WorkerService get() {
        return pulsar().getWorkerService();
    }

    @POST
    @ApiOperation(value = "Creates a new Pulsar Function in cluster mode")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 400, message = "Invalid request (function already exists, etc.)"),
            @ApiResponse(code = 408, message = "Request timeout"),
            @ApiResponse(code = 200, message = "Pulsar Function successfully created")
    })
    @Path("/{tenant}/{namespace}/{functionName}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void registerFunction(
            @ApiParam(value = "The functions tenant")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The functions namespace")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The functions name")
            final @PathParam("functionName") String functionName,
            final @FormDataParam("data") InputStream uploadedInputStream,
            final @FormDataParam("data") FormDataContentDisposition fileDetail,
            final @FormDataParam("url") String functionPkgUrl,
            @ApiParam(
                    value = "A JSON value presenting functions configuration payload. An example of the expected functions can be found here.  \n" +
                            "autoAck  \n" +
                            "  Whether or not the framework will automatically acknowledge messages.  \n" +
                            "runtime  \n" +
                            "  What is the runtime of the function. Possible Values: [JAVA, PYTHON, GO]  \n" +
                            "resources  \n" +
                            "  The size of the system resources allowed by the function runtime. The resources include: cpu, ram, disk.  \n" +
                            "className  \n" +
                            "  The class name of functions.  \n" +
                            "tenant  \n" +
                            "  The tenant of functions.  \n" +
                            "namespace  \n" +
                            "  The namespace of functions.  \n" +
                            "name  \n" +
                            "  The name of functions.  \n" +
                            "customSchemaInputs  \n" +
                            "  The map of input topics to Schema class names (The customSchemaInputs is JSON string).  \n" +
                            "customSerdeInputs  \n" +
                            "  The map of input topics to SerDe class names (The customSerdeInputs is JSON string).  \n" +
                            "deadLetterTopic  \n" +
                            "  Messages that are not processed successfully are sent to `deadLetterTopic`.  \n" +
                            "runtimeFlags  \n" +
                            "  Any flags that you want to pass to the runtime. Note that in thread mode, these flags have no impact.  \n" +
                            "fqfn  \n" +
                            "  The Fully Qualified Function Name (FQFN) for the function.  \n" +
                            "inputSpecs  \n" +
                            "  A generalized way of specifying inputs.  \n" +
                            "inputs  \n" +
                            "  The input topic or topics (multiple topics can be specified as a comma-separated list) of functions.  \n" +
                            "jar  \n" +
                            "  Path to the JAR file for the function (if the function is written in Java). " +
                            "  It also supports URL path [http/https/file (file protocol assumes that file " +
                            "  already exists on worker host)] from which worker can download the package.  \n" +
                            "py  \n" +
                            "  Path to the main Python file/Python Wheel file for the function (if the function is written in Python).  \n" +
                            "go  \n" +
                            "  Path to the main Go executable binary for the function (if the function is written in Go).  \n" +
                            "logTopic  \n" +
                            "  The topic to which the functions logs are produced.  \n" +
                            "maxMessageRetries  \n" +
                            "  How many times should we try to process a message before giving up.  \n" +
                            "output  \n" +
                            "  The functions output topic (If none is specified, no output is written).  \n" +
                            "outputSerdeClassName  \n" +
                            "  The SerDe class to be used for messages output by the function.  \n" +
                            "parallelism  \n" +
                            "  The functions parallelism factor (i.e. the number of function instances to run).  \n" +
                            "processingGuarantees  \n" +
                            "  The processing guarantees (that is, delivery semantics) applied to the function." +
                            "  Possible Values: [ATLEAST_ONCE, ATMOST_ONCE, EFFECTIVELY_ONCE]  \n" +
                            "retainOrdering  \n" +
                            "  Function consumes and processes messages in order.  \n" +
                            "outputSchemaType  \n" +
                            "   Represents either a builtin schema type (for example: 'avro', 'json', ect) or the class name for a Schema implementation." +
                            "subName  \n" +
                            "  Pulsar source subscription name. User can specify a subscription-name for the input-topic consumer.  \n" +
                            "windowConfig  \n" +
                            "  The window functions configuration.  \n" +
                            "timeoutMs  \n" +
                            "  The message timeout in milliseconds.  \n" +
                            "topicsPattern  \n" +
                            "  The topic pattern to consume from a list of topics under a namespace that match the pattern." +
                            "  [--input] and [--topic-pattern] are mutually exclusive. Add SerDe class name for a " +
                            "  pattern in --custom-serde-inputs (supported for java fun only)  \n" +
                            "userConfig  \n" +
                            "  User-defined config key/values  \n" +
                            "secrets  \n" +
                            "  This is a map of secretName(that is how the secret is going to be accessed in the function via context) to an object that" +
                            "  encapsulates how the secret is fetched by the underlying secrets provider. The type of an value here can be found by the" +
                            "  SecretProviderConfigurator.getSecretObjectType() method. \n" +
                            "cleanupSubscription  \n" +
                            "  Whether the subscriptions the functions created/used should be deleted when the functions are deleted.  \n",
                    examples = @Example(
                            value = @ExampleProperty(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    value = "{\n"
                                            + "  \"inputs\": persistent://public/default/input-topic,\n"
                                            + "  \"parallelism\": 4\n"
                                            + "  \"output\": persistent://public/default/output-topic\n"
                                            + "  \"log-topic\": persistent://public/default/log-topic\n"
                                            + "  \"classname\": org.example.test.ExclamationFunction\n"
                                            + "  \"jar\": java-function-1.0-SNAPSHOT.jar\n"
                                            + "}\n"
                            )
                    )
            )
            final @FormDataParam("functionConfig") String functionConfigJson) {

        functions.registerFunction(tenant, namespace, functionName, uploadedInputStream, fileDetail,
            functionPkgUrl, functionConfigJson, clientAppId(), clientAuthData());
    }

    @PUT
    @ApiOperation(value = "Updates a Pulsar Function currently running in cluster mode")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 400, message = "Invalid request (function doesn't exist, etc.)"),
            @ApiResponse(code = 200, message = "Pulsar Function successfully updated")
    })
    @Path("/{tenant}/{namespace}/{functionName}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void updateFunction(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName,
            final @FormDataParam("data") InputStream uploadedInputStream,
            final @FormDataParam("data") FormDataContentDisposition fileDetail,
            final @FormDataParam("url") String functionPkgUrl,
            @ApiParam(
                    value = "A JSON value presenting a functions config playload. An example of the expected functions can be found down here.  \n" +
                            "autoAck  \n" +
                            "  Whether or not the framework will automatically acknowledge messages.  \n" +
                            "runtime  \n" +
                            "  What is the runtime of the function. Possible Values: [JAVA, PYTHON, GO]  \n" +
                            "resources  \n" +
                            "  The size of the system resources allowed by the function runtime. The resources include: cpu, ram, disk.  \n" +
                            "className  \n" +
                            "  The class name of functions.  \n" +
                            "tenant  \n" +
                            "  The tenant of functions.  \n" +
                            "namespace  \n" +
                            "  The namespace of functions.  \n" +
                            "name  \n" +
                            "  The name of functions.  \n" +
                            "customSchemaInputs  \n" +
                            "  The map of input topics to Schema class names (The customSchemaInputs is JSON string).  \n" +
                            "customSerdeInputs  \n" +
                            "  The map of input topics to SerDe class names (The customSerdeInputs is JSON string).  \n" +
                            "deadLetterTopic  \n" +
                            "  Messages that are not processed successfully are sent to `deadLetterTopic`.  \n" +
                            "runtimeFlags  \n" +
                            "  Any flags that you want to pass to the runtime. Note that in thread mode, these flags have no impact.  \n" +
                            "fqfn  \n" +
                            "  The Fully Qualified Function Name (FQFN) for the function.  \n" +
                            "inputSpecs  \n" +
                            "  A generalized way of specifying inputs.  \n" +
                            "inputs  \n" +
                            "  The input topic or topics (multiple topics can be specified as a comma-separated list) of functions.  \n" +
                            "jar  \n" +
                            "  Path to the JAR file for the function (if the function is written in Java). " +
                            "  It also supports URL path [http/https/file (file protocol assumes that file " +
                            "  already exists on worker host)] from which worker can download the package.  \n" +
                            "py  \n" +
                            "  Path to the main Python file/Python Wheel file for the function (if the function is written in Python).  \n" +
                            "go  \n" +
                            "  Path to the main Go executable binary for the function (if the function is written in Go).  \n" +
                            "logTopic  \n" +
                            "  The topic to which the functions logs are produced.  \n" +
                            "maxMessageRetries  \n" +
                            "  How many times should we try to process a message before giving up.  \n" +
                            "output  \n" +
                            "  The functions output topic (If none is specified, no output is written).  \n" +
                            "outputSerdeClassName  \n" +
                            "  The SerDe class to be used for messages output by the function.  \n" +
                            "parallelism  \n" +
                            "  The functions parallelism factor (i.e. the number of function instances to run).  \n" +
                            "processingGuarantees  \n" +
                            "  The processing guarantees (that is, delivery semantics) applied to the function." +
                            "  Possible Values: [ATLEAST_ONCE, ATMOST_ONCE, EFFECTIVELY_ONCE]  \n" +
                            "retainOrdering  \n" +
                            "  Function consumes and processes messages in order.  \n" +
                            "outputSchemaType  \n" +
                            "   Represents either a builtin schema type (for example: 'avro', 'json', ect) or the class name for a Schema implementation." +
                            "subName  \n" +
                            "  Pulsar source subscription name. User can specify a subscription-name for the input-topic consumer.  \n" +
                            "windowConfig  \n" +
                            "  The window functions configuration.  \n" +
                            "timeoutMs  \n" +
                            "  The message timeout in milliseconds.  \n" +
                            "topicsPattern  \n" +
                            "  The topic pattern to consume from a list of topics under a namespace that match the pattern." +
                            "  [--input] and [--topic-pattern] are mutually exclusive. Add SerDe class name for a " +
                            "  pattern in --custom-serde-inputs (supported for java fun only)  \n" +
                            "userConfig  \n" +
                            "  User-defined config key/values  \n" +
                            "secrets  \n" +
                            "  This is a map of secretName(that is how the secret is going to be accessed in the function via context) to an object that" +
                            "  encapsulates how the secret is fetched by the underlying secrets provider. The type of an value here can be found by the" +
                            "  SecretProviderConfigurator.getSecretObjectType() method. \n" +
                            "cleanupSubscription  \n" +
                            "  Whether the subscriptions the functions created/used should be deleted when the functions are deleted.  \n",
                    examples = @Example(
                            value = @ExampleProperty(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    value = "{\n"
                                            + "  \"inputs\": persistent://public/default/input-topic,\n"
                                            + "  \"parallelism\": 4\n"
                                            + "  \"output\": persistent://public/default/output-topic\n"
                                            + "  \"log-topic\": persistent://public/default/log-topic\n"
                                            + "  \"classname\": org.example.test.ExclamationFunction\n"
                                            + "  \"jar\": java-function-1.0-SNAPSHOT.jar\n"
                                            + "}\n"
                            )
                    )
            )
            final @FormDataParam("functionConfig") String functionConfigJson,
            @ApiParam(value = "The update options is for the Pulsar Function that needs to be updated.")
            final @FormDataParam("updateOptions") UpdateOptions updateOptions) throws IOException {

        functions.updateFunction(tenant, namespace, functionName, uploadedInputStream, fileDetail,
                functionPkgUrl, functionConfigJson, clientAppId(), clientAuthData(), updateOptions);
    }


    @DELETE
    @ApiOperation(value = "Deletes a Pulsar Function currently running in cluster mode")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "The function doesn't exist"),
            @ApiResponse(code = 408, message = "Request timeout"),
            @ApiResponse(code = 200, message = "The function was successfully deleted")
    })
    @Path("/{tenant}/{namespace}/{functionName}")
    public void deregisterFunction(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName) {
        functions.deregisterFunction(tenant, namespace, functionName, clientAppId(), clientAuthData());
    }

    @GET
    @ApiOperation(
            value = "Fetches information about a Pulsar Function currently running in cluster mode",
            response = FunctionConfig.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 408, message = "Request timeout"),
            @ApiResponse(code = 404, message = "The function doesn't exist")
    })
    @Path("/{tenant}/{namespace}/{functionName}")
    public FunctionConfig getFunctionInfo(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName) throws IOException {
        return functions.getFunctionInfo(tenant, namespace, functionName, clientAppId(), clientAuthData());
    }

    @GET
    @ApiOperation(
            value = "Displays the status of a Pulsar Function instance",
            response = FunctionStatus.FunctionInstanceStatus.FunctionInstanceStatusData.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 404, message = "The function doesn't exist")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{tenant}/{namespace}/{functionName}/{instanceId}/status")
    public FunctionStatus.FunctionInstanceStatus.FunctionInstanceStatusData getFunctionInstanceStatus(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName,
            @ApiParam(value = "The function instanceId (if instance-id is not provided, the stats of all instances is returned")
            final @PathParam("instanceId") String instanceId) throws IOException {
        return functions.getFunctionInstanceStatus(tenant, namespace, functionName, instanceId, uri.getRequestUri(), clientAppId(), clientAuthData());
    }

    @GET
    @ApiOperation(
            value = "Displays the status of a Pulsar Function",
            response = FunctionStatus.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 404, message = "The function doesn't exist")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{tenant}/{namespace}/{functionName}/status")
    public FunctionStatus getFunctionStatus(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName) throws IOException {
        return functions.getFunctionStatus(tenant, namespace, functionName, uri.getRequestUri(), clientAppId(), clientAuthData());
    }

    @GET
    @ApiOperation(
            value = "Displays the stats of a Pulsar Function",
            response = FunctionStats.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 404, message = "The function doesn't exist")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{tenant}/{namespace}/{functionName}/stats")
    public FunctionStats getFunctionStats(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName) throws IOException {
        return functions.getFunctionStats(tenant, namespace, functionName, uri.getRequestUri(), clientAppId(), clientAuthData());
    }

    @GET
    @ApiOperation(
            value = "Displays the stats of a Pulsar Function instance",
            response = FunctionStats.FunctionInstanceStats.FunctionInstanceStatsData.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 404, message = "The function doesn't exist")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{tenant}/{namespace}/{functionName}/{instanceId}/stats")
    public FunctionStats.FunctionInstanceStats.FunctionInstanceStatsData getFunctionInstanceStats(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName,
            @ApiParam(value = "The function instanceId (if instance-id is not provided, the stats of all instances is returned")
            final @PathParam("instanceId") String instanceId) throws IOException {
        return functions.getFunctionsInstanceStats(tenant, namespace, functionName, instanceId, uri.getRequestUri(), clientAppId(), clientAuthData());
    }

    @GET
    @ApiOperation(
            value = "Lists all Pulsar Functions currently deployed in a given namespace",
            response = String.class,
            responseContainer = "Collection"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions")
    })
    @Path("/{tenant}/{namespace}")
    public List<String> listFunctions(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace) {
        return functions.listFunctions(tenant, namespace, clientAppId(), clientAuthData());
    }

    @POST
    @ApiOperation(
            value = "Triggers a Pulsar Function with a user-specified value or file data",
            response = Message.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "The function does not exist"),
            @ApiResponse(code = 408, message = "Request timeout"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{tenant}/{namespace}/{functionName}/trigger")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String triggerFunction(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName,
            @ApiParam(value = "The value with which you want to trigger the function")
            final @FormDataParam("data") String triggerValue,
            @ApiParam(value = "The path to the file that contains the data with which you'd like to trigger the function")
            final @FormDataParam("dataStream") InputStream triggerStream,
            @ApiParam(value = "The specific topic name that the function consumes from which you want to inject the data to")
            final @FormDataParam("topic") String topic) {
        return functions.triggerFunction(tenant, namespace, functionName, triggerValue, triggerStream, topic, clientAppId(), clientAuthData());
    }

    @GET
    @ApiOperation(
        value = "Fetch the current state associated with a Pulsar Function",
        response = FunctionState.class
    )
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid request"),
        @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
        @ApiResponse(code = 404, message = "The key does not exist"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{tenant}/{namespace}/{functionName}/state/{key}")
    public FunctionState getFunctionState(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName,
            @ApiParam(value = "The stats key")
            final @PathParam("key") String key) {
        return functions.getFunctionState(tenant, namespace, functionName, key, clientAppId(), clientAuthData());
    }

    @POST
    @ApiOperation(
            value = "Put the state associated with a Pulsar Function"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 404, message = "The function does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{tenant}/{namespace}/{functionName}/state/{key}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void putFunctionState(final @PathParam("tenant") String tenant,
                                 final @PathParam("namespace") String namespace,
                                 final @PathParam("functionName") String functionName,
                                 final @PathParam("key") String key,
                                 final @FormDataParam("state") FunctionState stateJson) {
        functions.putFunctionState(tenant, namespace, functionName, key, stateJson, clientAppId(), clientAuthData());
    }

    @POST
    @ApiOperation(value = "Restart function instance", response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "The function does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{tenant}/{namespace}/{functionName}/{instanceId}/restart")
    @Consumes(MediaType.APPLICATION_JSON)
    public void restartFunction(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName,
            @ApiParam(value = "The function instanceId (if instance-id is not provided, all instances are restarted")
            final @PathParam("instanceId") String instanceId) {
        functions.restartFunctionInstance(tenant, namespace, functionName, instanceId, uri.getRequestUri(), clientAppId(), clientAuthData());
    }

    @POST
    @ApiOperation(value = "Restart all function instances", response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "The function does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{tenant}/{namespace}/{functionName}/restart")
    @Consumes(MediaType.APPLICATION_JSON)
    public void restartFunction(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName) {
        functions.restartFunctionInstances(tenant, namespace, functionName, clientAppId(), clientAuthData());
    }

    @POST
    @ApiOperation(value = "Stop function instance", response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "The function does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{tenant}/{namespace}/{functionName}/{instanceId}/stop")
    @Consumes(MediaType.APPLICATION_JSON)
    public void stopFunction(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName,
            @ApiParam(value = "The function instanceId (if instance-id is not provided, all instances are stopped. ")
            final @PathParam("instanceId") String instanceId) {
        functions.stopFunctionInstance(tenant, namespace, functionName, instanceId, uri.getRequestUri(), clientAppId(), clientAuthData());
    }

    @POST
    @ApiOperation(value = "Stop all function instances", response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "The function does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{tenant}/{namespace}/{functionName}/stop")
    @Consumes(MediaType.APPLICATION_JSON)
    public void stopFunction(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName) {
        functions.stopFunctionInstances(tenant, namespace, functionName, clientAppId(), clientAuthData());
    }

    @POST
    @ApiOperation(value = "Start function instance", response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "The function does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{tenant}/{namespace}/{functionName}/{instanceId}/start")
    @Consumes(MediaType.APPLICATION_JSON)
    public void startFunction(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName,
            @ApiParam(value = "The function instanceId (if instance-id is not provided, all instances sre started. ")
            final @PathParam("instanceId") String instanceId) {
        functions.startFunctionInstance(tenant, namespace, functionName, instanceId, uri.getRequestUri(), clientAppId(), clientAuthData());
    }

    @POST
    @ApiOperation(value = "Start all function instances", response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "The function does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{tenant}/{namespace}/{functionName}/start")
    @Consumes(MediaType.APPLICATION_JSON)
    public void startFunction(
            @ApiParam(value = "The tenant of functions")
            final @PathParam("tenant") String tenant,
            @ApiParam(value = "The namespace of functions")
            final @PathParam("namespace") String namespace,
            @ApiParam(value = "The name of functions")
            final @PathParam("functionName") String functionName) {
        functions.startFunctionInstances(tenant, namespace, functionName, clientAppId(), clientAuthData());
    }

    @POST
    @ApiOperation(
            value = "Uploads Pulsar Function file data",
            hidden = true
    )
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void uploadFunction(final @FormDataParam("data") InputStream uploadedInputStream,
                               final @FormDataParam("path") String path) {
        functions.uploadFunction(uploadedInputStream, path);
    }

    @GET
    @ApiOperation(
            value = "Downloads Pulsar Function file data",
            hidden = true
    )
    @Path("/download")
    public StreamingOutput downloadFunction(final @QueryParam("path") String path) {
        return functions.downloadFunction(path);
    }

    @GET
    @ApiOperation(
            value = "Fetches a list of supported Pulsar IO connectors currently running in cluster mode",
            response = List.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 408, message = "Request timeout")
    })
    @Path("/connectors")
    @Deprecated
    /**
     * Deprecated in favor of moving endpoint to {@link org.apache.pulsar.broker.admin.v2.Worker}
     */
    public List<ConnectorDefinition> getConnectorsList() throws IOException {
        return functions.getListOfConnectors();
    }
}
