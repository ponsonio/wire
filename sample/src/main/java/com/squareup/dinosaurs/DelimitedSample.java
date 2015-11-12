package com.squareup.dinosaurs;

import com.squareup.geology.Period;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import java.io.EOFException;
import java.io.IOException;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

public final class DelimitedSample {
  public static void main(String... args) throws IOException {
    Buffer buffer = new Buffer();

    Dinosaur stegosaurus = new Dinosaur.Builder()
        .name("Stegosaurus")
        .period(Period.JURASSIC)
        .build();
    System.out.println("WRITE: " + stegosaurus);
    writeDelimited(stegosaurus, Dinosaur.ADAPTER, buffer);

    Dinosaur tRex = new Dinosaur.Builder()
        .name("T-Rex")
        .period(Period.JURASSIC)
        .build();
    System.out.println("WRITE: " + tRex);
    writeDelimited(tRex, Dinosaur.ADAPTER, buffer);

    System.out.println("BYTES: " + buffer.clone().readByteString().hex());

    Dinosaur read1 = readDelimited(Dinosaur.ADAPTER, buffer);
    Dinosaur read2 = readDelimited(Dinosaur.ADAPTER, buffer);
    System.out.println("READ: " + read1);
    System.out.println("READ: " + read2);
  }

  private static <T extends Message<T, ?>> void writeDelimited(T message, ProtoAdapter<T> adapter,
      BufferedSink sink) throws IOException {
    ProtoWriter writer = new ProtoWriter(sink);
    int length = adapter.encodedSize(message);
    writer.writeVarint64(length);
    adapter.encode(writer, message);
  }

  private static <T extends Message<T, ?>> T readDelimited(ProtoAdapter<T> adapter,
      BufferedSource source) throws IOException {
    ProtoReader reader = new ProtoReader(source);
    long length = reader.readVarint64();
    BufferedSource lengthLimitedSource = Okio.buffer(new FixedLengthSource(source, length));
    return adapter.decode(lengthLimitedSource);
  }

  public static final class FixedLengthSource extends ForwardingSource {
    private long bytesRemaining;
    private boolean closed;

    public FixedLengthSource(Source delegate, long length) throws IOException {
      super(delegate);
      bytesRemaining = length;
    }

    @Override public long read(Buffer sink, long byteCount) throws IOException {
      if (byteCount < 0) throw new IllegalArgumentException("byteCount < 0: " + byteCount);
      if (closed) throw new IllegalStateException("closed");
      if (bytesRemaining == 0) return -1;

      long read = super.read(sink, Math.min(bytesRemaining, byteCount));
      if (read == -1) {
        throw new EOFException("unexpected end of stream");
      }

      bytesRemaining -= read;
      return read;
    }

    @Override public void close() throws IOException {
      if (closed) return;
      closed = true;
    }
  }
}
