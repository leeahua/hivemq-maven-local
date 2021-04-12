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

import java.nio.charset.StandardCharsets;

/**
 * The MQTT Connect message
 *
 * @author Dominik Obermaier
 * @since 1.4
 */
public class Connect implements Message {

    private boolean cleanSession;

    private boolean will;

    private QoS willQos;

    private boolean willRetain;

    private boolean passwordRequired;

    private boolean usernameRequired;

    private int keepAliveTimer;

    private String clientIdentifier;

    private String willTopic;

    private byte[] willMessage;

    private String username;

    private byte[] password;

    private boolean bridge;

    //Default version is 3.1 until 3.1.1 gained enough traction
    private ProtocolVersion protocolVersion = ProtocolVersion.MQTTv3_1;

    /**
     * @return if the message uses a clean (= non persistent) session
     */
    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(final boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    /**
     * @return if the Connect message contains a LWT message
     */
    public boolean isWill() {
        return will;
    }

    public void setWill(final boolean will) {
        this.will = will;
    }

    /**
     * @return the QoS level of the LWT message
     */
    public QoS getWillQos() {
        return willQos;
    }

    public void setWillQos(final QoS willQos) {
        this.willQos = willQos;
    }

    /**
     * @return if the LWT message should be retained
     */
    public boolean isWillRetain() {
        return willRetain;
    }

    public void setWillRetain(final boolean willRetain) {
        this.willRetain = willRetain;
    }

    /**
     * @return <code>true</code> if the message contains a password
     */
    public boolean isPasswordRequired() {
        return passwordRequired;
    }

    public void setPasswordRequired(final boolean passwordRequired) {
        this.passwordRequired = passwordRequired;
    }

    /**
     * @return <code>true</code> if the message contains a username
     */
    public boolean isUsernameRequired() {
        return usernameRequired;
    }

    public void setUsernameRequired(final boolean usernameRequired) {
        this.usernameRequired = usernameRequired;
    }

    /**
     * @return the the keep alive value of the Connect message
     */
    public int getKeepAliveTimer() {
        return keepAliveTimer;
    }

    public void setKeepAliveTimer(final int keepAliveTimer) {
        this.keepAliveTimer = keepAliveTimer;
    }

    /**
     * @return the client identifier of the client which issued the Connect message
     */
    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public void setClientIdentifier(final String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
    }

    /**
     * @return the topic of the LWT message
     */
    public String getWillTopic() {
        return willTopic;
    }

    public void setWillTopic(final String willTopic) {
        this.willTopic = willTopic;
    }

    /**
     * @return the payload of the LWT message
     */
    public byte[] getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(final byte[] willMessage) {
        this.willMessage = willMessage;
    }

    /**
     * @return the username or null
     */
    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * @return The password as byte array or null
     */
    public byte[] getPassword() {
        return password;
    }

    /**
     * @return the password as UTF8 encoded String. This method is a convenient
     * method but you should consider using getPassword() instead since MQTT passwords
     * can contain raw bytes
     */
    public String getPasswordAsUTF8String() {
        if (password == null) {
            return "";
        }
        return new String(password, StandardCharsets.UTF_8);
    }

    public void setPassword(final byte[] password) {
        this.password = password;
    }

    public void setPassword(final String password) {
        this.password = password.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isBridge() {
        return bridge;
    }

    public void setBridge(final boolean bridge) {
        this.bridge = bridge;
    }

    /**
     * @return the protocol version used in the Connect message
     */
    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(final ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
}
