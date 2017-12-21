package io.swagger.wes.api;

import io.swagger.wes.api.*;
import io.swagger.wes.model.*;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import io.swagger.wes.model.Ga4ghWesServiceInfo;
import io.swagger.wes.model.Ga4ghWesWorkflowListResponse;
import io.swagger.wes.model.Ga4ghWesWorkflowLog;
import io.swagger.wes.model.Ga4ghWesWorkflowRequest;
import io.swagger.wes.model.Ga4ghWesWorkflowRunId;
import io.swagger.wes.model.Ga4ghWesWorkflowStatus;

import java.io.IOException;
import java.util.List;
import io.swagger.wes.api.NotFoundException;

import io.consonance.webservice.core.ConsonanceUser;

import java.io.InputStream;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-09-15T17:06:31.319-07:00")
public abstract class Ga4ghApiService {
    public abstract Response cancelJob(String workflowId, ConsonanceUser user) throws NotFoundException;
    public abstract Response getServiceInfo(ConsonanceUser user) throws NotFoundException;
    public abstract Response getWorkflowLog(String workflowId, ConsonanceUser user) throws NotFoundException;
    public abstract Response getWorkflowStatus(String workflowId, ConsonanceUser user) throws NotFoundException;
    public abstract Response listWorkflows(Long pageSize, String pageToken, String keyValueSearch, ConsonanceUser user) throws NotFoundException;
    public abstract Response runWorkflow(Ga4ghWesWorkflowRequest body, ConsonanceUser user) throws NotFoundException, IOException, TimeoutException;
}
