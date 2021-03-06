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

package org.sonar.server.computation.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.server.computation.AnalysisReportQueue;

import java.util.List;

/**
 * @since 5.0
 */
public class ActiveAnalysisReportsAction implements RequestHandler {
  private final AnalysisReportQueue queue;

  public ActiveAnalysisReportsAction(AnalysisReportQueue queue) {
    this.queue = queue;
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    List<AnalysisReportDto> reports = queue.all();

    JsonWriter json = response.newJsonWriter().beginObject();
    writeReports(reports, json);
    json.endObject();
    json.close();
  }

  private void writeReports(List<AnalysisReportDto> reports, JsonWriter json) {
    json.name("reports").beginArray();
    for (AnalysisReportDto report : reports) {
      json.beginObject();
      json.prop("id", report.getId());
      json.prop("project", report.getProjectKey());
      json.prop("projectName", report.getProjectKey());
      json.propDateTime("startedAt", report.getStartedAt());
      json.propDateTime("finishedAt", report.getFinishedAt());
      json.propDateTime("submittedAt", report.getCreatedAt());
      json.prop("status", report.getStatus().toString());
      json.endObject();
    }
    json.endArray();
  }

  void define(WebService.NewController controller) {
    controller
      .createAction("active")
      .setDescription("List all the active analysis reports")
      .setSince("5.0")
      .setInternal(true)
      .setHandler(this);
  }

}
