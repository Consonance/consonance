package info.pancancer.arch3.beans;

import java.util.Map;
import java.util.UUID;

/**
 * Created by boconnor on 2015-04-22.
 */
public class Job {

    String orderUUID = UUID.randomUUID().toString().toLowerCase();
    String workflow;
    String workflowVersion;
    String jobHash;
    Map<String, String> ini;

    public Job(String workflow, String workflowVersion, String jobHash, Map<String, String>  ini) {
        this.workflow = workflow;
        this.workflowVersion = workflowVersion;
        this.jobHash = jobHash;
        this.ini = ini;

    }

    public String toJSON () {

        String j = "{" +
                "    \"job_hash\": \"<hash>\",\n" +
                        "    \"workflow_name\": \"Sanger\",\n" +
                        "    \"workflow_version\" : \"1.0.1\",\n" +
                        "    \"arguments\" : {\n" +
                        "      \"param1\": \"bar\",\n" +
                        "      \"param2\": \"1928\",\n" +
                        "      \"param3\": \"abc\"\n" +
                        "    }\n";
        return j;
    }

}
