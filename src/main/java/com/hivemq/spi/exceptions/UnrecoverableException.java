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

package com.hivemq.spi.exceptions;

/**
 * An exception which indicates an unrecoverable state.
 * <p>
 * <b>HiveMQ will shut down gracefully when this exception is thrown</b>
 *
 * @author Dominik Obermaier
 */
public class UnrecoverableException extends RuntimeException {

    private final boolean showException;


    public UnrecoverableException() {
        this(true);
    }

    public UnrecoverableException(boolean showException) {

        this.showException = showException;
    }

    public boolean isShowException() {
        return showException;
    }
}
