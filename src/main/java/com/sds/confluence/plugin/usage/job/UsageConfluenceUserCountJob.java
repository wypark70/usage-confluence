package com.sds.confluence.plugin.usage.job;

import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.crowd.util.UserUtils;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sds.confluence.plugin.usage.config.UsageConfluenceConfig;
import com.sds.confluence.plugin.usage.domain.SystemInfo;
import com.sds.confluence.plugin.usage.domain.UserCount;
import com.sds.confluence.plugin.usage.domain.UserCountRequest;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@Named
@Slf4j
public class UsageConfluenceUserCountJob implements JobRunner {
  private static final String CLASS_NAME = UsageConfluenceConfig.class.getName();
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @ComponentImport
  private final PluginSettingsFactory pluginSettingsFactory;
  @ComponentImport
  private final UserAccessor userAccessor;


  @Inject
  public UsageConfluenceUserCountJob(PluginSettingsFactory pluginSettingsFactory, UserAccessor userAccessor) {
    this.pluginSettingsFactory = pluginSettingsFactory;
    this.userAccessor = userAccessor;
  }


  @SuppressWarnings("DuplicatedCode")
  @Override
  public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
    log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    log.debug("UsageConfluenceUserCountJob");
    log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    log.debug(jobRunnerRequest.getJobId().toString());
    log.debug(jobRunnerRequest.getStartTime().toString());
    log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

    PluginSettings settings = pluginSettingsFactory.createGlobalSettings();

    String userCountApiUrl = (String) settings.get(CLASS_NAME + ".userCountApiUrl");
    String userCountApiKey = (String) settings.get(CLASS_NAME + ".userCountApiKey");

    SystemInfo systemInfo = new SystemInfo();
    systemInfo.setHost((String) settings.get(CLASS_NAME + ".host"));
    systemInfo.setIp((String) settings.get(CLASS_NAME + ".ip"));
    systemInfo.setPort((String) settings.get(CLASS_NAME + ".port"));
    systemInfo.setProductCode((String) settings.get(CLASS_NAME + ".productCode"));


    List<UserCount> userCountList = new ArrayList<>();
    UserCount userCount = new UserCount();
    int licenseConsumingUsers = userAccessor.countLicenseConsumingUsers();
    userCount.setLookupTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    userCount.setTotal(String.valueOf(licenseConsumingUsers));
    userCount.setUsage(String.valueOf(licenseConsumingUsers));
    userCountList.add(userCount);

    UserCountRequest userCountRequest = new UserCountRequest();
    userCountRequest.setInfo(systemInfo);
    userCountRequest.setList(userCountList);

    log.debug("userCountApiUrl: " + userCountApiUrl);
    log.debug("userCountApiKey: " + userCountApiKey);
    log.debug(gson.toJson(userCountRequest));

    postUserCountReport(userCountApiUrl, userCountApiKey, userCountRequest);

    return JobRunnerResponse.success("Job finished successfully.");
  }

  @SuppressWarnings("DuplicatedCode")
  private void postUserCountReport(String userCountApiUrl, String userCountApiKey, UserCountRequest userCountRequest) {
    try {
      URL url = new URL(userCountApiUrl);
      HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

      httpURLConnection.setRequestMethod("POST");
      httpURLConnection.setRequestProperty("Authorization", userCountApiKey);
      httpURLConnection.setRequestProperty("Content-Type", "application/json");
      httpURLConnection.setDoInput(true);
      httpURLConnection.setDoOutput(true);

      BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(httpURLConnection.getOutputStream()));
      bufferedWriter.write(gson.toJson(userCountRequest));
      bufferedWriter.flush();
      bufferedWriter.close();

      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
      StringBuilder stringBuilder = new StringBuilder();
      String line = null;

      while ((line = bufferedReader.readLine()) != null) {
        stringBuilder.append(line);
      }

      log.debug(stringBuilder.toString());

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
