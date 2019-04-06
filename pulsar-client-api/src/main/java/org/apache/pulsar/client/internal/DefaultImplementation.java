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
package org.apache.pulsar.client.internal;

import static org.apache.pulsar.client.internal.ReflectionUtils.catchExceptions;
import static org.apache.pulsar.client.internal.ReflectionUtils.getConstructor;
import static org.apache.pulsar.client.internal.ReflectionUtils.getStaticMethod;
import static org.apache.pulsar.client.internal.ReflectionUtils.newClassInstance;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.function.Supplier;

import lombok.experimental.UtilityClass;

import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.PulsarClientException.UnsupportedAuthenticationException;
import org.apache.pulsar.client.api.schema.*;
import org.apache.pulsar.common.schema.KeyValue;
import org.apache.pulsar.common.schema.SchemaInfo;
import org.apache.pulsar.common.schema.SchemaType;

@SuppressWarnings("unchecked")
@UtilityClass
public class DefaultImplementation {

    private static final Class<ClientBuilder> CLIENT_BUILDER_IMPL = newClassInstance(
            "org.apache.pulsar.client.impl.ClientBuilderImpl");

    private static final Constructor<MessageId> MESSAGE_ID_IMPL_long_long_int = getConstructor(
            "org.apache.pulsar.client.impl.MessageIdImpl",
            Long.TYPE, Long.TYPE, Integer.TYPE);

    private static final Method MESSAGE_ID_IMPL_fromByteArray = getStaticMethod(
            "org.apache.pulsar.client.impl.MessageIdImpl", "fromByteArray",
            byte[].class);
    private static final Method MESSAGE_ID_IMPL_fromByteArrayWithTopic = getStaticMethod(
            "org.apache.pulsar.client.impl.MessageIdImpl",
            "fromByteArrayWithTopic", byte[].class, String.class);

    private static final Constructor<Authentication> AUTHENTICATION_TOKEN_String = getConstructor(
            "org.apache.pulsar.client.impl.auth.AuthenticationToken", String.class);

    private static final Constructor<Authentication> AUTHENTICATION_TOKEN_Supplier = getConstructor(
            "org.apache.pulsar.client.impl.auth.AuthenticationToken", Supplier.class);

    private static final Constructor<Authentication> AUTHENTICATION_TLS_String_String = getConstructor(
            "org.apache.pulsar.client.impl.auth.AuthenticationTls", String.class, String.class);

    private static final Constructor<SchemaDefinitionBuilder> SCHEMA_DEFINITION_BUILDER_CONSTRUCTOR = getConstructor(
            "org.apache.pulsar.client.impl.schema.SchemaDefinitionBuilderImpl");

    public static <T> SchemaDefinitionBuilder<T> newSchemaDefinitionBuilder() {
        return catchExceptions(() -> (SchemaDefinitionBuilder<T>)SCHEMA_DEFINITION_BUILDER_CONSTRUCTOR.newInstance());
    }

    public static ClientBuilder newClientBuilder() {
        return catchExceptions(() -> CLIENT_BUILDER_IMPL.newInstance());
    }

    public static MessageId newMessageId(long ledgerId, long entryId, int partitionIndex) {
        return catchExceptions(() -> MESSAGE_ID_IMPL_long_long_int.newInstance(ledgerId, entryId, partitionIndex));
    }

    public static MessageId newMessageIdFromByteArray(byte[] data) {
        return catchExceptions(() -> (MessageId) MESSAGE_ID_IMPL_fromByteArray.invoke(null, data));
    }

    public static MessageId newMessageIdFromByteArrayWithTopic(byte[] data, String topicName) {
        return catchExceptions(() -> (MessageId) MESSAGE_ID_IMPL_fromByteArrayWithTopic.invoke(null, data, topicName));
    }

    public static Authentication newAuthenticationToken(String token) {
        return catchExceptions(() -> (Authentication) AUTHENTICATION_TOKEN_String.newInstance(token));
    }

