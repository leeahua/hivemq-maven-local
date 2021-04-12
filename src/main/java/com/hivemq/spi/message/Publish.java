/*
 * Copyright 2014 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.spi.message;

/**
 * A MQTT Publish message
 * <p>
 * <p>
 * Note that a Publish message is considered equal if the topic Strings are equal
 *
 * @author Dominik Obermaier
 * @since 1.4
 */
public class Publish extends MessageWithId {

    private byte[] payload;

    private String topic;

    private boolean duplicateDelivery;

    private boolean retain;

    private QoS qoS;

    public Publish() {
    }

    public Publish(final byte[] payload, final String topic, final QoS qoS) {
        this.payload = payload;
        this.topic = topic;
        this.qoS = qoS;
    }

    /**
     * @return the payload of the Publish message
     */
    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(final byte[] payload) {
        this.payload = payload;
    }

    /**
     * @return the topic of the Publish message
     */
    public String getTopic() {
        return topic;
    }

    public void setTopic(final String topic) {
        this.topic = topic;
    }

    /**
     * @return <code>true</code> if the message is a duplicate message
     */
    public boolean isDuplicateDelivery() {
        return duplicateDelivery;
    }

    public void setDuplicateDelivery(final boolean duplicateDelivery) {
        this.duplicateDelivery = duplicateDelivery;
    }

    /**
     * @return if the Publish message should be retained
     */
    public boolean isRetain() {
        return retain;
    }

    public void setRetain(final boolean retain) {
        this.retain = retain;
    }

    /**
     * @return the QoS level of the Publish message
     */
    public QoS getQoS() {
        return qoS;
    }

    public void setQoS(final QoS qoS) {
        this.qoS = qoS;
    }

    /**
     * Crates a deep copy of a {@link Publish} object.
     * <p>
     * Use this method if you want to reuse a publish received by a callback to prevent side effects.
     *
     * @param original the original Publish message
     * @return a deep copy of the original Publish message
     */
    public static Publish copy(final Publish original) {
        final Publish publish = new Publish();
        publish.setQoS(original.getQoS());
        publish.setRetain(original.isRetain());
        publish.setPayload(original.getPayload());
        publish.setTopic(original.getTopic());
        publish.setDuplicateDelivery(original.isDuplicateDelivery());
        publish.setMessageId(original.getMessageId());
        return publish;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Publish)) return false;

        final Publish publish = (Publish) o;

        if (messageId != publish.messageId) return false;
        if (topic != null ? !topic.equals(publish.topic) : publish.topic != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = topic != null ? topic.hashCode() : 0;
        result = 31 * result + messageId;
        return result;
    }
}
