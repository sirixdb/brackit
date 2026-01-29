/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Cost model framework for query optimization.
 *
 * <p>This package provides interfaces and implementations for estimating
 * the cost of query execution plans, enabling cost-based query optimization.</p>
 *
 * <h2>Key Components</h2>
 * <ul>
 * <li>{@link io.brackit.query.compiler.optimizer.cost.CostModel} - Interface for cost estimation</li>
 * <li>{@link io.brackit.query.compiler.optimizer.cost.OperatorContext} - Context for operator cost estimation</li>
 * <li>{@link io.brackit.query.compiler.optimizer.cost.SimpleCostModel} - Default implementation</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * CostModel costModel = new SimpleCostModel();
 * OperatorContext ctx = OperatorContext.acquire();
 * ctx.forJoin(leftCard, rightCard, joinSelectivity);
 * double cost = costModel.estimateCost(XQ.Join, ctx);
 * }</pre>
 */
package io.brackit.query.compiler.optimizer.cost;
