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

package com.hivemq.spi.callback.events;

import com.hivemq.spi.callback.AsynchronousCallback;
import com.hivemq.spi.message.Publish;
import com.hivemq.spi.security.ClientData;

/**
 * Gets called when an <b>outgoing</b> Publish message event occurs.
 * That means, the callback always gets called when a subscribing client is going
 * to receive a message
 * <p>
 * <b>This callback is called VERY often, so make sure you don't block and use proper caching</b>
 *
 * @author Christian Goetz
 * @since 1.4
 */
public interface OnPublishSend extends AsynchronousCallback {

    /**
     * Gets called when a Publish is sent by HiveMQ to a subscribing client.
     *
     * @param publish    the Publish message
     * @param clientData the information about the client
     */
    void onPublishSend(Publish publish, ClientData clientData);
}
