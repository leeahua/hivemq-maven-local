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

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.List;

/**
 * The MQTT SubAck message
 *
 * @author Dominik Obermaier
 * @since 1.4
 */
public class SubAck extends MessageWithId {

    public static final byte QoS_0 = 0x00;
    public static final byte QoS_1 = 0x01;
    public static final byte QoS_2 = 0x02;
    public static final byte FAILURE = (byte) 0x80;


    private List<Byte> grantedQos;


    public SubAck(final int messageId, final Byte... entries) {
        this(messageId, Arrays.asList(entries));
    }

    public SubAck(final int messageId, final List<Byte> grantedQos) {
        super(messageId);
        Preconditions.checkArgument(messageId > 0 && messageId <= 65535);
        this.grantedQos = grantedQos;
    }

    public List<Byte> getGrantedQos() {
        return grantedQos;
    }
}