    public static Authentication newAuthenticationToken(Supplier<String> supplier) {
        return catchExceptions(() -> (Authentication) AUTHENTICATION_TOKEN_Supplier.newInstance(supplier));
    }

    public static Authentication newAuthenticationTLS(String certFilePath, String keyFilePath) {
        return catchExceptions(
                () -> (Authentication) AUTHENTICATION_TLS_String_String.newInstance(certFilePath, keyFilePath));
    }

    public static Authentication createAuthentication(String authPluginClassName, String authParamsString)
            throws UnsupportedAuthenticationException {
        return catchExceptions(
                () -> (Authentication) getStaticMethod("org.apache.pulsar.client.impl.AuthenticationUtil", "create",
                        String.class, String.class)
                                .invoke(null, authPluginClassName, authParamsString));
    }

    public static Authentication createAuthentication(String authPluginClassName, Map<String, String> authParams)
            throws UnsupportedAuthenticationException {
        return catchExceptions(
                () -> (Authentication) getStaticMethod("org.apache.pulsar.client.impl.AuthenticationUtil", "create",
                        String.class, Map.class)
                                .invoke(null, authPluginClassName, authParams));
    }

    public static Schema<byte[]> newBytesSchema() {
        return catchExceptions(
                () -> (Schema<byte[]>) newClassInstance("org.apache.pulsar.client.impl.schema.BytesSchema")
                        .newInstance());
    }

    public static Schema<String> newStringSchema() {
        return catchExceptions(
                () -> (Schema<String>) newClassInstance("org.apache.pulsar.client.impl.schema.StringSchema")
                        .newInstance());
    }

    public static Schema<String> newStringSchema(Charset charset) {
        return catchExceptions(
                () -> (Schema<String>) getConstructor("org.apache.pulsar.client.impl.schema.StringSchema", Charset.class)
                        .newInstance(charset));
    }

    public static Schema<Byte> newByteSchema() {
        return catchExceptions(
                () -> (Schema<Byte>) newClassInstance("org.apache.pulsar.client.impl.schema.ByteSchema")
                        .newInstance());
    }

    public static Schema<Short> newShortSchema() {
        return catchExceptions(
                () -> (Schema<Short>) newClassInstance("org.apache.pulsar.client.impl.schema.ShortSchema")
                        .newInstance());
    }

    public static Schema<Integer> newIntSchema() {
        return catchExceptions(
                () -> (Schema<Integer>) newClassInstance("org.apache.pulsar.client.impl.schema.IntSchema")
                        .newInstance());
    }

    public static Schema<Long> newLongSchema() {
        return catchExceptions(
                () -> (Schema<Long>) newClassInstance("org.apache.pulsar.client.impl.schema.LongSchema")
                        .newInstance());
    }

    public static Schema<Boolean> newBooleanSchema() {
        return catchExceptions(
                () -> (Schema<Boolean>) newClassInstance("org.apache.pulsar.client.impl.schema.BooleanSchema")
                        .newInstance());
    }

    public static Schema<ByteBuffer> newByteBufferSchema() {
        return catchExceptions(
                () -> (Schema<ByteBuffer>) newClassInstance("org.apache.pulsar.client.impl.schema.ByteBufferSchema")
                        .newInstance());
    }

    public static Schema<Float> newFloatSchema() {
        return catchExceptions(
                () -> (Schema<Float>) newClassInstance("org.apache.pulsar.client.impl.schema.FloatSchema")
                        .newInstance());
    }

    public static Schema<Double> newDoubleSchema() {
        return catchExceptions(
                () -> (Schema<Double>) newClassInstance("org.apache.pulsar.client.impl.schema.DoubleSchema")
                        .newInstance());
    }

    public static Schema<Date> newDateSchema() {
        return catchExceptions(
                () -> (Schema<Date>) getStaticMethod("org.apache.pulsar.client.impl.schema.DateSchema", "of", null)
                        .invoke(null, null));
    }

