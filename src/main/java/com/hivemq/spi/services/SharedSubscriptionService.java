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

package com.hivemq.spi.services;

import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ReadOnly;
import com.hivemq.spi.annotations.ThreadSafe;

import java.util.List;

/**
 * A service which allows read, add and remove shared subscriptions.
 * <p>
 * All implementations of the SharedSubscriptionService are guaranteed to be thread safe.
 *
 * @author Dominik Obermaier
 * @since 3.0
 * @deprecated Since 3.1 Shared subscriptions are created by using a specific marker in the topic.
 * Use {@link BlockingSubscriptionStore} or {@link AsyncSubscriptionStore} to create such subscriptions.
 * See the HiveMQ documentation about shared subscriptions for more detailed information.
 */
@ThreadSafe
@Deprecated
public interface SharedSubscriptionService {

    /**
     * Adds shared subscriptions to the SharedSubscriptionService.
     * <p>
     * Although the contract of this method is to disallow null values, this method is lenient.
     * If <code>null</code> values are passed, these values are ignored.
     *
     * @param sharedSubscriptions the shared subscriptions to add
     * @deprecated Since 3.1 Shared subscriptions are created by using a specific marker in the topic.
     * Use {@link BlockingSubscriptionStore} or {@link AsyncSubscriptionStore} to create such subscriptions.
     * See the HiveMQ documentation about shared subscriptions for more detailed information.
     */
    @Deprecated
    void addSharedSubscriptions(@NotNull final String... sharedSubscriptions);

    /**
     * Returns <code>true</code> if shared subscriptions are available, <code>false</code> otherwise
     *
     * @return <code>true</code> if shared subscriptions are available, <code>false</code> otherwise
     * @deprecated Since 3.1 Shared subscriptions are created by using a specific marker in the topic.
     * Use {@link BlockingSubscriptionStore} or {@link AsyncSubscriptionStore} to create such subscriptions.
     * See the HiveMQ documentation about shared subscriptions for more detailed information.
     */
    @Deprecated
    boolean sharedSubscriptionsAvailable();


    /**
     * Removes a shared subscription from the SharedSubscriptionService.
     * <p>
     * Although the contract of this method is to disallow null values, this method is lenient.
     * If a <code>null</code> value is passed, the value is ignored.
     *
     * @param sharedSubscription the shared subscription to remove
     * @deprecated Since 3.1 Shared subscriptions are created by using a specific marker in the topic.
     * Use {@link BlockingSubscriptionStore} or {@link AsyncSubscriptionStore} to create such subscriptions.
     * See the HiveMQ documentation about shared subscriptions for more detailed information.
     */
    @Deprecated
    void removeSharedSubscription(@NotNull final String sharedSubscription);

    /**
     * Returns all shared subscriptions. The returned List is read-only representation
     * of all shared subscriptions
     *
     * @return a read-only List of all shared subscriptions
     * @deprecated Since 3.1 Shared subscriptions are created by using a specific marker in the topic.
     * Use {@link BlockingSubscriptionStore} or {@link AsyncSubscriptionStore} to create such subscriptions.
     * See the HiveMQ documentation about shared subscriptions for more detailed information.
     */
    @ReadOnly
    @Deprecated
    List<String> getSharedSubscriptions();

    /**
     * Returns the number of all shared subscriptions.
     *
     * @return the number of all shared subscriptions
     * @deprecated Since 3.1 Shared subscriptions are created by using a specific marker in the topic.
     * Use {@link BlockingSubscriptionStore} or {@link AsyncSubscriptionStore} to create such subscriptions.
     * See the HiveMQ documentation about shared subscriptions for more detailed information.
     */
    @Deprecated
    long getSharedSubscriptionsSize();
}
