package com.inscriptive.kahlo.lifecycle;

import com.inscriptive.common.Logger;
import com.inscriptive.common.cli.Command;

import java.util.Set;

/**
 * A command that executes the prepare, validate, and start phases of its lifecycles before some
 * work, then shuts down the lifecycles.
 */
public abstract class LifecycleRunAndExitCommand implements Command {
  private static final Logger logger = Logger.getLogger(LifecycleRunAndExitCommand.class);

  private final Set<? extends Lifecycle> lifecycles;

  public LifecycleRunAndExitCommand(LifecycleComponent component) {
    this.lifecycles = component.lifecycles();
  }

  @Override
  public final void execute(String... args) throws Exception {
    try {
      logger.info("Running lifecycles: %s", lifecycles);
      logger.info("Lifecycle: prepare");
      lifecycles.forEach(Lifecycle::prepare);
      logger.info("Lifecycle: validate");
      lifecycles.forEach(Lifecycle::validate);
      logger.info("Lifecycle: start");
      lifecycles.forEach(Lifecycle::start);
      run(args);
      lifecycles.forEach(Lifecycle::shutdown);
    } catch (Exception e) {
      logger.error("Error during startup. Shutting down: %s", e);
      lifecycles.forEach(Lifecycle::shutdown);
      System.out.flush();
      System.exit(1);
    }
    System.exit(0);
  }

  /**
   * Do work within a running lifecycle context. Implementations should exit cleanly, without
   * leaving any new threads running.
   */
  protected abstract void run(String... args) throws Exception;
}
