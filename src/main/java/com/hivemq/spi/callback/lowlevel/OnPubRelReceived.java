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
import com.hivemq.spi.message.PubRel;
import com.hivemq.spi.security.ClientData;

/**
 * The OnPubRelReceived Callback gets called after HiveMQ receives a PubRel
 * message from a client.
 *
 * @author Christian Goetz
 * @since 1.4
 */
public interface OnPubRelReceived extends AsynchronousCallback, LowlevelCallback {

    /**
     * This method gets called when a PubRel message is received from a client. It's not possible
     * to interfere with HiveMQ and this callback is for information purposes
     *
     * @param pubRel     the PubRel message which was received by HiveMQ
     * @param clientData the ClientData for the client which sent the PubRel message
     */
    void onPubrelReceived(PubRel pubRel, ClientData clientData);
}
