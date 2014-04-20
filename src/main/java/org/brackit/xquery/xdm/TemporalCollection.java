package org.brackit.xquery.xdm;

public interface TemporalCollection<E extends Node<E>> extends Collection<E> {

	public E getDocument(int revision) throws DocumentException;
	
	public E getDocument(int revision, boolean updatabale) throws DocumentException;

	public E getDocument(int revision, String name) throws DocumentException;
	
	public E getDocument(int revision, String name, boolean updatable) throws DocumentException;
	
	public E getDocument(String name) throws DocumentException;
}
