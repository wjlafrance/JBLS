package integration;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs the packaged JBLS jar as a separate process in a temp working directory
 * (JBLS reads/writes settings.ini, ips.ini, bots.ini relative to its cwd).
 * A subprocess keeps JBLS's static state (Constants, caches, SRP.N) out of the test JVM.
 */
final class JblsServer implements AutoCloseable {
  final int port;
  final Path workDir;
  final Path log;
  private final Process process;

  /**
   * @param mainSettings extra [Main] keys (override defaults)
   * @param extraIni     raw text appended to settings.ini (product sections etc.)
   * @param files        extra files to write into the working dir (e.g. bots.ini)
   */
  JblsServer(Map<String, String> mainSettings, String extraIni, Map<String, String> files) throws Exception {
    String jar = System.getProperty("jbls.jar");
    if (jar == null || !new File(jar).isFile())
      throw new IllegalStateException("System property jbls.jar must point at the packaged JBLS jar (run via mvn verify)");

    port = freePort();
    workDir = Files.createTempDirectory("jbls-e2e-");
    log = workDir.resolve("jbls.out");

    // One line per key: JBLS's INI reader takes the first occurrence, so overrides must replace defaults.
    Map<String, String> main = new java.util.LinkedHashMap<>();
    main.put("BNLSPort", Integer.toString(port));
    main.put("EnableHTTP", "false");
    main.put("IPAuth", "0");
    main.put("RequireAuth", "false");
    main.put("DisplayPacketInfo", "true");
    main.putAll(mainSettings);
    StringBuilder ini = new StringBuilder("[Main]\n");
    main.forEach((k, v) -> ini.append(k).append('=').append(v).append('\n'));
    ini.append("[Statistics]\nEnable=false\n");
    ini.append(extraIni);
    Files.writeString(workDir.resolve("settings.ini"), ini);
    for (Map.Entry<String, String> f : files.entrySet())
      Files.writeString(workDir.resolve(f.getKey()), f.getValue());

    String java = ProcessHandle.current().info().command().orElse("java");
    List<String> cmd = new ArrayList<>(List.of(java, "-jar", jar));
    process = new ProcessBuilder(cmd)
        .directory(workDir.toFile())
        .redirectErrorStream(true)
        .redirectOutput(log.toFile())
        .start();
    awaitListening();
  }

  JblsServer() throws Exception {
    this(Map.of(), "", Map.of());
  }

  BnlsClient connect() throws IOException {
    return new BnlsClient(port);
  }

  String output() throws IOException {
    return Files.readString(log);
  }

  boolean isAlive() {
    return process.isAlive();
  }

  private void awaitListening() throws Exception {
    long deadline = System.currentTimeMillis() + 15_000;
    while (System.currentTimeMillis() < deadline) {
      if (!process.isAlive())
        throw new IllegalStateException("JBLS exited early:\n" + output());
      if (output().contains("Server socket opened on port " + port)) {
        // Port is bound; confirm it accepts (this opens and closes one connection).
        try (Socket s = new Socket("127.0.0.1", port)) {
          return;
        } catch (IOException e) {
          // not yet
        }
      }
      Thread.sleep(50);
    }
    throw new IllegalStateException("JBLS did not start listening on " + port + ":\n" + output());
  }

  private static int freePort() throws IOException {
    try (ServerSocket s = new ServerSocket(0)) {
      return s.getLocalPort();
    }
  }

  @Override
  public void close() throws Exception {
    process.destroy();
    if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS))
      process.destroyForcibly();
  }
}
