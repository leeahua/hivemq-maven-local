package bk1;

import bg1.XodusUtils;
import com.hivemq.spi.annotations.NotNull;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalExecutable;

public class ClearTransactionalExecutable implements TransactionalExecutable {
    @NotNull
    private final Store store;

    public ClearTransactionalExecutable(@NotNull Store store) {
        this.store = store;
    }

    public void execute(Transaction txn) {
        Cursor cursor = this.store.openCursor(txn);
        Throwable throwable = null;
        try {
            while (cursor.getNext()) {
                cursor.deleteCurrent();
            }
        } catch (Throwable e) {
            throwable = e;
            throw e;
        } finally {
            XodusUtils.close(cursor, throwable);
        }
    }
}
