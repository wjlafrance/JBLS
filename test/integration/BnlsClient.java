package integration;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Minimal BNLS wire client: (WORD length incl. 3-byte header)(BYTE id)(payload), little-endian. */
final class BnlsClient implements AutoCloseable {
  private final Socket socket;
  private final DataInputStream in;
  private final OutputStream out;

  BnlsClient(int port) throws IOException {
    socket = new Socket();
    socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
    socket.setSoTimeout(5000);
    in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    out = socket.getOutputStream();
  }

  void send(int id, byte[] payload) throws IOException {
    ByteBuffer b = ByteBuffer.allocate(3 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
    b.putShort((short) (3 + payload.length));
    b.put((byte) id);
    b.put(payload);
    out.write(b.array());
    out.flush();
  }

  Packet receive() throws IOException {
    int len = in.readUnsignedByte() | (in.readUnsignedByte() << 8);
    int id = in.readUnsignedByte();
    byte[] body = new byte[len - 3];
    in.readFully(body);
    return new Packet(id, body);
  }

  Packet request(int id, byte[] payload) throws IOException {
    send(id, payload);
    return receive();
  }

  /** True if the server closed the connection within the timeout; false if it stayed open (or sent data). */
  boolean closedByServer(int timeoutMillis) throws IOException {
    socket.setSoTimeout(timeoutMillis);
    try {
      return in.read() == -1;
    } catch (SocketTimeoutException e) {
      return false;
    } catch (EOFException | SocketException e) {
      return true;
    }
  }

  /** True if any byte arrives within the timeout (false on timeout or close). */
  boolean receivesWithin(int timeoutMillis) throws IOException {
    socket.setSoTimeout(timeoutMillis);
    try {
      return in.read() != -1;
    } catch (SocketTimeoutException e) {
      return false;
    } catch (EOFException | SocketException e) {
      return false;
    }
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }

  static final class Packet {
    final int id;
    private final ByteBuffer data;

    Packet(int id, byte[] body) {
      this.id = id;
      this.data = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
    }

    int dword() { return data.getInt(); }
    byte[] bytes(int n) { byte[] b = new byte[n]; data.get(b); return b; }
    int remaining() { return data.remaining(); }

    String ntString() {
      ByteArrayOutputStream s = new ByteArrayOutputStream();
      for (byte c; (c = data.get()) != 0; ) s.write(c);
      return s.toString(StandardCharsets.ISO_8859_1);
    }
  }

  /** Little-endian payload builder. */
  static final class Payload {
    private final ByteArrayOutputStream b = new ByteArrayOutputStream();

    Payload dword(int v) {
      b.write(v); b.write(v >> 8); b.write(v >> 16); b.write(v >> 24);
      return this;
    }
    Payload qword(long v) { dword((int) v); return dword((int) (v >>> 32)); }
    Payload bytes(byte[] v) { b.writeBytes(v); return this; }
    Payload ntString(String s) { b.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1)); b.write(0); return this; }
    byte[] build() { return b.toByteArray(); }
  }
}
