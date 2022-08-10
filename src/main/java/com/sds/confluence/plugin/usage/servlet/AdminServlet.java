package com.sds.confluence.plugin.usage.servlet;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.webresource.api.assembler.PageBuilderService;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Scanned
public class AdminServlet extends HttpServlet {

  private static final String ADMIN_TEMPLATE = "/templates/admin.vm";

  @ComponentImport
  private final PageBuilderService pageBuilderService;
  @ComponentImport
  private final TemplateRenderer templateRenderer;
  @ComponentImport
  private final UserManager userManager;

  @Inject
  public AdminServlet(PageBuilderService pageBuilderService, TemplateRenderer templateRenderer, UserManager userManager) {
    this.pageBuilderService = pageBuilderService;
    this.templateRenderer = templateRenderer;
    this.userManager = userManager;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    UserProfile userProfile = userManager.getRemoteUser();
    boolean isAdmin = userProfile != null && userManager.isAdmin(userProfile.getUserKey());

    if (!isAdmin) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    pageBuilderService.assembler().resources().requireWebResource("com.sds.confluence.plugin.usage-confluence:admin-web-resources").requireContext("atl.admin");
    resp.setContentType("text/html;charset=utf-8");

    templateRenderer.render(ADMIN_TEMPLATE, resp.getWriter());
  }

}
