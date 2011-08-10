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
package org.brackit.xquery.operator;

import java.util.ArrayList;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class BlockingParallelizer implements Operator {
	private static class BlockingParallelizerCursor implements Cursor {
		private final Cursor c;

		private final QueryContext ctx;

		private volatile boolean finished;

		private volatile QueryException error;

		// private int takes;
		//	
		// private int enqueueRetries;
		//	
		// private int dequeueRetries;

		private Tuple current;

		private Tuple[] currentBuffer;

		private Tuple[][] queue;

		private int start;

		private int end;

		private int pos = 0;

		private long producerBlock;

		private long consumerBlock;

		private ArrayList<Long> firstConsumerBlock = new ArrayList<Long>();

		BlockingParallelizerCursor(Cursor c, QueryContext ctx) {
			this.c = c;
			this.ctx = ctx;
		}

		@Override
		public void open(QueryContext ctx) throws QueryException {
			int noOfBuffers = 3;
			this.queue = new Tuple[noOfBuffers][1000];
			this.finished = false;
			start = noOfBuffers - 1;
			end = 0;
			new Thread() {
				public void run() {
					// System.out.println("internal starting");
					fill();
					// System.out.println("internal stopping");
				}
			}.start();
		}

		private void fill() {
			try {
				c.open(ctx);
				int pos = 0;
				Object[] buffer = queue[0];
				int length = buffer.length;
				Tuple t;

				while ((t = c.next(ctx)) != null) {
					buffer[pos++] = t;

					if (pos == length) {
						if (finished) {
							break;
						}

						// offer filled and take next
						buffer = enqueue();
						length = buffer.length;
						pos = 0;
					}
				}
				enqueue();
				finished = true;
			} catch (QueryException e) {
				error = e;
				finished = true;
			} finally {
				c.close(ctx);
			}
		}

		private synchronized Tuple[] enqueue() {
			int newQueueEnd = (end + 1) % queue.length;

			while (newQueueEnd == start) {
				// System.out.println("ENQUEUE: WAITING FOR " + newQueueEnd);
				long start = System.currentTimeMillis();
				try {
					wait();
				} catch (InterruptedException e) {
				}
				long end = System.currentTimeMillis();
				producerBlock += (end - start);
				// enqueueRetries++;
			}

			// take one from queue
			end = newQueueEnd;
			// System.out.println("ENQUEUED: " + newQueueEnd);
			notifyAll();
			return queue[newQueueEnd];
		}

		private synchronized Tuple[] dequeue() throws QueryException {
			QueryException deliverError = error; // volatile read

			if (deliverError != null) {
				error = null;
				throw deliverError;
			}

			int newQueueStart = (start + 1) % queue.length;

			while (newQueueStart == end) {
				// System.out.println("DEQUEUE: WAITING FOR " + newQueueStart);
				long start = System.currentTimeMillis();
				try {
					wait();
				} catch (InterruptedException e) {
				}
				long end = System.currentTimeMillis();

				// firstConsumerBlock.add(end - start);
				consumerBlock += (end - start);
				// dequeueRetries++;
			}

			// take one from queue
			start = newQueueStart;
			// System.out.println("DEQUEUED: " + newQueueStart);
			notifyAll();
			return queue[newQueueStart];
		}

		@Override
		public void close(QueryContext ctx) {
			finished = true;
			// System.out.println("Producer Block: " + producerBlock);
			// System.out.println("Consumer Block: " + consumerBlock);
			// System.out.println("Consumer Block: " + firstConsumerBlock);
			// System.out.println("Takes: " + takes);
			// System.out.println("EnqueueRetries: " + enqueueRetries);
			// System.out.println("DequeueRetries: " + dequeueRetries);
		}

		@Override
		public Tuple next(QueryContext ctx) throws QueryException {
			if ((currentBuffer == null) || (pos == currentBuffer.length)) {
				currentBuffer = dequeue();
				pos = 0;
			}

			current = currentBuffer[pos];
			currentBuffer[pos++] = null;

			if (current == null) {
				return null;
			}

			// takes++;
			Tuple deliver = current;
			current = null;
			return deliver;
		}
	}

	private final Operator in;

	public BlockingParallelizer(Operator in) {
		this.in = in;
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
		return new BlockingParallelizerCursor(in.create(ctx, tuple), ctx);
	}	
	
	@Override
	public int tupleWidth(int initSize) {
		return in.tupleWidth(initSize);
	}
}
