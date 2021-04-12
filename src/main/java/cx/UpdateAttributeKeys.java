package cx;

import com.hivemq.update.entity.UpdateResponse;
import io.netty.util.AttributeKey;

public interface UpdateAttributeKeys {
    AttributeKey<UpdateResponse> RESPONSE = AttributeKey.valueOf("response");
}
