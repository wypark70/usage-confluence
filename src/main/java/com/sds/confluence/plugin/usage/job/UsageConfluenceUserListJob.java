package com.sds.confluence.plugin.usage.job;

import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.user.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sds.confluence.plugin.usage.config.UsageConfluenceConfig;
import com.sds.confluence.plugin.usage.domain.SystemInfo;
import com.sds.confluence.plugin.usage.domain.UserInfo;
import com.sds.confluence.plugin.usage.domain.UserInfoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@Named
public class UsageConfluenceUserListJob implements JobRunner {
  private static final Logger log = LoggerFactory.getLogger(UsageConfluenceUserListJob.class);
  private static final String CLASS_NAME = UsageConfluenceConfig.class.getName();
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @ComponentImport
  private final PluginSettingsFactory pluginSettingsFactory;
  @ComponentImport
  private final UserAccessor userAccessor;

  @Inject
  public UsageConfluenceUserListJob(PluginSettingsFactory pluginSettingsFactory, UserAccessor userAccessor) {
    this.pluginSettingsFactory = pluginSettingsFactory;
    this.userAccessor = userAccessor;
  }

  @SuppressWarnings("DuplicatedCode")
  @Override
  public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
    log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    log.debug("UsageConfluenceUserListJob");
    log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    log.debug(jobRunnerRequest.getJobId().toString());
    log.debug(jobRunnerRequest.getStartTime().toString());
    log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

    PluginSettings settings = pluginSettingsFactory.createGlobalSettings();

    String userListApiUrl = (String) settings.get(CLASS_NAME + ".userListApiUrl");
    String userListApiKey = (String) settings.get(CLASS_NAME + ".userListApiKey");

    SystemInfo systemInfo = new SystemInfo();
    systemInfo.setHost((String) settings.get(CLASS_NAME + ".host"));
    systemInfo.setIp((String) settings.get(CLASS_NAME + ".ip"));
    systemInfo.setPort((String) settings.get(CLASS_NAME + ".port"));
    systemInfo.setProductCode((String) settings.get(CLASS_NAME + ".productCode"));

    List<UserInfo> userInfoList = new ArrayList<>();
    List<User> userList = userAccessor.getUsersWithConfluenceAccessAsList();
    String lookupTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    String tenantCode = (String) settings.get(CLASS_NAME + ".tenantCode");
    userList.forEach(user -> {
      UserInfo userInfo = new UserInfo();
      userInfo.setLookupTime(lookupTime);
      userInfo.setUserId(user.getEmail());
      userInfo.setUserName(user.getName());
      userInfo.setActiveYn("Y");
      userInfo.setTenantCode(tenantCode);
      userInfoList.add(userInfo);
    });

    UserInfoRequest userInfoRequest = new UserInfoRequest();
    userInfoRequest.setInfo(systemInfo);
    userInfoRequest.setList(userInfoList);

    log.debug("userListApiUrl: " + userListApiUrl);
    log.debug("userListApiKey: " + userListApiKey);
    log.debug(gson.toJson(userInfoRequest));

    try {
      postUserInfoReport(userListApiUrl, userListApiKey, userInfoRequest);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return JobRunnerResponse.success("Job finished successfully.");
  }

  @SuppressWarnings("DuplicatedCode")
  private void postUserInfoReport(String userListApiUrl, String userListApiKey, UserInfoRequest userInfoRequest) throws IOException {
    URL url = new URL(userListApiUrl);
    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

    httpURLConnection.setRequestMethod("POST");
    httpURLConnection.setRequestProperty("Authorization", userListApiKey);
    httpURLConnection.setRequestProperty("Content-Type", "application/json");
    httpURLConnection.setDoInput(true);
    httpURLConnection.setDoOutput(true);

    try (OutputStream outputStream = httpURLConnection.getOutputStream()) {
      byte[] input = gson.toJson(userInfoRequest).getBytes(StandardCharsets.UTF_8);
      outputStream.write(input, 0, input.length);
    }

    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8))) {
      StringBuilder stringBuilder = new StringBuilder();
      String responseLine = null;
      while ((responseLine = bufferedReader.readLine()) != null) {
        stringBuilder.append(responseLine.trim());
      }
      log.debug(stringBuilder.toString());
    }
  }
}
