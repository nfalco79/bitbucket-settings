package com.github.nfalco79.bitbucket.reposettings.util;

import com.github.nfalco79.bitbucket.client.model.Webhook;

public class WebhookUtil {

    private static final String JENKINS_WEBHOOK_NAME = "Jenkins hook";
    private static final String JENKINS_WEBHOOK_ALIAS = "Jenkins";
    private static final String JENKINS_WEBHOOK_URL = "%s/bitbucket-scmsource-hook/notify";

    public static final String[] JENKINS_WEBHOOKS_NAMES = { JENKINS_WEBHOOK_NAME, JENKINS_WEBHOOK_ALIAS };

    public static Webhook getDefault(String jenkinsURL) {
        Webhook webhook = new Webhook();
        webhook.setUrl(String.format(JENKINS_WEBHOOK_URL, jenkinsURL));
        webhook.setDescription(JENKINS_WEBHOOK_NAME);
        webhook.getEvents().add(Webhook.REPO_PUSH);
        webhook.getEvents().add(Webhook.PULLREQUEST_CREATED);
        webhook.getEvents().add(Webhook.PULLREQUEST_UPDATED);
        webhook.getEvents().add(Webhook.PULLREQUEST_FULFILLED);
        webhook.getEvents().add(Webhook.PULLREQUEST_REJECTED);
        return webhook;
    }

}