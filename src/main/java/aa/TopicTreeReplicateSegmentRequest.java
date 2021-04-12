package aa;

import by.Segment;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import j1.ClusterRequest;

public class TopicTreeReplicateSegmentRequest
        implements ClusterRequest {
    private final ImmutableSet<Segment> segments;
    
    public TopicTreeReplicateSegmentRequest(@NotNull ImmutableSet<Segment> segments) {
        Preconditions.checkNotNull(segments, "Segments must not be null");
        this.segments = segments;
    }

    @NotNull
    public ImmutableSet<Segment> getSegments() {
        return segments;
    }
}
