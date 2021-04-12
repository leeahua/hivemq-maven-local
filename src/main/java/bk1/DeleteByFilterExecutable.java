package bk1;

import bg1.XodusUtils;
import com.hivemq.spi.annotations.NotNull;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalExecutable;
import u.Filter;

public class DeleteByFilterExecutable implements TransactionalExecutable {
    private final Filter filter;
    private final Store store;

    public DeleteByFilterExecutable(@NotNull Filter filter, @NotNull Store store) {
        this.filter = filter;
        this.store = store;
    }

    public void execute(Transaction txn) {
        Cursor cursor = this.store.openCursor(txn);
        while (cursor.getNext()) {
            if (this.filter.test(XodusUtils.toString(cursor.getKey()))) {
                cursor.deleteCurrent();
            }
        }
    }
}
