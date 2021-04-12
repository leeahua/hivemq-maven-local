package j;

import org.jgroups.Address;
import org.jgroups.blocks.RspFilter;
// TODO:
public class a implements RspFilter {
    private int a = 0;

    public boolean isAcceptable(Object paramObject, Address paramAddress) {
        if (((Boolean) paramObject).booleanValue()) {
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
