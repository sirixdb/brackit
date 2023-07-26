package io.brackit.query.jdm.node;

import io.brackit.query.node.AbstractNode;

/**
 * Adds temporal methods.
 *
 * @author Johannes Lichtenberger
 */
public abstract class AbstractTemporalNode<E extends AbstractTemporalNode<E>> extends AbstractNode<AbstractTemporalNode<E>>
    implements TemporalNode<AbstractTemporalNode<E>> {
}
