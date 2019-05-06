package com.electriccloud.plugin.spec.Bamboo

import com.electriccloud.plugin.spec.http.*
import net.sf.json.JSON
import net.sf.json.JSONArray
import org.apache.http.impl.client.DefaultHttpClient

class Bamboo {
    final static String API_BASE = '/rest/api/latest/'

    RestClient client

    String url
    String username
    String password

    Bamboo(String url, String username, String password) {
        this.url = url
        this.username = username
        this.password = password

        // Instantiate client
        DefaultHttpClient httpclient = new DefaultHttpClient()
        ICredentials credentials = new HeaderCredentials(username, password)
        client = new RestClient(httpclient, credentials, URI.create(this.url))
    }

    JSON getPlanJSON(String projectKey, String planKey) {
        String path = 'plan/' + projectKey + '-' + planKey
        return request(RestClient.METHOD_GET, path, [expand: 'stages.stage'])
    }

    Plan getPlan(String projectKey, String planKey) {
        return new Plan(this, getPlanJSON(projectKey, planKey))
    }

    Plan[] getAllPlans(String projectKey = null) {
        if (projectKey != null) {
            return getAllPlansForProject(projectKey)
        }

        String path = 'project'
        String expand = 'projects.project.plans'

        JSON responseJSON = request(RestClient.METHOD_GET, path, [expand: expand])

        Plan[] result = []
        responseJSON['projects']['project'].each { JSONArray project ->
            project['plans']['plan'].each { plan ->
                result.push(new Plan(this, (JSON) plan))
            }
        }

        return result
    }

    Plan[] getAllPlansForProject(String projectKey) {
        String path = 'project/' + projectKey
        String expand = 'plans.plan'

        JSON responseJSON = request(RestClient.METHOD_GET, path, [expand: expand])

        Plan[] result = []

        responseJSON['plans']['plan'].each { plan ->
            result.push(new Plan(this, (JSON) plan))
        }

        return result
    }

    JSON request(String method, String path, Map parameters = [:], JSON payload = null) {
        assert method
        assert path

        if (!path =~ /^\//) {
            path = path.replaceAll(/^\/+/, '')
        }
        path = API_BASE + path

        def url_with_params = this.client.buildURI(path, parameters)

        if (payload && !(method == RestClient.METHOD_PUT || method == RestClient.METHOD_POST)) {
            throw new RuntimeException("Payload is implemented only for PUT and POST methods")
        }

        JSON result = null

        try {
            // Adding parameters to URI
            switch (method.toUpperCase()) {
                case RestClient.METHOD_GET:
                    result = this.client.get(url_with_params)
                    break
                case RestClient.METHOD_POST:
                    result = this.client.post(url_with_params, payload)
                    break
                case RestClient.METHOD_PUT:
                    result = this.client.put(url_with_params, payload)
                    break
                case RestClient.METHOD_PATCH:
                    throw new RuntimeException("PATCH method is not implemented.")
                    result = this.client.patch(url_with_params)
                    break
                case RestClient.METHOD_DELETE:
                    result = this.client.delete(url_with_params)
                    break
            }
        }
        catch (RestException e) {
            println "Error happened for the request" + e.getHttpStatusCode()
            println e.getMessage()
        }

        assert result
        return result
    }


/*    JSON postWithContentType(String path, Map queryParameters = [:], JSON payload, String contentType) {
        assert queryParameters['api-version']
        URI uri = this.client.buildURI(path, queryParameters)

        HttpPost request = new HttpPost(uri)

        println("[DEBUG] HELPER REQUEST PATH: " + uri)

        JSON result = null
        try {
            result = this.client.request(request, payload.toString(), contentType)
        } catch (RestException e) {
            println "Error happened for the request" + e.getHttpStatusCode()
            println e.getMessage()
        }
        assert result
        return result
    }*/


}
