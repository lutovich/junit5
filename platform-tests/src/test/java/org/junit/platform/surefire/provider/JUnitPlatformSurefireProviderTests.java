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

import static java.util.Collections.newSetFromMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.maven.cli.MavenCli;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.extensions.TempDirectory;
import org.junit.jupiter.extensions.TempDirectory.Root;

class JUnitPlatformSurefireProviderTests {

	private static final String MAVEN_PROJECT_DIR_NAME = "test-maven-project";
	private static final String PACKAGE_NAME = "tests";

	private static final List<String> REQUIRED_ARTIFACT_NAMES = Arrays.asList("junit-platform-surefire-provider",
		"junit-jupiter-engine", "junit-jupiter-api", "junit-platform-engine", "junit-platform-commons",
		"junit-platform-launcher", "opentest4j");

	@Test
	@ExtendWith(TempDirectory.class)
	void basicIT(@Root Path tempDir) throws Exception {
		RecordingEventSpy eventSpy = new RecordingEventSpy();
		MavenCli mavenCli = newMavenCli(eventSpy);

		Path mavenProjectDir = createMavenProject(tempDir, "EqualityTest.java");
		String mavenProjectPath = mavenProjectDir.toString();

		System.setProperty("maven.multiModuleProjectDirectory", mavenProjectPath);

		int exitCode = mavenCli.doMain(new String[] { "test" }, mavenProjectPath, System.out, System.out);

		assertEquals(0, exitCode, "exit code");
		assertEquals(1, eventSpy.testExecutionResultsCount(), "execution results");
		assertFalse(eventSpy.testExecutionHasExceptions(0));
	}

	private static Path createMavenProject(Path rootDir, String... javaFileNames) throws Exception {
		List<Dependency> dependencies = findDependencies();

		Path projectDir = rootDir.toAbsolutePath().resolve(MAVEN_PROJECT_DIR_NAME);
		Files.createDirectory(projectDir);

		createPomFile(projectDir, dependencies);

		Path packageDir = projectDir.resolve("src").resolve("test").resolve("java").resolve(PACKAGE_NAME);
		Files.createDirectories(packageDir);

		for (String javaFileName : javaFileNames) {
			Path javaFile = resourceFile(javaFileName);
			Files.copy(javaFile, packageDir.resolve(javaFileName));
		}

		return projectDir;
	}

	private static void createPomFile(Path projectDir, List<Dependency> dependencies) throws IOException {
		Path pom = projectDir.resolve("pom.xml");

		try (BufferedWriter writer = Files.newBufferedWriter(pom)) {
			Template template = Velocity.getTemplate(resourceFile("pom.xml.vm").toString());
			VelocityContext context = new VelocityContext();
			context.put("dependencies", dependencies);
			template.merge(context, writer);
		}
	}

	private static List<Dependency> findDependencies() {
		String classpath = System.getProperty("java.class.path");
		PathMatcher nameMatcher = createNameMatcher();

		List<Dependency> dependencies = Stream.of(classpath.split(File.pathSeparator)).map(Paths::get).filter(
			file -> nameMatcher.matches(file.getFileName())).filter(distinctByName()).map(
				JUnitPlatformSurefireProviderTests::createDependency).collect(toList());

		assertEquals(REQUIRED_ARTIFACT_NAMES.size(), dependencies.size(), () -> {
			return dependencies.stream().map(d -> Paths.get(d.getPath()).toString()).collect(joining("\n"));
		});

		return dependencies;
	}

	private static Predicate<Path> distinctByName() {
		Set<String> seen = newSetFromMap(new ConcurrentHashMap<>());
		return file -> seen.add(file.getFileName().toString());
	}

	private static Dependency createDependency(Path file) {
		// Maven does not seem to care about groupId, artifactId and version for system scope dependencies.
		// The only important thing is systemPath, let's just use random unique strings for the rest.
		return new Dependency(randomString(), randomString(), randomString(), file.toAbsolutePath().toString());
	}

	private static String randomString() {
		return UUID.randomUUID().toString();
	}

	private static PathMatcher createNameMatcher() {
		String names = String.join(",", REQUIRED_ARTIFACT_NAMES);
		String namePattern = "*{" + names + "}*[!-test].jar";
		return FileSystems.getDefault().getPathMatcher("glob:" + namePattern);
	}

	private static MavenCli newMavenCli(EventSpy eventSpy) {
		return new MavenCli() {
			@Override
			protected void customizeContainer(PlexusContainer container) {
				super.customizeContainer(container);
				registerEventSpy(eventSpy, container);
			}
		};
	}

	private static void registerEventSpy(EventSpy eventSpy, PlexusContainer container) {
		try {
			EventSpyDispatcher eventSpyDispatcher = container.lookup(EventSpyDispatcher.class);
			List<EventSpy> eventSpies = new ArrayList<>(eventSpyDispatcher.getEventSpies());
			eventSpies.add(eventSpy);
			eventSpyDispatcher.setEventSpies(eventSpies);
		}
		catch (ComponentLookupException e) {
			throw new RuntimeException(e);
		}
	}

	private static Path resourceFile(String name) {
		Path file = Paths.get(".", "src", "test", "resources", "surefire-tests", name);
		assertTrue(Files.isRegularFile(file));
		return file;
	}
}
