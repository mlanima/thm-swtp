package de.thm.swtp.api.common;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins TxLogger's commit/rollback timing without spinning a full Spring tx context:
 * TransactionSynchronizationManager is driven manually, and a Logback ListAppender
 * records the emitted events. Avoids Mockito ambiguity across SLF4J's overloaded
 * info/warn signatures.
 */
class TxLoggerTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger("TxLoggerTest");
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        logger.detachAppender(appender);
    }

    private List<ILoggingEvent> events() {
        return appender.list;
    }

    private void runCallbacks(int status, boolean commit) {
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            if (commit) {
                sync.afterCommit();
            }
            sync.afterCompletion(status);
        }
    }

    @Test
    void logsImmediatelyWhenNoTransactionIsActive() {
        // No initSynchronization() → not active → immediate info, no registration.
        TxLogger.afterCommit(logger, "Project deleted: project={}", "p1");

        assertThat(events()).hasSize(1);
        assertThat(events().get(0).getFormattedMessage()).isEqualTo("Project deleted: project=p1");
        assertThat(events().get(0).getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
    }

    @Test
    void logsInfoAfterCommitAndNoWarn() {
        TransactionSynchronizationManager.initSynchronization();
        TxLogger.afterCommit(logger, "Project deleted: project={}", "p1");

        // Before commit: nothing logged yet.
        assertThat(events()).isEmpty();

        runCallbacks(TransactionSynchronization.STATUS_COMMITTED, true);

        assertThat(events()).hasSize(1);
        assertThat(events().get(0).getFormattedMessage()).isEqualTo("Project deleted: project=p1");
        assertThat(events().get(0).getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
    }

    @Test
    void logsWarnWithContextOnRollbackAndSkipsInfo() {
        TransactionSynchronizationManager.initSynchronization();
        TxLogger.afterCommit(logger, "Project deleted: project={}", "p1");

        // Rollback: afterCommit is NOT triggered, only afterCompletion(ROLLED_BACK).
        runCallbacks(TransactionSynchronization.STATUS_ROLLED_BACK, false);

        assertThat(events()).hasSize(1);
        assertThat(events().get(0).getLevel()).isEqualTo(ch.qos.logback.classic.Level.WARN);
        assertThat(events().get(0).getFormattedMessage()).isEqualTo("Rolled back — Project deleted: project=p1");
    }
}
