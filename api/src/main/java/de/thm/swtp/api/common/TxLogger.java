package de.thm.swtp.api.common;

import org.slf4j.Logger;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Logs lifecycle messages only after the surrounding transaction has committed,
 * so the log never claims success for an action that rolled back.
 *
 * <p>On rollback the message is not logged; the rollback exception propagates and
 * is logged separately (e.g. via {@code GlobalExceptionHandler}). If no transaction
 * is active, the message is logged immediately so the line is not lost.
 */
public final class TxLogger {

    private TxLogger() {
    }

    public static void afterCommit(Logger logger, String message, Object... args) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // ponytail: no active tx — log immediately so the line isn't lost
            logger.info(message, args);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                logger.info(message, args);
            }
        });
    }
}