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

import com.hivemq.spi.callback.SynchronousCallback;
import com.hivemq.spi.callback.exception.InvalidSubscriptionException;
import com.hivemq.spi.callback.security.OnAuthorizationCallback;
import com.hivemq.spi.message.Subscribe;
import com.hivemq.spi.security.ClientData;

/**
 * Gets called when a {@link Subscribe} message arrives
 * <p>
 * When a subscription was invalid it's possible to throw a
 * {@link InvalidSubscriptionException}.
 * <p>
 * Please note that the recommended way to handle Topic Permissions is
 * to use the {@link OnAuthorizationCallback}.
 *
 * @author Dominik Obermaier
 * @since 1.4
 */
public interface OnSubscribeCallback extends SynchronousCallback {


    /**
     * Called when a {@link Subscribe} message arrives.
     *
     * @param message    the Subscribe message
     * @param clientData the information about the client
     * @throws InvalidSubscriptionException when a subscription was invalid
     */
    void onSubscribe(Subscribe message, ClientData clientData) throws InvalidSubscriptionException;
}
