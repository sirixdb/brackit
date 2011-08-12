/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.brackit.xquery.atomic.AbstractTimeInstant;
import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.DTD;
import org.brackit.xquery.atomic.Date;
import org.brackit.xquery.atomic.DateTime;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Time;
import org.brackit.xquery.node.SimpleStore;
import org.brackit.xquery.node.d2linked.D2NodeFactory;
import org.brackit.xquery.update.UpdateList;
import org.brackit.xquery.update.op.UpdateOp;
import org.brackit.xquery.xdm.Collection;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.NodeFactory;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Store;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class QueryContext {
	protected static final NodeFactory<?> FACTORY = new D2NodeFactory();

	protected static final Store STORE = null;

	private final NodeFactory<?> factory;

	private final Store store;

	private UpdateList updates;

	private Item defaultItem;

	private IntNumeric defaultSize;

	private IntNumeric defaultPos;

	private Map<QNm, Sequence> externalVars;

	private Node<?> defaultDocument;

	private Collection<?> defaultCollection;

	private DateTime dateTime;

	private Date date;

	private Time time;

	private DTD implicitTimezone = AbstractTimeInstant.LOCAL_TIMEZONE;

	public QueryContext() {
		this.factory = FACTORY;
		this.store = new SimpleStore();
	}

	public QueryContext(Store store) {
		this.factory = FACTORY;
		this.store = store;
	}

	public List<UpdateOp> getPendingUpdates() {
		return (updates == null) ? null : updates.list();
	}

	public synchronized void addPendingUpdate(UpdateOp op) {
		if (updates == null) {
			updates = new UpdateList();
		}
		updates.append(op);
	}

	public void applyUpdates() throws QueryException {
		if (updates != null) {
			updates.apply(this);
		}
	}

	public void bind(QNm name, Sequence sequence) {
		if (externalVars == null) {
			externalVars = new HashMap<QNm, Sequence>(3);
		}
		externalVars.put(name, sequence);
	}

	public Sequence resolve(QNm name) throws QueryException {
		return (externalVars != null) ? externalVars.get(name) : null;
	}

	public boolean isBound(QNm name) {
		return ((externalVars != null) && (externalVars.containsKey(name)));
	}

	public void setDefaultContext(Item item, IntNumeric position,
			IntNumeric size) throws QueryException {
		if (item == null) {
			if ((position != null) || (size != null)) {
				throw new QueryException(
						ErrorCode.BIT_DYN_INT_ERROR,
						"Illegal default context: $fs:dot=%s $fs:position=%s $fs:last=%s",
						item, position, size);
			}
		} else if ((position == null) || (size == null)
				|| (position.cmp(Int32.ONE) < 0) || (size.cmp(Int32.ONE) < 0)
				|| (position.cmp(size) > 0)) {
			throw new QueryException(
					ErrorCode.BIT_DYN_INT_ERROR,
					"Illegal default context: $fs:dot=%s $fs:position=%s $fs:last=%s",
					item, position, size);
		}
		defaultItem = item;
		defaultPos = position;
		defaultSize = size;
	}

	public Item getItem() {
		return defaultItem;
	}

	public IntNumeric getPosition() {
		return defaultPos;
	}

	public IntNumeric getSize() {
		return defaultSize;
	}

	public Node<?> getDefaultDocument() {
		return defaultDocument;
	}

	public void setDefaultDocument(Node<?> defaultDocument) {
		this.defaultDocument = defaultDocument;
	}

	public Collection<?> getDefaultCollection() {
		return defaultCollection;
	}

	public void setDefaultCollection(Collection<?> defaultCollection) {
		this.defaultCollection = defaultCollection;
	}

	public DateTime getDateTime() {
		return (dateTime != null) ? dateTime : (dateTime = new DateTime(
				implicitTimezone));
	}

	public Date getDate() {
		return (date != null) ? date : (date = new Date(getDateTime()));
	}

	public Time getTime() {
		return (time != null) ? time : (time = new Time(getDateTime()));
	}

	public DTD getImplicitTimezone() {
		return implicitTimezone;
	}

	public AnyURI getBaseUri() {
		return AnyURI.EMPTY;
	}

	public NodeFactory<?> getNodeFactory() {
		return factory;
	}

	public Store getStore() {
		return store;
	}
}
