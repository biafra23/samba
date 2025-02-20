/*
 * Copyright Consensys Software Inc., 2022
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package samba.samba;

import static samba.samba.exceptions.ExitConstants.ERROR_EXIT_CODE;
import static samba.samba.exceptions.ExitConstants.FATAL_EXIT_CODE;

import samba.logging.StatusLogger;
import samba.samba.exceptions.ExceptionUtil;
import samba.samba.exceptions.FatalServiceFailureException;
import samba.samba.exceptions.ShuttingDownException;
import samba.storage.DatabaseStorageException;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;
import java.nio.channels.ClosedChannelException;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.pegasys.teku.infrastructure.events.ChannelExceptionHandler;

public final class SambaDefaultExceptionHandler
    implements ChannelExceptionHandler, UncaughtExceptionHandler {
  private static final Logger LOG = LogManager.getLogger(SambaDefaultExceptionHandler.class);

  private final StatusLogger statusLog;

  public SambaDefaultExceptionHandler() {
    this(StatusLogger.STATUS_LOG);
  }

  @VisibleForTesting
  SambaDefaultExceptionHandler(final StatusLogger statusLog) {
    this.statusLog = statusLog;
  }

  @Override
  public void handleException(
      final Throwable error,
      final Object subscriber,
      final Method invokedMethod,
      final Object[] args) {
    handleException(
        error,
        "event '"
            + invokedMethod.getDeclaringClass()
            + "."
            + invokedMethod.getName()
            + "' in handler '"
            + subscriber.getClass().getName()
            + "'");
  }

  @Override
  public void uncaughtException(final Thread t, final Throwable e) {
    handleException(e, t.getName());
  }

  private void handleException(final Throwable exception, final String subscriberDescription) {
    final Optional<FatalServiceFailureException> fatalServiceError =
        ExceptionUtil.getCause(exception, FatalServiceFailureException.class);

    if (fatalServiceError.isPresent()) {
      final String failedService = fatalServiceError.get().getService();
      statusLog.fatalError(failedService, exception);
      System.exit(FATAL_EXIT_CODE);
    } else if (ExceptionUtil.getCause(exception, DatabaseStorageException.class)
        .filter(DatabaseStorageException::isUnrecoverable)
        .isPresent()) {
      statusLog.fatalError(subscriberDescription, exception);
      System.exit(FATAL_EXIT_CODE);
    } else if (exception instanceof OutOfMemoryError) {
      statusLog.fatalError(subscriberDescription, exception);
      System.exit(ERROR_EXIT_CODE);
    } else if (exception instanceof ShuttingDownException) {
      LOG.debug("Shutting down", exception);
    } else if (isExpectedNettyError(exception)) {
      LOG.debug("Channel unexpectedly closed", exception);
    } else if (Throwables.getRootCause(exception) instanceof RejectedExecutionException) {
      LOG.error(
          "Unexpected rejected execution due to full task queue in {}", subscriberDescription);
    } else if (isSpecFailure(exception)) {
      statusLog.specificationFailure(subscriberDescription, exception);
    } else {
      statusLog.unexpectedFailure(subscriberDescription, exception);
    }
  }

  private boolean isExpectedNettyError(final Throwable exception) {
    return exception instanceof ClosedChannelException;
  }

  private static boolean isSpecFailure(final Throwable exception) {
    return exception instanceof IllegalArgumentException;
  }
}
