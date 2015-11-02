package io.consonance.client.cli;

import io.consonance.client.WebClient;
import io.consonance.webservice.ConsonanceWebserviceApplication;
import io.consonance.webservice.ConsonanceWebserviceConfiguration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.swagger.client.api.OrderApi;
import io.swagger.client.model.Job;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Test the main method in conjunction with a real workflow.
 *
 * @author dyuen
 */
public class SystemMainIT {

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();


    @ClassRule
    public static final DropwizardAppRule<ConsonanceWebserviceConfiguration> RULE =
            new DropwizardAppRule<>(ConsonanceWebserviceApplication.class, ResourceHelpers.resourceFilePath("run-fox.yml"));


    @Test
    public void testGetConfiguration() throws Exception {
        Main main = new Main();
        main.setWebClient(SystemClientIT.getWebClient());
        main.runMain(new String[] { "--metadata" });
        // reset system.out
        // check out the output
        assertTrue(systemOutRule.getLog().contains("database"));
    }

    @Test
    public void testQuietGetConfiguration() throws Exception {
        Main main = new Main();
        main.setWebClient(SystemClientIT.getWebClient());
        main.runMain(new String[] { "--quiet", "--metadata" });
        // reset system.out
        // check out the output
        assertTrue(systemOutRule.getLog().contains("database"));
    }

    @Test
    public void testDebugGetConfiguration() throws Exception {
        Main main = new Main();
        main.setWebClient(SystemClientIT.getWebClient());
        main.runMain(new String[] { "--debug", "--metadata" });
        // reset system.out
        // check out the output
        assertTrue(systemOutRule.getLog().contains("database"));
    }

    @Test
    public void testScheduleAndCheckStatus() throws Exception{
        final WebClient webClient = SystemClientIT.getWebClient();

        Main main = new Main();
        main.setWebClient(SystemClientIT.getWebClient());
        final File file = Files.createTempFile("test", "test").toFile();
        main.runMain(new String[] { "run","--flavour","m1.test","--image-descriptor", file.getAbsolutePath() ,
                "--run-descriptor", file.getAbsolutePath(), "--extra-file", "node-engine.cwl="+file.getAbsolutePath()+"=true",
                "--extra-file", "pointless.txt="+file.getAbsolutePath()+"=false"});
        // reset system.out
        // check out the output
        assertTrue(systemOutRule.getLog().contains("job_uuid"));

        OrderApi api = new OrderApi(webClient);
        final List<Job> jobs = api.listWorkflowRuns();
        assertTrue(jobs.size() == 1);
        Job job = jobs.get(0);

        // only the file with keep=true should have been kept
        assertTrue(job.getExtraFiles().size() == 1);

        //reset
        systemOutRule.clearLog();
        // status check the UUID
        main.runMain(new String[] { "status", "--job_uuid", job.getJobUuid() });
        // reset system.out
        // check out the output
        assertTrue(systemOutRule.getLog().contains("job_uuid"));
    }

    @Test
    public void testHTTPScheduleAndCheckStatus() throws Exception{
        final WebClient webClient = SystemClientIT.getWebClient();

        Main main = new Main();
        main.setWebClient(SystemClientIT.getWebClient());
        final File file = Files.createTempFile("test", "test").toFile();
        main.runMain(new String[] { "run","--flavour","m1.test","--image-descriptor", "https://raw.githubusercontent.com/Consonance/consonance/develop/README.md" ,
                "--run-descriptor", "https://raw.githubusercontent.com/Consonance/consonance/develop/README.md"});
        // reset system.out
        // check out the output
        assertTrue(systemOutRule.getLog().contains("job_uuid"));

        OrderApi api = new OrderApi(webClient);
        final List<Job> jobs = api.listWorkflowRuns();
        assertTrue(jobs.size() == 1);
        Job job = jobs.get(0);

        // assert that readme looks ok
        assertTrue(!job.getContainerImageDescriptor().isEmpty());
        assertTrue(!job.getContainerRuntimeDescriptor().isEmpty());

        //reset
        systemOutRule.clearLog();
        // status check the UUID
        main.runMain(new String[] { "status", "--job_uuid", job.getJobUuid() });
        // reset system.out
        // check out the output
        assertTrue(systemOutRule.getLog().contains("job_uuid"));
    }



}
