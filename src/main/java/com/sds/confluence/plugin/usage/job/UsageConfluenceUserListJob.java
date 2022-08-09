package com.sds.confluence.plugin.usage.job;

import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.inject.Named;


@Named
public class UsageConfluenceUserListJob implements JobRunner {
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @Override
  public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    System.out.println("UsageConfluenceUserListJob");
    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    System.out.println(jobRunnerRequest.getJobId());
    System.out.println(jobRunnerRequest.getStartTime());
    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    return JobRunnerResponse.success("Job finished successfully.");
  }
}
