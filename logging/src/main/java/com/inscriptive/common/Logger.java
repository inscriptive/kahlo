package com.inscriptive.common;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A logger that wraps log4j2's ThreadContext API. That API is based on thread locals, but that's
 * not good enough for us. For example, a single gRPC call context may span multiple threads.
 * Thread pooling also poses a threat to the thread local context. Instead, we use a plugin model
 * to let client code register a callback, which the logger will call on every log line.
 * TODO(jack) be more efficient??
 *
 * Feel free to add further delegating methods to this class; the log4j Logger interface is pretty
 * big.
 */
public class Logger {
  // guards the THREAD_CONTEXT_PLUGINS map
  private static final ReadWriteLock lock = new ReentrantReadWriteLock();
  private static final Map<String, ThreadContextPlugin> THREAD_CONTEXT_PLUGINS = new HashMap<>();

  // Futz around with static initialization stuff for loggers.
  static {
    // will redirect java.util.logging.Logger to slf4j
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    // We make sure unhandled exceptions also flow through our logger rather than stderr.
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
      // Log a failure similar to how ThreadGroup.uncaughtException() does it.
      LogManager.getRootLogger().fatal("Unhandled exception " + t.getName(), e);

      // Because of a quirk of multiline handling in our log aggregator, we need
      // 1 more line after the previous stack trace to flush fluentd's buffer.
      LogManager.getRootLogger().info("goodbye.");
    });
  }

  private final org.apache.logging.log4j.Logger logger;

  /**
   * Never create a `Logger` manually. Always use one of the factories.
   *
   * @param l
   */
  private Logger(org.apache.logging.log4j.Logger l) {
    logger = l;
  }

  public static Logger getLogger(Class<?> klass) {
    return new Logger(LogManager.getFormatterLogger(klass));
  }

  public void log(Level level, String message, Object... args) {
    getThreadContext();
    logger.log(level, message, args);
  }

  public void debug(String message, Object... args) {
    getThreadContext();
    logger.debug(message, args);
  }

  public void info(String message, Object... args) {
    getThreadContext();
    logger.info(message, args);
  }

  public void warn(String message, Object... args) {
    getThreadContext();
    logger.warn(message, args);
  }

  public void error(String message, Object... args) {
    getThreadContext();
    logger.error(message, args);
  }

  public void fatal(String message, Object... args) {
    getThreadContext();
    logger.fatal(message, args);
  }

  /**
   * Register a provider for an item of thread-local logging context.
   *
   * @param plugin the plugin to register.
   */
  public static void registerThreadContextPlugin(ThreadContextPlugin plugin) {
    Lock writeLock = lock.writeLock();
    try {
      writeLock.lock();
      if (THREAD_CONTEXT_PLUGINS.containsKey(plugin.key)) {
        checkArgument(THREAD_CONTEXT_PLUGINS.get(plugin.key) == plugin,
            "A different plugin for key %s was already registered: %s", plugin.key,
            THREAD_CONTEXT_PLUGINS.get(plugin.key));
      } else {
        THREAD_CONTEXT_PLUGINS.put(plugin.key, plugin);
      }
    } finally {
      writeLock.unlock();
    }
  }

  private static void getThreadContext() {
    ThreadContext.clearMap();
    Lock readLock = lock.readLock();
    try {
      readLock.lock();
      THREAD_CONTEXT_PLUGINS.forEach((key, plugin) -> {
        String value = plugin.contextValue();
        if (value != null) {
          ThreadContext.put(key, value);
        }
      });
    } finally {
      readLock.unlock();
    }
  }

  public abstract static class ThreadContextPlugin {
    protected final String key;

    protected ThreadContextPlugin(String key) {
      this.key = key;
    }

    /**
     * Provide the piece of logging context for this plugin's key. If this method returns null, no
     * value will be added to the logging context.
     *
     * NOTE: IMPLEMENTATIONS SHOULD BE VERY FAST
     */
    protected abstract String contextValue();

    /* TODO(jack) allow plugins to decide whether a null value should be logged */
  }
}
