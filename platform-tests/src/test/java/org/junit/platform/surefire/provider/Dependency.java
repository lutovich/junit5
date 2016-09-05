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

public class Dependency {

	private final String groupId;
	private final String artifactId;
	private final String version;
	private final String path;

	public Dependency(String groupId, String artifactId, String version, String path) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.path = path;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getPath() {
		return path;
	}
}
