package info.pancancer.arch3.beans;

import java.util.List;

/**
 * Created by boconnor on 2015-04-22.
 */
public class Provision {

    int cores;
    int memGb;
    int storageGb;
    List<String> ansiblePlaybooks;

    public Provision(int cores, int memGb, int storageGb, List<String> ansiblePlaybooks) {
        this.cores = cores;
        this.memGb = memGb;
        this.storageGb = storageGb;
        this.ansiblePlaybooks = ansiblePlaybooks;
    }


    public String toJSON () {

        String j = "{" +
                "    \"cores\": 8,\n" +
                        "    \"mem_gb\": 25,\n" +
                        "    \"storage_gb\": 1024,\n" +
                        "    \"bindle_profiles_to_run\": [\"<list_of_bindle_profiles_aka_anible_scripts>\"],\n" +
                        "    \"workflow_zips\": [\"http://s3/workflow.zip\"],\n" +
                        "    \"docker_images\": [\"seqware-whitestar\"]\n" +
                        "  }\n";
        return j;
    }

}
