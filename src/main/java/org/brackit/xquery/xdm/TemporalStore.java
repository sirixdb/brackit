package org.brackit.xquery.xdm;

import org.brackit.xquery.node.parser.SubtreeParser;

public interface TemporalStore {
	public TemporalCollection<?> lookup(String name) throws DocumentException;

	public TemporalCollection<?> create(String name) throws DocumentException;

	public TemporalCollection<?> create(String name, SubtreeParser parser)
			throws DocumentException;

	public TemporalCollection<?> create(String name, Stream<SubtreeParser> parsers)
			throws DocumentException;

	public void drop(String name) throws DocumentException;

	public void makeDir(String path) throws DocumentException;
}
