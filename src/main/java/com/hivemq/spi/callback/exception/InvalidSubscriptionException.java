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

package com.hivemq.spi.callback.exception;

import com.hivemq.spi.message.Subscribe;

/**
 * An exception which can be thrown when one or more subscriptions
 * in the {@link Subscribe} message was invalid
 *
 * @author Dominik Obermaier
 * @since 1.4
 */
public class InvalidSubscriptionException extends Exception {

    public InvalidSubscriptionException() {
        super();
    }

    public InvalidSubscriptionException(final String message) {
        super(message);
    }

    public InvalidSubscriptionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InvalidSubscriptionException(final Throwable cause) {
        super(cause);
    }
}
