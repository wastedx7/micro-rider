package com.ridehailing.common.kafka;

import com.google.protobuf.MessageLite;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * Kafka value serializer that writes protobuf messages in their wire format.
 */
public class ProtobufSerializer implements Serializer<MessageLite> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // no-op
    }

    @Override
    public byte[] serialize(String topic, MessageLite data) {
        if (data == null) {
            return null;
        }
        return data.toByteArray();
    }

    @Override
    public void close() {
        // no-op
    }
}