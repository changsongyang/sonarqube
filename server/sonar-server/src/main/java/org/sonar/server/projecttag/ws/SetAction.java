/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

package org.sonar.server.projecttag.ws;

import java.util.List;
import java.util.Locale;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class SetAction implements ProjectTagsWsAction {
  /**
   * The characters allowed in project tags are lower-case
   * letters, digits, plus (+), sharp (#), dash (-) and dot (.)
   */
  private static final String VALID_TAG_REGEXP = "[a-z0-9+#\\-.]+$";
  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_TAGS = "tags";

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;

  public SetAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("set")
      .setDescription("Set tags on a project.<br>" +
        "Requires the following permission: 'Administer' rights on the specified project")
      .setSince("6.4")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_TAGS)
      .setDescription("Comma-separated list of tags")
      .setRequired(true)
      .setExampleValue("finance, offshore");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    List<String> tags = request.mandatoryParamAsStrings(PARAM_TAGS).stream()
      .filter(StringUtils::isNotBlank)
      .map(t -> t.toLowerCase(Locale.ENGLISH))
      .map(SetAction::checkTag)
      .distinct()
      .collect(Collectors.toList());

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = componentFinder.getByKey(dbSession, projectKey);
      checkRequest(project.isRootProject(), "Component must be a project");
      userSession.checkComponentUuidPermission(UserRole.ADMIN, project.uuid());

      project.setTags(tags);
      dbClient.componentDao().updateTags(dbSession, project);
      dbSession.commit();
    }

    response.noContent();
  }

  private static String checkTag(String tag) {
    checkRequest(tag.matches(VALID_TAG_REGEXP), "Tag '%s' is invalid. Project tags accept only the characters: a-z, 0-9, '+', '-', '#', '.'", tag);
    return tag;
  }
}
