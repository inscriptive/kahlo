package com.inscriptive.common.grpc.shared;

import com.google.common.base.Throwables;
import com.google.protobuf.Message;
import io.grpc.Metadata;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Static instances of {@link Metadata.AsciiMarshaller} and {@link Metadata.BinaryMarshaller}
 */
public class Marshallers {
  private Marshallers() {
  }

  /**
   * Just the identity function for strings that are already ascii
   */
  static final Metadata.AsciiMarshaller<String> SIMPLE_ASCII_MARSHALLER =
      new Metadata.AsciiMarshaller<String>() {
        @Override
        public String toAsciiString(String value) {
          return value;
        }

        @Override
        public String parseAsciiString(String serialized) {
          return serialized;
        }
      };

  public static final <M extends Message> Metadata.BinaryMarshaller<M> protoMessageMarshaller(
      Class<M> messageClass) {
    Method parseFrom;
    try {
      parseFrom = messageClass.getDeclaredMethod("parseFrom", byte[].class);
    } catch (NoSuchMethodException e) {
      throw Throwables.propagate(e);
    }
    return new Metadata.BinaryMarshaller<M>() {
      @Override
      public byte[] toBytes(M m) {
        return m.toByteArray();
      }

      @Override
      public M parseBytes(byte[] bytes) {
        try {
          return (M) parseFrom.invoke(null, bytes);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw Throwables.propagate(e);
        }
      }
    };
  }
}
