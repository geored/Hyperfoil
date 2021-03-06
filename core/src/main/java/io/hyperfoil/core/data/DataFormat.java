package io.hyperfoil.core.data;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public enum DataFormat {
   // Make sure to release the buffer when done!
   BYTEBUF {
      @Override
      public Object convert(ByteBuf data, int offset, int length) {
         return data.retainedSlice(offset, length);
      }

      @Override
      public Object convert(byte[] data, int offset, int length) {
         ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(length);
         buffer.writeBytes(data, offset, length);
         return buffer;
      }
   },
   BYTES {
      @Override
      public Object convert(ByteBuf data, int offset, int length) {
         byte[] bytes = new byte[length];
         data.getBytes(offset, bytes, 0, length);
         return bytes;
      }

      @Override
      public Object convert(byte[] data, int offset, int length) {
         if (offset == 0 && length == data.length) {
            return data;
         }
         byte[] bytes = new byte[length];
         System.arraycopy(data, offset, bytes, 0, length);
         return bytes;
      }
   },
   STRING {
      @Override
      public Object convert(ByteBuf data, int offset, int length) {
         return data.toString(offset, length, StandardCharsets.UTF_8);
      }

      @Override
      public Object convert(byte[] data, int offset, int length) {
         return new String(data, offset, length, StandardCharsets.UTF_8);
      }
   };

   public abstract Object convert(ByteBuf data, int offset, int length);
   public abstract Object convert(byte[] data, int offset, int length);
}
