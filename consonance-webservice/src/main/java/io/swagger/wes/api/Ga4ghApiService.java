package io.swagger.api;

import io.swagger.api.*;
import io.swagger.model.*;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import io.swagger.model.Ga4ghWesServiceInfo;
import io.swagger.model.Ga4ghWesWorkflowListResponse;
import io.swagger.model.Ga4ghWesWorkflowLog;
import io.swagger.model.Ga4ghWesWorkflowRequest;
import io.swagger.model.Ga4ghWesWorkflowRunId;
import io.swagger.model.Ga4ghWesWorkflowStatus;

import java.util.List;
import io.swagger.api.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-09-15T17:06:31.319-07:00")
public abstract class Ga4ghApiService {
    public abstract Response cancelJob(String workflowId,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getServiceInfo(SecurityContext securityContext) throws NotFoundException;
    public abstract Response getWorkflowLog(String workflowId,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getWorkflowStatus(String workflowId,SecurityContext securityContext) throws NotFoundException;
    public abstract Response listWorkflows( Long pageSize, String pageToken, String keyValueSearch,SecurityContext securityContext) throws NotFoundException;
    public abstract Response runWorkflow(Ga4ghWesWorkflowRequest body,SecurityContext securityContext) throws NotFoundException;
}
