package com.edwin.gitops.client;

import com.edwin.gitops.config.properties.GitOpsProperties;
import com.edwin.gitops.domain.content.Content;
import com.edwin.gitops.domain.pulls.PullRequest;
import com.edwin.gitops.domain.refs.Branch;
import com.edwin.gitops.utils.ContentUtil;
import com.edwin.gitops.utils.HttpUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.springframework.http.*;

import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public class BranchClient {
    private final String NEW_BRANCH_NAME;

    private final RestTemplate restTemplate;

    private final String DEFAULT_BASE_BRANCH;

    private final String PR_BRANCH_NAME;

    private static final String CONTENT_URL = "/contents";
    private static final String GIT_REFS_HEAD_URL = "/git/refs/heads";
    private static final String UPDATE_BY_MESSAGE = "update file, modify tag";

    private static final String PULL_URL = "/pulls";
    private static final String CREATE_PULL_REQUEST_TITLE = "create-pull-request";

    public BranchClient(GitOpsProperties gitOpsProperties, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.NEW_BRANCH_NAME = gitOpsProperties.getNewBranchName();
        this.DEFAULT_BASE_BRANCH = gitOpsProperties.getDefaultBranch();
        this.PR_BRANCH_NAME = gitOpsProperties.getNewBranchName();
    }


    public String getMasterBranchSHA(String baseUrl, String authorization) {
        Branch masterBranch = getMasterBranch(baseUrl, authorization);
        return masterBranch.getObject().getSha();
    }


    public Branch getMasterBranch(String baseUrl, String authorization) {


        String url = baseUrl + GIT_REFS_HEAD_URL + "/" + DEFAULT_BASE_BRANCH;

        HttpHeaders headers = HttpUtil.getHeaders(authorization);

        ResponseEntity<Branch> responseEntity = restTemplate.exchange(url,
                HttpMethod.GET,
                new HttpEntity<String>(headers),
                Branch.class);

        return responseEntity.getBody();
    }

    public void createBranch(String baseUrl, String authorization, String masterBranchSHA) {

        String url = baseUrl + "/git/refs";

        JsonObject payload = new JsonObject();
        payload.add("ref", new JsonPrimitive("refs/heads/" + NEW_BRANCH_NAME));
        payload.add("sha", new JsonPrimitive(masterBranchSHA));

        HttpHeaders headers = HttpUtil.getHeaders(authorization);

        restTemplate.exchange(url
                , HttpMethod.POST,
                new HttpEntity<>(payload.toString(), headers),
                Branch.class);

    }


    public void createBranchByMaster(String baseUrl, String authorization) {
        String masterBranchSHA = getMasterBranchSHA(baseUrl, authorization);
        createBranch(baseUrl, authorization, masterBranchSHA);
    }

    public void deleteBranch(String baseUrl, String authorization) {
        String url = baseUrl + GIT_REFS_HEAD_URL + "/" + NEW_BRANCH_NAME;

        HttpHeaders headers = HttpUtil.getHeaders(authorization);

        restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                new HttpEntity<String>(headers),
                Void.class);
    }


    public Content getContentFileByPath(String baseUrl, String authorization, String repoFilepath) {


        String url = baseUrl + CONTENT_URL + "/" + repoFilepath;

        HttpHeaders headers = HttpUtil.getHeaders(authorization);

        ResponseEntity<Content> contentResponseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<String>(headers),
                Content.class);
        return contentResponseEntity.getBody();
    }


    public PullRequest createPullRequest(String baseUrl, String authorization) {

        String url = baseUrl + PULL_URL;


        JsonObject payload = new JsonObject();
        payload.add("title", new JsonPrimitive(CREATE_PULL_REQUEST_TITLE));
        payload.add("head", new JsonPrimitive(PR_BRANCH_NAME));
        payload.add("base", new JsonPrimitive(DEFAULT_BASE_BRANCH));

        HttpHeaders headers = HttpUtil.getHeaders(authorization);

        ResponseEntity<PullRequest> pullRequestResponseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(payload.toString(), headers),
                PullRequest.class);
        return pullRequestResponseEntity.getBody();

    }

    public void mergePullRequestById(String baseUrl, String authorization, Integer id) {


        String url = baseUrl + PULL_URL + "/" + id + "/merge";

        JsonObject payload = new JsonObject();
        payload.add("commit_message", new JsonPrimitive("merge-message,time by " + Instant.now()));

        HttpHeaders headers = HttpUtil.getHeaders(authorization);

        restTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(payload.toString(), headers),
                Void.class);
    }

    public void createAndMergePullRequest(String baseUrl, String authorization) {
        PullRequest pullRequest = createPullRequest(baseUrl, authorization);
        mergePullRequestById(baseUrl, authorization, pullRequest.getNumber());
    }


    public void updateDeployment(String baseUrl, String authorization, String filePath, Map<String, String> replaceMap) throws IOException {

        updateDeploymentTag(baseUrl, authorization, filePath, replaceMap);
        createAndMergePullRequest(baseUrl, authorization);
        deleteBranch(baseUrl, authorization);
    }


    public void updateDeploymentTag(String baseUrl, String authorization, String repoFilepath, Map<String, String> replaceMap) {


        createBranchByMaster(baseUrl, authorization);

        Content content = getContentFileByPath(baseUrl, authorization, repoFilepath);

        String contentBase64 = ContentUtil.replaceDataToBase64(content, replaceMap);

        String url = baseUrl + CONTENT_URL + "/" + repoFilepath;

        JsonObject payload = new JsonObject();
        payload.add("message", new JsonPrimitive(UPDATE_BY_MESSAGE));
        payload.add("content", new JsonPrimitive(contentBase64));
        payload.add("sha", new JsonPrimitive(content.getSha()));
        payload.add("branch", new JsonPrimitive(NEW_BRANCH_NAME));

        HttpHeaders headers = HttpUtil.getHeaders(authorization);

        restTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(payload.toString(), headers),
                Void.class);
    }


}
