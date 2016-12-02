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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;

public class AnalyzeChangedFilesJob extends Job {
  private final Collection<IProject> projects;

  public AnalyzeChangedFilesJob(Collection<IProject> projects) {
    super("Analyze changeset");
    this.projects = projects;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    projects.stream().forEach(MarkerUtils::deleteChangeSetIssuesMarkers);
    List<Job> jobs = new ArrayList<>();
    for (IProject project : projects) {
      AnalyzeProjectRequest req = new AnalyzeProjectRequest(project, collectChangedFiles(project, monitor), TriggerType.CHANGESET);
      AnalyzeProjectJob job = new AnalyzeProjectJob(req);
      job.schedule();
      jobs.add(job);
    }
    jobs.stream().forEach(j -> {
      try {
        j.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    return Status.OK_STATUS;
  }

  private Collection<IFile> collectChangedFiles(IProject project, IProgressMonitor monitor) {
    RepositoryProvider provider = RepositoryProvider.getProvider(project);
    if (provider == null) {
      return Collections.emptyList();
    }

    Collection<IFile> changedFiles = new ArrayList<>();

    Subscriber subscriber = provider.getSubscriber();

    // Allow the subscriber to refresh its state
    try {
      subscriber.refresh(subscriber.roots(), IResource.DEPTH_INFINITE, monitor);

      // Collect all the synchronization states and print
      IResource[] children = subscriber.roots();
      for (int i = 0; i < children.length; i++) {
        collect(subscriber, children[i], changedFiles);
      }
    } catch (TeamException e) {
      throw new IllegalStateException("Unable to analyze changed files", e);
    }
    return changedFiles;
  }

  void collect(Subscriber subscriber, IResource resource, Collection<IFile> changedFiles) throws TeamException {
    IFile file = (IFile) resource.getAdapter(IFile.class);
    if (file != null) {
      SyncInfo syncInfo = subscriber.getSyncInfo(resource);
      if (!SyncInfo.isInSync(syncInfo.getKind())) {
        changedFiles.add(file);
      }
    }
    for (IResource child : subscriber.members(resource)) {
      collect(subscriber, child, changedFiles);
    }
  }
}
