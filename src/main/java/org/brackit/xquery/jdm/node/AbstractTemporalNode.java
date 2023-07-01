package org.brackit.xquery.jdm.node;

import org.brackit.xquery.node.AbstractNode;

/**
 * Adds temporal methods.
 *
 * @author Johannes Lichtenberger
 */
public abstract class AbstractTemporalNode<E extends AbstractTemporalNode<E>> extends
    AbstractNode<AbstractTemporalNode<E>> implements TemporalNode<AbstractTemporalNode<E>> {
}
