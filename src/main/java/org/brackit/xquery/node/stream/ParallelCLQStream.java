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
package org.brackit.xquery.node.stream;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Stream;

/**
 * 
 * @author Sebastian Baechle
 * 
 * @param <E>
 */
public class ParallelCLQStream<E> implements Stream<E> {

	private final Stream<? extends E> stream;

	private final ConcurrentLinkedQueue<E> queue;

	private volatile boolean finished;

	private volatile DocumentException error;

	private int outerRetry;

	private int innerRetry;

	private int takes;

	public ParallelCLQStream(Stream<? extends E> stream) {
		super();
		this.stream = stream;
		this.queue = new ConcurrentLinkedQueue<E>();
		this.finished = false;

		final Stream<? extends E> s = stream;

		new Thread() {
			public void run() {
				System.out.println("internal starting");
				try {
					E next;
					while ((next = s.next()) != null) {
						do {
							if (queue.offer(next)) {
								break;
							}
							innerRetry++;
						} while (!finished);
					}
					finished = true;
					s.close();
				} catch (DocumentException e) {
					error = e;
					finished = true;
				}
				System.out.println("internal stopping");
			}
		}.start();
	}

	@Override
	public void close() {
		finished = true;
		System.out.println("Inner retry " + innerRetry);
		System.out.println("Outer retry " + outerRetry);
		System.out.println("Takes: " + takes);
	}

	@Override
	public E next() throws DocumentException {
		DocumentException deliverError = error; // volatile read

		if (deliverError != null) {
			error = null;
			throw deliverError;
		}

		E current;
		do {
			current = queue.poll();
			if (current != null) {
				break;
			}
			outerRetry++;
		} while (!finished);

		takes++;
		return current;
	}
}
