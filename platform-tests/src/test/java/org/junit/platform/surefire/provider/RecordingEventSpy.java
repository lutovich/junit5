/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.platform.surefire.provider;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.MavenExecutionResult;

public class RecordingEventSpy implements EventSpy {

	private final List<MavenExecutionResult> executionResults = new CopyOnWriteArrayList<>();

	@Override
	public void init(Context context) throws Exception {
	}

	@Override
	public void onEvent(Object event) throws Exception {
		if (event instanceof MavenExecutionResult) {
			executionResults.add((MavenExecutionResult) event);
		}
	}

	@Override
	public void close() throws Exception {
	}

	public int testExecutionResultsCount() {
		return executionResults.size();
	}

	public boolean testExecutionHasExceptions(int index) {
		return executionResults.get(index).hasExceptions();
	}
}
