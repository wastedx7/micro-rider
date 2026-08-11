package com.ridehailing.common.kafka;

import com.google.protobuf.MessageLite;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Kafka value deserializer that turns protobuf wire bytes back into a generated message.
 * The concrete message type is supplied via the {@link #MESSAGE_CLASS_CONFIG} config key,
 * or via the typed constructor for programmatic use.
 */
public class ProtobufDeserializer<T extends MessageLite> implements Deserializer<T> {

    private static final Logger log = LoggerFactory.getLogger(ProtobufDeserializer.class);

    public static final String MESSAGE_CLASS_CONFIG = "protobuf.message.class";

    private final Class<T> fixedType;
    private Class<T> messageType;

    public ProtobufDeserializer() {
        this.fixedType = null;
    }

    public ProtobufDeserializer(Class<T> messageType) {
        this.fixedType = messageType;
        this.messageType = messageType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        if (fixedType != null) {
            return;
        }
        Object configured = configs.get(MESSAGE_CLASS_CONFIG);
        if (configured instanceof Class<?> clazz) {
            messageType = (Class<T>) clazz;
        } else if (configured instanceof String className) {
            try {
                messageType = (Class<T>) Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Unknown message class: " + className, e);
            }
        } else {
            throw new IllegalStateException(
                    "Missing '" + MESSAGE_CLASS_CONFIG + "' config for ProtobufDeserializer");
        }
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            Method parseFrom = messageType.getMethod("parseFrom", byte[].class);
            return messageType.cast(parseFrom.invoke(null, (Object) data));
        } catch (ReflectiveOperationException e) {
            log.error("Failed to deserialize protobuf message of type {} from topic {}",
                    messageType.getName(), topic, e);
            throw new IllegalArgumentException("Protobuf deserialization failed", e);
        }
    }

    @Override
    public void close() {
        // no-op
    }
}