    public static Schema<Time> newTimeSchema() {
        return catchExceptions(
              () -> (Schema<Time>) getStaticMethod("org.apache.pulsar.client.impl.schema.TimeSchema", "of", null)
                    .invoke(null, null));
    }

    public static Schema<Timestamp> newTimestampSchema() {
        return catchExceptions(
              () -> (Schema<Timestamp>) getStaticMethod("org.apache.pulsar.client.impl.schema.TimestampSchema", "of", null)
                    .invoke(null, null));
    }

    public static <T> Schema<T> newAvroSchema(SchemaDefinition schemaDefinition) {
        return catchExceptions(
                () -> (Schema<T>) getStaticMethod("org.apache.pulsar.client.impl.schema.AvroSchema", "of", SchemaDefinition.class)
                        .invoke(null,schemaDefinition));
    }

    public static <T extends com.google.protobuf.GeneratedMessageV3> Schema<T> newProtobufSchema(Class<T> clazz) {
        return catchExceptions(
                () -> (Schema<T>) getStaticMethod("org.apache.pulsar.client.impl.schema.ProtobufSchema", "of", Class.class)
                        .invoke(null, clazz));
    }

    public static <T> Schema<T> newJSONSchema(SchemaDefinition schemaDefinition) {
        return catchExceptions(
                () -> (Schema<T>) getStaticMethod("org.apache.pulsar.client.impl.schema.JSONSchema", "of", SchemaDefinition.class)
                        .invoke(null, schemaDefinition));
    }

    public static Schema<GenericRecord> newAutoConsumeSchema() {
        return catchExceptions(
                () -> (Schema<GenericRecord>) newClassInstance("org.apache.pulsar.client.impl.schema.AutoConsumeSchema")
                        .newInstance());
    }

    public static Schema<byte[]> newAutoProduceSchema() {
        return catchExceptions(
                () -> (Schema<byte[]>) newClassInstance("org.apache.pulsar.client.impl.schema.AutoProduceBytesSchema")
                        .newInstance());
    }

    public static Schema<KeyValue<byte[], byte[]>> newKeyValueBytesSchema() {
        return catchExceptions(
                () -> (Schema<KeyValue<byte[], byte[]>>) getStaticMethod("org.apache.pulsar.client.impl.schema.KeyValueSchema",
                        "kvBytes").invoke(null));
    }

    public static <K, V> Schema<KeyValue<K, V>> newKeyValueSchema(Schema<K> keySchema, Schema<V> valueSchema) {
        return catchExceptions(
                () -> (Schema<KeyValue<K, V>>) getStaticMethod("org.apache.pulsar.client.impl.schema.KeyValueSchema",
                        "of", Schema.class, Schema.class).invoke(null, keySchema, valueSchema));
    }

    public static <K, V> Schema<KeyValue<K, V>> newKeyValueSchema(Class<K> key, Class<V> value, SchemaType type) {
        return catchExceptions(
                () -> (Schema<KeyValue<K, V>>) getStaticMethod("org.apache.pulsar.client.impl.schema.KeyValueSchema",
                        "of", Class.class, Class.class, SchemaType.class).invoke(null, key, value, type));
    }

    public static Schema<?> getSchema(SchemaInfo schemaInfo) {
        return catchExceptions(
                () -> (Schema<?>) getStaticMethod("org.apache.pulsar.client.impl.schema.AutoConsumeSchema",
                        "getSchema", SchemaInfo.class).invoke(null, schemaInfo));
    }

    public static GenericSchema getGenericSchema(SchemaInfo schemaInfo) {
        return catchExceptions(
            () -> (GenericSchema) getStaticMethod("org.apache.pulsar.client.impl.schema.generic.GenericSchemaImpl",
                "of", SchemaInfo.class).invoke(null, schemaInfo));
    }

    public static RecordSchemaBuilder newRecordSchemaBuilder(String name) {
        return catchExceptions(
                () -> (RecordSchemaBuilder) getConstructor("org.apache.pulsar.client.impl.schema.RecordSchemaBuilderImpl",
                        String.class).newInstance(name));
    }
}
