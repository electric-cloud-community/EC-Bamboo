package com.electriccloud.plugin.spec.Bamboo

import com.electriccloud.plugin.spec.http.RestClient
import net.sf.json.JSON

class Plan {
    private Bamboo client
    private JSON planJSON

    private String projectKey
    private String planKey

    Plan(Bamboo client, String projectKey, String planKey) {
        JSON planJSON = client.getPlanJSON(projectKey, planKey)
        this(client, planJSON)
    }

    Plan(Bamboo client, JSON planJSON) {
        this.client = client
        updateInfo(planJSON)
    }

    void refresh() {
        updateInfo(client.getPlanJSON(this.projectKey, this.planKey))
    }

    void updateInfo(JSON planJSON) {
        this.planJSON = planJSON

        this.projectKey = planJSON['projectKey']
        this.planKey = planJSON['shortKey']
    }

    void enable() {
        client.request(RestClient.METHOD_POST, "plan/${this.projectKey}-${this.planKey}/enable")
    }

    void disable() {
        client.request(RestClient.METHOD_DELETE, "plan/${this.projectKey}-${this.planKey}/enable")
    }

    void isEnabled() {
        throw new RuntimeException("Not implemented yet")
    }

    JSON getRuns() {
        return client.request(RestClient.METHOD_GET, "result/${this.projectKey}-${this.planKey}")
    }

    JSON getDetails() {
        return this.planJSON
    }

    void queueRun() {
        throw new RuntimeException("Not implemented yet")
    }
}
