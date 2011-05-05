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
package org.brackit.xquery.compiler.optimizer.expr;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.IntegerNumeric;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class ParallelExpr implements Expr {
	final Expr expr;

	public ParallelExpr(Expr expr) {
		this.expr = expr;
	}

	public static class ParallelArrayBlockIter implements Iter {
		final Iter in;

		volatile boolean finished;

		volatile QueryException error;

		public int takes;
		//	
		public int enqueueRetries;
		//	
		public int dequeueRetries;

		Item current;

		Item[] currentBuffer;

		Item[][] freeQueue;

		volatile int freeQueueStart;

		volatile int freeQueueEnd;

		int pos = 0;

		public ParallelArrayBlockIter(Iter stream) {
			super();
			this.in = stream;
			int noOfBuffers = 3;
			this.freeQueue = new Item[noOfBuffers][120];
			this.finished = false;
			freeQueueStart = noOfBuffers - 1;
			freeQueueEnd = 0;

			new Thread() {
				public void run() {
					// System.out.println("internal starting");
					fill();
					// System.out.println("internal stopping");
				}
			}.start();
		}

		void fill() {
			try {
				int pos = 0;
				Item[] buffer = freeQueue[0];
				int length = buffer.length;
				Item next;

				while ((next = in.next()) != null) {
					buffer[pos++] = next;

					if (pos == length) {
						// offer filled and take next
						buffer = enqueue();
						length = buffer.length;
						pos = 0;
					}
				}
				enqueue();
				finished = true;
				in.close();
			} catch (QueryException e) {
				error = e;
				finished = true;
			}
		}

		Item[] enqueue() {
			int queueStart = freeQueueStart; // volatile read
			int queueEnd = freeQueueEnd; // volatile read

			int newQueueEnd = (queueEnd + 1) % freeQueue.length;

			while (newQueueEnd == queueStart) {
				// spin until one more free
				queueStart = freeQueueStart; // volatile read
				enqueueRetries++;
			}

			// take one from queue
			freeQueueEnd = newQueueEnd;
			return freeQueue[newQueueEnd];
		}

		Item[] dequeue() {
			int queueStart = freeQueueStart; // volatile read
			int queueEnd = freeQueueEnd; // volatile read

			int newQueueStart = (queueStart + 1) % freeQueue.length;

			while (newQueueStart == queueEnd) {
				// spin until one more free
				queueEnd = freeQueueEnd; // volatile read
				dequeueRetries++;
			}

			// take one from queue
			freeQueueStart = newQueueStart;
			return freeQueue[newQueueStart];
		}

		@Override
		public void close() {
			finished = true;
			// System.out.println("Takes: " + takes);
			// System.out.println("EnqueueRetries: " + enqueueRetries);
			// System.out.println("DequeueRetries: " + dequeueRetries);
		}

		public boolean hasNext() throws QueryException {
			QueryException deliverError = error; // volatile read

			if (deliverError != null) {
				error = null;
				throw deliverError;
			}

			if ((currentBuffer == null) || (pos == currentBuffer.length)) {
				currentBuffer = dequeue();
				pos = 0;
			}

			current = currentBuffer[pos];
			currentBuffer[pos++] = null;

			return (current != null);
		}

		@Override
		public Item next() throws QueryException {
			QueryException deliverError = error; // volatile read

			if (deliverError != null) {
				error = null;
				throw deliverError;
			}

			if ((current == null) && (!hasNext())) {
				return null;
			}

			takes++;

			Item deliver = current;
			current = null;
			return deliver;
		}
	}

	@Override
	public Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		final Sequence s = expr.evaluate(ctx, tuple);

		if ((s == null) || (s instanceof Item)) {
			return s;
		}

		return new Sequence() {
			@Override
			public IntegerNumeric size(QueryContext ctx) throws QueryException {
				return s.size(ctx);
			}

			@Override
			public Iter iterate() {
				return new ParallelArrayBlockIter(s.iterate());
			}

			@Override
			public boolean booleanValue(QueryContext ctx) throws QueryException {
				return s.booleanValue(ctx);
			}
		};
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return expr.evaluateToItem(ctx, tuple);
	}

	@Override
	public boolean isUpdating() {
		return expr.isUpdating();
	}

	@Override
	public boolean isVacuous() {
		return expr.isVacuous();
	}
}
