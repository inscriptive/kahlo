package com.inscriptive.kahlo.lifecycle;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.inscriptive.common.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

/**
 * Manager for lifecycles. Bound as a singleton within its injection scope. It runs a set of
 * lifecycles as a partially-ordered set of operations. Provides graceful shutdown guarantees
 * using the {@link Runtime#addShutdownHook(Thread)} mechanism. Thus, tasks run with this class
 * will exit gracefully on system interrupts or uncaught exceptions.
 */
@Singleton
public class LifecycleManager {
  private static final Logger logger = Logger.getLogger(LifecycleManager.class);

  private final Set<? extends Lifecycle> lifecycles;
  private final Thread shutdownHook;

  @Inject
  public LifecycleManager(Set<Lifecycle> lifecycles) {
    this.lifecycles = lifecycles;
    shutdownHook = new Thread(() -> {
      logger.info("Lifecycle: shutdown");
      lifecycles.forEach(this::silentlyShutdown);
    });
  }

  /**
   * Run the lifecycle, including a task to execute within the lifecycle context. Exits once the
   * task is complete.
   *
   * It's assumed but not required that the task has finite length.
   *
   * @param main the task to run in the lifecycle context.
   */
  public void runInContext(ThrowingRunnable main) {
    lifecycles.forEach(l -> logger.info("Registered lifecycle: %s", l));

    try {
      prepare();
      validate();
      start();
      try {
        main.run();
      } catch (Throwable t) {
        Throwables.propagate(t);
      }
    } finally {
      logger.info("Lifecycle: shutdown");
      lifecycles.forEach(this::silentlyShutdown);
    }
  }

  /**
   * Run the lifecycle without immediately exiting.
   */
  public void runAsync() {
    Thread keepalive = new Thread(() -> {
      try {
        while (true) {
          Thread.sleep(1_000);
        }
      } catch (InterruptedException e) {
      }
    });
    keepalive.setName("keepalive");
    keepalive.setPriority(Thread.MIN_PRIORITY);
    keepalive.setDaemon(false);
    keepalive.start();

    Runtime.getRuntime().addShutdownHook(shutdownHook);

    lifecycles.forEach(l -> logger.info("Registered lifecycle: %s", l));

    try {
      prepare();
      validate();
      start();
    } catch (Throwable t) {
      logger.info("Lifecycle: shutdown");
      lifecycles.forEach(this::silentlyShutdown);
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
      throw t;
    }
  }

  private void prepare() {
    logger.info("Lifecycle: prepare");
    lifecycles.forEach(l -> {
      logger.info("preparing: %s", l);
      l.prepare();
    });
  }

  private void validate() {
    logger.info("Lifecycle: validate");
    lifecycles.forEach(l -> {
      logger.info("validating: %s", l);
      l.validate();
    });
  }

  private void start() {
    logger.info("Lifecycle: start");
    lifecycles.forEach(l -> {
      logger.info("starting: %s", l);
      l.start();
    });
  }

  private void silentlyShutdown(Lifecycle lifecycle) {
    try {
      lifecycle.shutdown();
    } catch (Throwable t) {
      logger.error("Error during shutdown of %s: %s", lifecycle, t);
    }
  }

  @VisibleForTesting
  Thread shutdownHook() {
    return shutdownHook;
  }

  public interface ThrowingRunnable {
    void run() throws Throwable;
  }
}
