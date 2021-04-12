package j;

import org.jgroups.Address;
import org.jgroups.blocks.RspFilter;
// TODO:
public class b
        implements RspFilter {
    private int a;

    public boolean isAcceptable(Object paramObject, Address paramAddress) {
        if (paramObject != null) {
            return true;
        }
        this.a += 1;
        return false;
    }

    public boolean needMoreResponses() {
        return true;
    }

    public int a() {
        return this.a;
    }
}
