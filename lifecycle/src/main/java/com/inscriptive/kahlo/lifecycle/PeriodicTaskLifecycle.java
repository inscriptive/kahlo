package com.inscriptive.kahlo.lifecycle;

import com.google.common.base.Throwables;
import com.inscriptive.common.Logger;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class PeriodicTaskLifecycle implements Lifecycle {
  private static final Logger logger = Logger.getLogger(PeriodicTaskLifecycle.class);

  private final ScheduledExecutorService executor;

  protected PeriodicTaskLifecycle() {
    this.executor = Executors.newSingleThreadScheduledExecutor();
  }

  public void start() {
    executor.scheduleAtFixedRate(() -> {
      try {
        while (doTask()) {
          logger.info("did task: %s", description());
        }
        logger.info("empty task. waiting: %s", description());
      } catch (Throwable t) {
        logger.error("Error in task: %s\n%s", description(), Throwables.getStackTraceAsString(t));
      }
    }, period().toMillis(), period().toMillis(), TimeUnit.MILLISECONDS);
  }

  public void shutdown() {
    executor.shutdown();
  }

  /**
   * Do the task. Return true if some work was done, hinting that we should be scheduled to do it
   * again immediately.
   */
  public abstract boolean doTask();

  protected Duration period() {
    return Duration.ofSeconds(3);
  }

  protected abstract String description();
}
