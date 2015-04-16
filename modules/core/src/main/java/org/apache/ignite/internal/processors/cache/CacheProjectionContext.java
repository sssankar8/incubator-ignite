/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache;

import org.apache.ignite.internal.util.tostring.*;
import org.apache.ignite.internal.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import javax.cache.expiry.*;
import java.io.*;
import java.util.*;

/**
 * Cache projection context.
 */
public class CacheProjectionContext implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Skip store. */
    @GridToStringInclude
    private boolean skipStore;

    /** Client ID which operates over this projection, if any, */
    private UUID subjId;

    /** */
    private boolean keepPortable;

    /** */
    private ExpiryPolicy expiryPlc;

    /**
     * Empty constructor required for {@link Externalizable}.
     */
    public CacheProjectionContext() {
        // No-op.
    }

    /**
     * @param skipStore Skip store flag.
     * @param subjId Subject ID.
     * @param keepPortable Keep portable flag.
     * @param expiryPlc Expiry policy.
     */
    public CacheProjectionContext(
        boolean skipStore,
        @Nullable UUID subjId,
        boolean keepPortable,
        @Nullable ExpiryPolicy expiryPlc) {

        this.skipStore = skipStore;

        this.subjId = subjId;

        this.keepPortable = keepPortable;

        this.expiryPlc = expiryPlc;
    }

    /**
     * @return Keep portable flag.
     */
    public boolean isKeepPortable() {
        return keepPortable;
    }

    /**
     * @return {@code True} if portables should be deserialized.
     */
    public boolean deserializePortables() {
        return !keepPortable;
    }

    /** {@inheritDoc} */
    public CacheProjectionContext keepPortable() {
        return new CacheProjectionContext(
            skipStore,
            subjId,
            true,
            expiryPlc);
    }

    /**
     * Gets client ID for which this projection was created.
     *
     * @return Client ID.
     */
    @Nullable public UUID subjectId() {
        return subjId;
    }

    /** {@inheritDoc} */
    public CacheProjectionContext forSubjectId(UUID subjId) {
        A.notNull(subjId, "subjId");

        return new CacheProjectionContext(
            skipStore,
            subjId,
            keepPortable,
            expiryPlc);
    }

    /** {@inheritDoc} */
    public boolean skipStore() {
        return skipStore;
    }

    /** {@inheritDoc} */
    public CacheProjectionContext setSkipStore(boolean skipStore) {
        return new CacheProjectionContext(
            skipStore,
            subjId,
            keepPortable,
            expiryPlc);
    }

    /** {@inheritDoc} */
    @Nullable public ExpiryPolicy expiry() {
        return expiryPlc;
    }

    /** {@inheritDoc} */
    public CacheProjectionContext withExpiryPolicy(ExpiryPolicy plc) {
        return new CacheProjectionContext(
            skipStore,
            subjId,
            true,
            plc);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(CacheProjectionContext.class, this);
    }
}