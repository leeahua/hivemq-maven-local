package av1;

import av.PersistenceConfigurationService.PersistenceMode;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class MetadataSerializer {
    public static final int SERIALIZER_ID = 1;
    private final Kryo kryo = new Kryo();

    public MetadataSerializer() {
        this.kryo.register(MetaInformation.class, new MetaInformationSerializer(), SERIALIZER_ID);
    }

    public byte[] write(MetaInformation metaInformation) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Output output = new Output(outputStream);
        this.kryo.writeObject(output, metaInformation);
        output.flush();
        byte[] metadata = outputStream.toByteArray();
        output.close();
        return metadata;
    }

    public MetaInformation read(byte[] metadata) {
        return this.kryo.readObject(new Input(new ByteArrayInputStream(metadata)),
                MetaInformation.class);
    }

    private static class MetaInformationSerializer extends Serializer<MetaInformation> {
        @Override
        public void write(Kryo kryo, Output output, MetaInformation object) {
            output.writeString(object.getHivemqVersion());
            output.writeString(object.getClientSessionPersistenceVersion());
            output.writeString(object.getIncomingMessageFlowPersistenceVersion());
            output.writeString(object.getOutgoingMessageFlowPersistenceVersion());
            output.writeString(object.getQueuedMessagesPersistenceVersion());
            output.writeString(object.getRetainedMessagesPersistenceVersion());
            output.writeString(object.getSubscriptionPersistenceVersion());
            output.writeInt(covertMode(object.getClientSessionPersistenceMode()));
            output.writeInt(covertMode(object.getIncomingMessageFlowPersistenceMode()));
            output.writeInt(covertMode(object.getOutgoingMessageFlowPersistenceMode()));
            output.writeInt(covertMode(object.getQueuedMessagesPersistenceMode()));
            output.writeInt(covertMode(object.getRetainedMessagesPersistenceMode()));
            output.writeInt(covertMode(object.getSubscriptionPersistenceMode()));
        }

        @Override
        public MetaInformation read(Kryo kryo, Input input, Class<MetaInformation> type) {
            String hivemqVersion = input.readString();
            String clientSessionPersistenceVersion = input.readString();
            String incomingMessageFlowPersistenceVersion = input.readString();
            String outgoingMessageFlowPersistenceVersion = input.readString();
            String queuedMessagesPersistenceVersion = input.readString();
            String retainedMessagesPersistenceVersion = input.readString();
            String subscriptionPersistenceVersion = input.readString();
            PersistenceMode clientSessionPersistenceMode = covertMode(input.readInt());
            PersistenceMode incomingMessageFlowPersistenceMode = covertMode(input.readInt());
            PersistenceMode outgoingMessageFlowPersistenceMode = covertMode(input.readInt());
            PersistenceMode queuedMessagesPersistenceMode = covertMode(input.readInt());
            PersistenceMode retainedMessagesPersistenceMode = covertMode(input.readInt());
            PersistenceMode subscriptionPersistenceMode = covertMode(input.readInt());
            MetaInformation metaInformation = new MetaInformation();
            metaInformation.setHivemqVersion(hivemqVersion);
            metaInformation.setClientSessionPersistenceVersion(clientSessionPersistenceVersion);
            metaInformation.setIncomingMessageFlowPersistenceVersion(incomingMessageFlowPersistenceVersion);
            metaInformation.setOutgoingMessageFlowPersistenceVersion(outgoingMessageFlowPersistenceVersion);
            metaInformation.setQueuedMessagesPersistenceVersion(queuedMessagesPersistenceVersion);
            metaInformation.setRetainedMessagesPersistenceVersion(retainedMessagesPersistenceVersion);
            metaInformation.setSubscriptionPersistenceVersion(subscriptionPersistenceVersion);
            metaInformation.setClientSessionPersistenceMode(clientSessionPersistenceMode);
            metaInformation.setIncomingMessageFlowPersistenceMode(incomingMessageFlowPersistenceMode);
            metaInformation.setOutgoingMessageFlowPersistenceMode(outgoingMessageFlowPersistenceMode);
            metaInformation.setQueuedMessagesPersistenceMode(queuedMessagesPersistenceMode);
            metaInformation.setRetainedMessagesPersistenceMode(retainedMessagesPersistenceMode);
            metaInformation.setSubscriptionPersistenceMode(subscriptionPersistenceMode);
            metaInformation.setMetadataFileExists(true);
            metaInformation.setPersistenceFolderExists(true);
            metaInformation.setDataFolderExists(true);
            return metaInformation;
        }


        private int covertMode(PersistenceMode mode) {
            if (mode == null) {
                return 0;
            }
            switch (mode) {
                case IN_MEMORY:
                    return 1;
                case FILE:
                    return 2;
            }
            throw new IllegalArgumentException("Unknown persistence mode");
        }

        private PersistenceMode covertMode(int mode) {
            switch (mode) {
                case 0:
                    return null;
                case 1:
                    return PersistenceMode.IN_MEMORY;
                case 2:
                    return PersistenceMode.FILE;
            }
            throw new IllegalArgumentException("Unknown persistence mode");
        }
    }
}
