package de.thm.swtp.api.common;

import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Logs lifecycle messages only after the surrounding transaction has committed,
 * so the log never claims success for an action that rolled back.
 *
 * <p>On commit the message is logged at info. On rollback it is logged at warn
 * (prefixed with "Rolled back —") so the failed business event stays traceable
 * alongside the technical exception logged by GlobalExceptionHandler. If no
 * transaction is active, the message is logged immediately so the line is not lost.
 */
public final class TxLogger {

    private TxLogger() {
    }

    public static void afterCommit(Logger logger, String message, Object... args) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            logger.info(message, args);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                logger.info(message, args);
            }

            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    // Format lazily — only on the rollback branch, not on every commit.
                    logger.warn("Rolled back — " + MessageFormatter.arrayFormat(message, args).getMessage());
                }
            }
        });
    }
}
