package io.swagger.workflow.api;

import io.swagger.workflow.api.*;
import io.swagger.workflow.model.*;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import io.swagger.workflow.model.Ga4ghWesServiceInfo;
import io.swagger.workflow.model.Ga4ghWesWorkflowListResponse;
import io.swagger.workflow.model.Ga4ghWesWorkflowLog;
import io.swagger.workflow.model.Ga4ghWesWorkflowRequest;
import io.swagger.workflow.model.Ga4ghWesWorkflowRunId;
import io.swagger.workflow.model.Ga4ghWesWorkflowStatus;

import java.util.List;
import io.swagger.workflow.api.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-12-24T02:51:26.646Z")
public abstract class Ga4ghApiService {
    public abstract Response cancelJob(String workflowId,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getServiceInfo(SecurityContext securityContext) throws NotFoundException;
    public abstract Response getWorkflowLog(String workflowId,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getWorkflowStatus(String workflowId,SecurityContext securityContext) throws NotFoundException;
    public abstract Response listWorkflows( Long pageSize, String pageToken, String keyValueSearch,SecurityContext securityContext) throws NotFoundException;
    public abstract Response runWorkflow(Ga4ghWesWorkflowRequest body,SecurityContext securityContext) throws NotFoundException;
}
