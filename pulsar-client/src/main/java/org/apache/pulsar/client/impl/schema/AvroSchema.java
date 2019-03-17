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
package org.apache.pulsar.client.impl.schema;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.pulsar.client.api.SchemaSerializationException;
import org.apache.pulsar.common.schema.SchemaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * An AVRO schema implementation.
 */
@Slf4j
public class AvroSchema<T> extends StructSchema<T> {

    private ReflectDatumWriter<T> datumWriter;
    private ReflectDatumReader<T> reader;
    private BinaryEncoder encoder;
    private ByteArrayOutputStream byteArrayOutputStream;

    private static final ThreadLocal<BinaryDecoder> decoders =
            new ThreadLocal<>();

    private AvroSchema(org.apache.avro.Schema schema,
                       Map<String, String> properties) {
        super(
            SchemaType.AVRO,
            schema,
            properties);

        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.encoder = EncoderFactory.get().binaryEncoder(this.byteArrayOutputStream, this.encoder);
        this.datumWriter = new ReflectDatumWriter<>(this.schema);
        this.reader = new ReflectDatumReader<>(this.schema);
    }

    @Override
    public synchronized byte[] encode(T message) {
        try {
            datumWriter.write(message, this.encoder);
            this.encoder.flush();
            return this.byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new SchemaSerializationException(e);
        } finally {
            this.byteArrayOutputStream.reset();
        }
    }

    @Override
    public T decode(byte[] bytes) {
        try {
            BinaryDecoder decoderFromCache = decoders.get();
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bytes, decoderFromCache);
            if (decoderFromCache == null) {
                decoders.set(decoder);
            }
            return reader.read(null, DecoderFactory.get().binaryDecoder(bytes, decoder));
        } catch (IOException e) {
            throw new SchemaSerializationException(e);
        }
    }

    public static <T> AvroSchema<T> of(Class<T> pojo) {
        return new AvroSchema<>(createAvroSchema(pojo), Collections.emptyMap());
    }

    public static <T> AvroSchema<T> of(Class<T> pojo, Map<String, String> properties) {
        return new AvroSchema<>(createAvroSchema(pojo), properties);
    }

    /**
     * Create an Avro schema based on provided schema definition.
     *
     * @param schemaDefinition avro schema definition
     * @param properties schema properties
     * @return avro schema instance
     */
    public static <T> AvroSchema<T> of(String schemaDefinition, Map<String, String> properties) {
        return new AvroSchema<>(parseAvroSchema(schemaDefinition), properties);
    }

}
