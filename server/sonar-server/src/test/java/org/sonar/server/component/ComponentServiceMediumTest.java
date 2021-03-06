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

package org.sonar.server.component;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class ComponentServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession session;
  ComponentService service;
  ComponentDto project;
  RuleDto rule;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    service = tester.get(ComponentService.class);

    project = ComponentTesting.newProjectDto().setKey("sample:root");
    tester.get(ComponentDao.class).insert(session, project);
    session.commit();

    // project can be seen by anyone
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.get(InternalPermissionService.class).addPermission(new PermissionChange().setComponentKey(project.getKey()).setGroup(DefaultGroups.ANYONE).setPermission(UserRole.USER));

    rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    session.commit();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void get_by_key() throws Exception {
    assertThat(service.getByKey(project.getKey())).isNotNull();
  }

  @Test
  public void get_nullable_by_key() throws Exception {
    assertThat(service.getNullableByKey(project.getKey())).isNotNull();
    assertThat(service.getNullableByKey("unknown")).isNull();
  }

  @Test
  public void get_by_uuid() throws Exception {
    assertThat(service.getByUuid(project.uuid())).isNotNull();
  }

  @Test
  public void get_nullable_by_uuid() throws Exception {
    assertThat(service.getNullableByUuid(project.uuid())).isNotNull();
    assertThat(service.getNullableByUuid("unknown")).isNull();
  }

  @Test
  public void update_project_key() throws Exception {
    ComponentDto file = ComponentTesting.newFileDto(project).setKey("sample:root:src/File.xoo");
    tester.get(ComponentDao.class).insert(session, file);

    session.commit();

    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.ADMIN, project.key(), project.key());
    service.updateKey(project.key(), "sample2:root");
    session.commit();

    // Check project key has been updated
    assertThat(service.getNullableByKey(project.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root")).isNotNull();

    // Check file key has been updated
    assertThat(service.getNullableByKey(file.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root:src/File.xoo")).isNotNull();

    // Check dry run cache have been updated
    assertThat(db.propertiesDao().selectProjectProperties("sample2:root", session)).hasSize(1);
  }

  @Test
  public void update_module_key() throws Exception {
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey("sample:root:module");
    tester.get(ComponentDao.class).insert(session, module);

    ComponentDto file = ComponentTesting.newFileDto(module).setKey("sample:root:module:src/File.xoo");
    tester.get(ComponentDao.class).insert(session, file);

    session.commit();

    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.ADMIN, project.key(), module.key());
    service.updateKey(module.key(), "sample:root2:module");
    session.commit();

    // Project key has not changed
    assertThat(service.getNullableByKey(project.key())).isNotNull();

    // Check module key has been updated
    assertThat(service.getNullableByKey(module.key())).isNull();
    assertThat(service.getNullableByKey("sample:root2:module")).isNotNull();

    // Check file key has been updated
    assertThat(service.getNullableByKey(file.key())).isNull();
    assertThat(service.getNullableByKey("sample:root2:module:src/File.xoo")).isNotNull();

    // Check dry run cache have been updated -> on a module it's the project cache that is updated
    assertThat(db.propertiesDao().selectProjectProperties(project.key(), session)).hasSize(1);
  }

  @Test
  public void update_provisioned_project_key() throws Exception {
    ComponentDto provisionedProject = ComponentTesting.newProjectDto().setKey("provisionedProject");
    tester.get(ComponentDao.class).insert(session, provisionedProject);

    session.commit();

    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.ADMIN, provisionedProject.key(), provisionedProject.key());
    service.updateKey(provisionedProject.key(), "provisionedProject2");
    session.commit();

    // Check project key has been updated
    assertThat(service.getNullableByKey(provisionedProject.key())).isNull();
    assertThat(service.getNullableByKey("provisionedProject2")).isNotNull();

    // Check dry run cache have been updated
    assertThat(db.propertiesDao().selectProjectProperties("provisionedProject2", session)).hasSize(1);
  }

  @Test(expected = ForbiddenException.class)
  public void fail_to_update_project_key_without_admin_permission() throws Exception {
    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.USER, project.key(), project.key());
    service.updateKey(project.key(), "sample2:root");
  }

  @Test
  public void check_module_keys_before_renaming() throws Exception {
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey("sample:root:module");
    tester.get(ComponentDao.class).insert(session, module);

    ComponentDto file = ComponentTesting.newFileDto(module).setKey("sample:root:module:src/File.xoo");
    tester.get(ComponentDao.class).insert(session, file);

    session.commit();

    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.ADMIN, project.key());
    Map<String, String> result = service.checkModuleKeysBeforeRenaming(project.key(), "sample", "sample2");

    assertThat(result).hasSize(2);
    assertThat(result.get("sample:root")).isEqualTo("sample2:root");
    assertThat(result.get("sample:root:module")).isEqualTo("sample2:root:module");
  }

  @Test
  public void check_module_keys_before_renaming_return_duplicate_key() throws Exception {
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey("sample:root:module");
    tester.get(ComponentDao.class).insert(session, module);

    ComponentDto module2 = ComponentTesting.newModuleDto(project).setKey("foo:module");
    tester.get(ComponentDao.class).insert(session, module2);

    session.commit();

    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.ADMIN, project.key());
    Map<String, String> result = service.checkModuleKeysBeforeRenaming(project.key(), "sample:root", "foo");

    assertThat(result).hasSize(2);
    assertThat(result.get("sample:root")).isEqualTo("foo");
    assertThat(result.get("sample:root:module")).isEqualTo("#duplicate_key#");
  }

  @Test(expected = ForbiddenException.class)
  public void fail_to_check_module_keys_before_renaming_without_admin_permission() throws Exception {
    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.USER, project.key(), project.key());
    service.checkModuleKeysBeforeRenaming(project.key(), "sample", "sample2");
  }

  @Test
  public void bulk_update_project_key() throws Exception {
    ComponentDto module = ComponentTesting.newModuleDto(project).setKey("sample:root:module");
    tester.get(ComponentDao.class).insert(session, module);

    ComponentDto file = ComponentTesting.newFileDto(module).setKey("sample:root:module:src/File.xoo");
    tester.get(ComponentDao.class).insert(session, file);

    session.commit();

    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.ADMIN, project.key());
    service.bulkUpdateKey(project.key(), "sample", "sample2");
    session.commit();

    // Check project key has been updated
    assertThat(service.getNullableByKey(project.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root")).isNotNull();

    // Check module key has been updated
    assertThat(service.getNullableByKey(module.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root:module")).isNotNull();

    // Check file key has been updated
    assertThat(service.getNullableByKey(file.key())).isNull();
    assertThat(service.getNullableByKey("sample2:root:module:src/File.xoo")).isNotNull();

    // Check dry run cache have been updated
    assertThat(db.propertiesDao().selectProjectProperties("sample2:root", session)).hasSize(1);
  }

  @Test
  public void bulk_update_provisioned_project_key() throws Exception {
    ComponentDto provisionedProject = ComponentTesting.newProjectDto().setKey("provisionedProject");
    tester.get(ComponentDao.class).insert(session, provisionedProject);

    session.commit();

    MockUserSession.set().setLogin("john").addComponentPermission(UserRole.ADMIN, provisionedProject.key(), provisionedProject.key());
    service.bulkUpdateKey(provisionedProject.key(), "provisionedProject", "provisionedProject2");
    session.commit();

    // Check project key has been updated
    assertThat(service.getNullableByKey(provisionedProject.key())).isNull();
    assertThat(service.getNullableByKey("provisionedProject2")).isNotNull();

    // Check dry run cache have been updated
    assertThat(db.propertiesDao().selectProjectProperties("provisionedProject2", session)).hasSize(1);
  }

  @Test(expected = ForbiddenException.class)
  public void fail_to_bulk_update_project_key_without_admin_permission() throws Exception {
    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.USER, project.key());
    service.bulkUpdateKey("sample:root", "sample", "sample2");
  }

}
