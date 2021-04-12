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

package com.hivemq.spi.callback.lowlevel;

import com.hivemq.spi.callback.AsynchronousCallback;
import com.hivemq.spi.callback.LowlevelCallback;
import com.hivemq.spi.message.ConnAck;
import com.hivemq.spi.security.ClientData;

/**
 * The OnConnAckSend Callback gets called when the broker sent
 * a ConnAck message to a client
 *
 * @author Christian Goetz
 * @since 1.4
 */
public interface OnConnAckSend extends AsynchronousCallback, LowlevelCallback {

    /**
     * This method gets called after a ConnAck message was sent to the client.
     * <p>
     * This method does not allow to interfere with the ConnAck message and is just
     * for information that this ConnAck message was sent.
     *
     * @param connAck    the ConnAck message
     * @param clientData the clientData
     */
    void onConnackSend(ConnAck connAck, ClientData clientData);
}
