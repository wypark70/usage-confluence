package com.sds.confluence.plugin.usage.job;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sds.confluence.plugin.usage.domain.SystemInfo;
import com.sds.confluence.plugin.usage.domain.UserCount;
import com.sds.confluence.plugin.usage.domain.UserCountRequest;

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
public class UsageConfluenceUserCountJob implements JobRunner {
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @Override
  public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    System.out.println("UsageConfluenceUserCountJob");
    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    System.out.println(jobRunnerRequest.getJobId());
    System.out.println(jobRunnerRequest.getStartTime());
    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    String userCountApiUrl = "http://localhost:8080/vas/api/user-count";
    String userCountApiKey = "user-count-api-key";
    UserCountRequest userCountRequest = new UserCountRequest();
    SystemInfo systemInfo = new SystemInfo();
    systemInfo.setHost("localhost");
    systemInfo.setIp("127.0.0.1");
    systemInfo.setPort("2990");
    systemInfo.setProductCode("JIRA");
    List<UserCount> userCountList = new ArrayList<>();
    UserCount userCount = new UserCount();
    userCount.setLookupTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    userCount.setTotal("1");
    userCount.setUsage("1");
    userCountList.add(userCount);
    userCountRequest.setInfo(systemInfo);
    userCountRequest.setList(userCountList);
    postUserCountReport(userCountApiUrl, userCountApiKey, userCountRequest);
    return JobRunnerResponse.success("Job finished successfully.");
  }

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

      System.out.println(stringBuilder);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
