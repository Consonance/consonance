package io.swagger.client.api;

import io.swagger.client.ApiException;
import io.swagger.client.ApiClient;
import io.swagger.client.Configuration;
import io.swagger.client.Pair;
import io.swagger.client.TypeRef;

import io.swagger.client.model.*;

import java.util.*;

import io.swagger.client.model.Job;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaClientCodegen", date = "2015-09-22T20:16:20.394Z")
public class JobApi {
  private ApiClient apiClient;

  public JobApi() {
    this(Configuration.getDefaultApiClient());
  }

  public JobApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  
  /**
   * List all known jobs
   * List all jobs
   * @return List<Job>
   */
  public List<Job> listWorkflowRuns () throws ApiException {
    Object postBody = null;
    byte[] postBinaryBody = null;
    
    // create path and map variables
    String path = "/job".replaceAll("\\{format\\}","json");

    // query params
    List<Pair> queryParams = new ArrayList<Pair>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, Object> formParams = new HashMap<String, Object>();

    

    

    

    final String[] accepts = {
      "application/json"
    };
    final String accept = apiClient.selectHeaderAccept(accepts);

    final String[] contentTypes = {
      
    };
    final String contentType = apiClient.selectHeaderContentType(contentTypes);

    String[] authNames = new String[] {  };

    

    
    
    TypeRef returnType = new TypeRef<List<Job>>() {};
    return apiClient.invokeAPI(path, "GET", queryParams, postBody, postBinaryBody, headerParams, formParams, accept, contentType, authNames, returnType);
    
    


  }
  
  /**
   * Schedule a new workflow run
   * 
   * @param body Workflow run that needs to be added to the store
   * @return Job
   */
  public Job addWorkflowRun (Job body) throws ApiException {
    Object postBody = body;
    byte[] postBinaryBody = null;
    
     // verify the required parameter 'body' is set
     if (body == null) {
        throw new ApiException(400, "Missing the required parameter 'body' when calling addWorkflowRun");
     }
     
    // create path and map variables
    String path = "/job".replaceAll("\\{format\\}","json");

    // query params
    List<Pair> queryParams = new ArrayList<Pair>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, Object> formParams = new HashMap<String, Object>();

    

    

    

    final String[] accepts = {
      "application/json"
    };
    final String accept = apiClient.selectHeaderAccept(accepts);

    final String[] contentTypes = {
      
    };
    final String contentType = apiClient.selectHeaderContentType(contentTypes);

    String[] authNames = new String[] {  };

    

    
    
    TypeRef returnType = new TypeRef<Job>() {};
    return apiClient.invokeAPI(path, "POST", queryParams, postBody, postBinaryBody, headerParams, formParams, accept, contentType, authNames, returnType);
    
    


  }
  
  /**
   * List all jobs owned by the logged-in user
   * List the jobs owned by the user
   * @return List<Job>
   */
  public List<Job> listOwnedWorkflowRuns () throws ApiException {
    Object postBody = null;
    byte[] postBinaryBody = null;
    
    // create path and map variables
    String path = "/job/listOwned".replaceAll("\\{format\\}","json");

    // query params
    List<Pair> queryParams = new ArrayList<Pair>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, Object> formParams = new HashMap<String, Object>();

    

    

    

    final String[] accepts = {
      "application/json"
    };
    final String accept = apiClient.selectHeaderAccept(accepts);

    final String[] contentTypes = {
      
    };
    final String contentType = apiClient.selectHeaderContentType(contentTypes);

    String[] authNames = new String[] {  };

    

    
    
    TypeRef returnType = new TypeRef<List<Job>>() {};
    return apiClient.invokeAPI(path, "GET", queryParams, postBody, postBinaryBody, headerParams, formParams, accept, contentType, authNames, returnType);
    
    


  }
  
}