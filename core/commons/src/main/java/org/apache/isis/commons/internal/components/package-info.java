/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

/**
 * Defines a {@link org.apache.isis.commons.internal.components.Component}
 * architecture, along with factories (called {@link org.apache.isis.core.commons.components.Installer}s)
 * to install (or create) those components.
 *
 * <p>
 * There are three subinterfaces of
 * {@link org.apache.isis.commons.internal.components.Component}, for three different
 * scopes:
 * <ul>
 * <li><p> {@link org.apache.isis.commons.internal.components.ApplicationScopedComponent application-scoped} </p></li>
 * <li><p> {@link org.apache.isis.commons.internal.components.SessionScopedComponent session-scoped} </p></li>
 * <li><p> {@link org.apache.isis.commons.internal.components.TransactionScopedComponent transaction-scoped} </p></li>
 * </ul>
 *
 * <p>
 * Many {@link org.apache.isis.commons.internal.components.Component}s may also be injectable,
 * meaning that they know hot to inject themselves to a candidate object &quot;if appropriate&quot;.
 * The convention adopted in most cases is for an object requiring injection of
 * component <tt>Xxx</tt> to implement an <tt>XxxAware</tt> interface that
 * defines a <tt>setXxx(Xxx)</tt> method.
 */
package org.apache.isis.commons.internal.components;