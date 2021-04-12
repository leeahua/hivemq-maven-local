package t;

import org.jgroups.Address;
import org.jgroups.Membership;
import org.jgroups.stack.MembershipChangePolicy;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class ClusterMembershipChangePolicy implements MembershipChangePolicy {
    private final SubViewComparator comparator = new SubViewComparator();

    public List<Address> getNewMembership(Collection<Address> currentMembers,
                                          Collection<Address> joiners,
                                          Collection<Address> leavers,
                                          Collection<Address> suspects) {
        Membership membership = new Membership(currentMembers);
        membership.remove(leavers);
        membership.remove(suspects);
        membership.add(joiners);
        return membership.getMembers();
    }

    public List<Address> getNewMembership(Collection<Collection<Address>> subviews) {
        Membership membership = new Membership();
        subviews.stream()
                .sorted(this.comparator)
                .forEach(membership::add);
        return membership.getMembers();
    }

    private class SubViewComparator implements Comparator<Collection<Address>> {

        @Override
        public int compare(Collection<Address> o1, Collection<Address> o2) {
            return Integer.compare(o1.size(), o2.size()) * -1;
        }
    }
}
