/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.issue;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.BatchComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.db.IssueMapper;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.issue.db.UpdateConflictResolver;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScanIssueStorage extends IssueStorage implements BatchComponent {

  private final ResourceCache resourceCache;
  private final ResourceDao resourceDao;
  private final ProjectTree projectTree;
  private final UpdateConflictResolver conflictResolver = new UpdateConflictResolver();

  public ScanIssueStorage(MyBatis mybatis, RuleFinder ruleFinder, ResourceCache resourceCache, ResourceDao resourceDao, ProjectTree projectTree) {
    super(mybatis, ruleFinder);
    this.resourceCache = resourceCache;
    this.resourceDao = resourceDao;
    this.projectTree = projectTree;
  }

  @Override
  protected void doInsert(DbSession session, long now, DefaultIssue issue) {
    IssueMapper issueMapper = session.getMapper(IssueMapper.class);
    long componentId = componentId(issue);
    long projectId = projectId();
    Rule rule = rule(issue);
    List<String> allTags = new ArrayList<String>();
    allTags.addAll(Arrays.asList(rule.getTags()));
    allTags.addAll(Arrays.asList(rule.getSystemTags()));
    issue.setTags(allTags);
    IssueDto dto = IssueDto.toDtoForBatchInsert(issue, componentId, projectId, rule.getId(), now);
    issueMapper.insert(dto);
  }

  @Override
  protected void doUpdate(DbSession session, long now, DefaultIssue issue) {
    IssueMapper issueMapper = session.getMapper(IssueMapper.class);
    IssueDto dto = IssueDto.toDtoForUpdate(issue, projectId(), now);
    if (Issue.STATUS_CLOSED.equals(issue.status()) || issue.selectedAt() == null) {
      // Issue is closed by scan or changed by end-user
      issueMapper.update(dto);

    } else {
      int count = issueMapper.updateIfBeforeSelectedDate(dto);
      if (count == 0) {
        // End-user and scan changed the issue at the same time.
        // See https://jira.codehaus.org/browse/SONAR-4309
        conflictResolver.resolve(issue, issueMapper);
      }
    }
  }

  @VisibleForTesting
  long componentId(DefaultIssue issue) {
    BatchResource resource = resourceCache.get(issue.componentKey());
    if (resource != null) {
      return resource.resource().getId();
    }

    // Load from db when component does not exist in cache (deleted file for example)
    ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(issue.componentKey()));
    if (resourceDto == null) {
      throw new IllegalStateException("Unknown component: " + issue.componentKey());
    }
    return resourceDto.getId();
  }

  @VisibleForTesting
  long projectId() {
    return projectTree.getRootProject().getId();
  }

}
