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
package org.sonar.core.source.db;

import com.google.common.base.Charsets;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;

public class FileSourceDto {
  private Long id;
  private String projectUuid;
  private String fileUuid;
  private Date createdAt;
  private Date updatedAt;
  private byte[] data;
  private String dataHash;

  public Long getId() {
    return id;
  }

  public FileSourceDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public FileSourceDto setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  public String getFileUuid() {
    return fileUuid;
  }

  public FileSourceDto setFileUuid(String fileUuid) {
    this.fileUuid = fileUuid;
    return this;
  }

  @CheckForNull
  public byte[] getData() {
    return data;
  }

  @CheckForNull
  public String getStringData() {
    return data != null ? new String(data, Charsets.UTF_8) : null;
  }

  public FileSourceDto setData(@Nullable byte[] data) {
    this.data = data;
    return this;
  }

  public FileSourceDto setStringData(@Nullable String data) {
    return setData(data != null ? data.getBytes(Charsets.UTF_8) : null);
  }

  public String getDataHash() {
    return dataHash;
  }

  public FileSourceDto setDataHash(String dataHash) {
    this.dataHash = dataHash;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public FileSourceDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public FileSourceDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}