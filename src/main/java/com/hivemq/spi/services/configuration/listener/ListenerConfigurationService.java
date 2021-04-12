/*
 * Copyright 2015 dc-square GmbH
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

package com.hivemq.spi.services.configuration.listener;

import com.hivemq.spi.annotations.ReadOnly;
import com.hivemq.spi.services.configuration.entity.Listener;
import com.hivemq.spi.services.configuration.entity.TcpListener;
import com.hivemq.spi.services.configuration.entity.TlsTcpListener;
import com.hivemq.spi.services.configuration.entity.TlsWebsocketListener;
import com.hivemq.spi.services.configuration.entity.WebsocketListener;
import com.hivemq.spi.services.configuration.exception.ConfigurationValidationException;

import java.util.List;

/**
 * The service which allows to inspect Listener configuration at runtime.
 * <p>
 * It's also possible to add new listeners at runtime.
 *
 * @author Dominik Obermaier
 * @since 3.0
 */
public interface ListenerConfigurationService {


    /**
     * Adds a new Listener at runtime
     *
     * @param listener the listener
     * @param <T>      the concrete listener subclass
     * @throws ConfigurationValidationException if the validation of the listener wasn't successful
     * @throws IllegalArgumentException
     */
    <T extends Listener> void addListener(final T listener) throws ConfigurationValidationException, IllegalArgumentException;

    /**
     * @return a unmodifiable list of all active listeners
     */
    @ReadOnly
    List<Listener> getListeners();

    /**
     * @return a unmodifiable list of all active TCP listeners
     */
    @ReadOnly
    List<TcpListener> getTcpListeners();

    /**
     * @return a unmodifiable list of all active TLS listeners
     */
    @ReadOnly
    List<TlsTcpListener> getTlsTcpListeners();

    /**
     * @return a unmodifiable list of all active Websocket listeners
     */
    @ReadOnly
    List<WebsocketListener> getWebsocketListeners();

    /**
     * @return a unmodifiable list of all active TLS Websocket listeners
     */
    @ReadOnly
    List<TlsWebsocketListener> getTlsWebsocketListeners();


}
