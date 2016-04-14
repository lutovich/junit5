/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit5.discoveryNEW;

import java.lang.reflect.AnnotatedElement;
import java.util.Set;

import org.junit.gen5.engine.TestDescriptor;
import org.junit.gen5.engine.UniqueId;

public interface ElementResolver {

	/**
	 * Return a set of {@linkplain TestDescriptor testDescriptors} that can be resolved by this resolver
	 */
	Set<TestDescriptor> resolve(AnnotatedElement element, TestDescriptor parent);

	boolean canResolveUniqueId(UniqueId.Segment segment, TestDescriptor parent);

	/**
	 * Will only be called if {@linkplain #canResolveUniqueId(UniqueId.Segment, TestDescriptor)} returns true.
	 *
	 * <p>Must return a valid {@linkplain TestDescriptor testDescriptor}.
	 */
	TestDescriptor resolve(UniqueId.Segment segment, TestDescriptor parent, UniqueId uniqueId);
}
