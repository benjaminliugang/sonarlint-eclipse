/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.internal.jobs;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.tracking.Console;
import org.sonarlint.eclipse.core.internal.tracking.IssueTrackerRegistry;
import org.sonarlint.eclipse.core.internal.tracking.ServerIssueTrackable;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;
import org.sonarsource.sonarlint.core.client.api.exceptions.DownloadException;

public class ServerIssueUpdateJob extends AbstractSonarProjectJob {
  public static final String PATH_SEPARATOR_PATTERN = Pattern.quote(File.separator);
  private final IssueTrackerRegistry issueTrackerRegistry;
  private final Console console = new Console();
  private final ServerConfiguration serverConfiguration;
  private final ConnectedSonarLintEngine engine;
  private final String localModuleKey;
  private final String serverModuleKey;
  private final String relativePath;

  public ServerIssueUpdateJob(SonarLintProject project, IssueTrackerRegistry issueTrackerRegistry, ServerConfiguration serverConfiguration, ConnectedSonarLintEngine engine, String localModuleKey, String serverModuleKey, String relativePath) {
    super("SonarLint Server Update", project);
    this.serverConfiguration = serverConfiguration;
    this.engine = engine;
    this.localModuleKey = localModuleKey;
    this.serverModuleKey = serverModuleKey;
    this.relativePath = relativePath;
    this.issueTrackerRegistry = issueTrackerRegistry;
  }

  @Override
  protected IStatus doRun(IProgressMonitor monitor) {
    try {
      ConnectedSonarLintEngine.class.getProtectionDomain().getCodeSource().getLocation();
      Iterator<ServerIssue> serverIssues = fetchServerIssues(serverConfiguration, engine, serverModuleKey, relativePath);
      Collection<Trackable> serverIssuesTrackable = toStream(serverIssues).map(ServerIssueTrackable::new).collect(Collectors.toList());

      issueTrackerRegistry.getOrCreate(localModuleKey).matchAndTrackAsBase(relativePath, serverIssuesTrackable);
    } catch (Throwable t) {
      // note: without catching Throwable, any exceptions raised in the thread will not be visible
      console.error("error while fetching and matching server issues", t);
    }
    return Status.OK_STATUS;
  }

  private <T> Stream<T> toStream(Iterator<T> iterator) {
    Iterable<T> iterable = () -> iterator;
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  private Iterator<ServerIssue> fetchServerIssues(ServerConfiguration serverConfiguration, ConnectedSonarLintEngine engine, String moduleKey, String relativePath) {
    try {
      String fileKey = toFileKey(relativePath);
      return engine.downloadServerIssues(serverConfiguration, moduleKey, fileKey);
    } catch (DownloadException e) {
      console.info(e.getMessage());
      return engine.getServerIssues(moduleKey, relativePath);
    }
  }

  /**
   * Convert relative path to SonarQube file key
   *
   * @param relativePath relative path string in the local OS
   * @return SonarQube file key
   */
  public static String toFileKey(String relativePath) {
    if (File.separatorChar != '/') {
      return relativePath.replaceAll(PATH_SEPARATOR_PATTERN, "/");
    }
    return relativePath;
  }

}
