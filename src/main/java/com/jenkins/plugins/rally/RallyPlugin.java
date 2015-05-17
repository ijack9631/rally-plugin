package com.jenkins.plugins.rally;

import com.jenkins.plugins.rally.config.RallyPluginConfiguration;
import com.jenkins.plugins.rally.connector.AlmConnector;
import com.jenkins.plugins.rally.connector.RallyApi;
import com.jenkins.plugins.rally.connector.RallyConnector;
import com.jenkins.plugins.rally.connector.RallyDetailsDTO;
import com.jenkins.plugins.rally.scm.JenkinsConnector;
import com.jenkins.plugins.rally.scm.ScmConnector;
import com.rallydev.rest.RallyRestApi;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @author Tushar Shinde
 * @author R. Michael Rogers
 */
public class RallyPlugin extends Builder {
    private static final String RALLY_URL = "https://rally1.rallydev.com";
    private static final String APPLICATION_NAME = "RallyConnect";
    private static final String WSAPI_VERSION = "v2.0";

    private final RallyPluginConfiguration config;
    private RallyRestApi restApi;
    private ScmConnector jenkinsConnector;

    @DataBoundConstructor
    public RallyPlugin(RallyPluginConfiguration config) throws RallyException {
        this.config = config;

        initialize();
    }

    private void initialize() throws RallyException {
        try {
            this.restApi = new RallyRestApi(new URI(RALLY_URL), this.config.getRally().getApiKey());
        } catch (URISyntaxException exception) {
            throw new RallyException(exception);
        }
        this.restApi.setWsapiVersion(WSAPI_VERSION);
        this.restApi.setApplicationName(APPLICATION_NAME);

        this.jenkinsConnector = new JenkinsConnector();
        this.jenkinsConnector.setScmConfiguration(this.config.getScm());
        this.jenkinsConnector.setBuildConfiguration(this.config.getBuild());
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        boolean shouldBuildSucceed = true;
        AlmConnector rallyConnector = null;
        PrintStream out = listener.getLogger();

        List<RallyDetailsDTO> detailsList;
        try {
            detailsList = this.jenkinsConnector.getChanges(build, out);
        } catch (RallyException exception) {
            out.println("Unable to retrieve SCM changes from Jenkins: " + exception.getMessage());
            return false;
        }

        try {
            rallyConnector = createRallyConnector();

            for (RallyDetailsDTO details : detailsList) {
                if (!details.getId().isEmpty()) {
                    try {
                        rallyConnector.updateChangeset(details);
                    } catch (Exception e) {
                        out.println("\trally update plug-in error: could not update changeset entry: " + e.getMessage());
                        e.printStackTrace(out);
                        shouldBuildSucceed = false;
                    }

                    try {
                        rallyConnector.updateRallyTaskDetails(details);
                    } catch (Exception e) {
                        out.println("\trally update plug-in error: could not update TaskDetails entry: " + e.getMessage());
                        e.printStackTrace(out);
                        shouldBuildSucceed = false;
                    }
                } else {
                    out.println("Could not update rally due to absence of id in a comment " + details.getMsg());
                }
            }
        } catch(RallyException e) {
            out.println("Unable to initialize Rally plugin, possibly due to misconfiguration: " + e.getMessage());
            e.printStackTrace(out);
            shouldBuildSucceed = false;
        } finally {
            if (rallyConnector != null) {
                rallyConnector.closeConnection();
            }
        }

        return shouldBuildSucceed;
    }

    private RallyConnector createRallyConnector() throws RallyException {
        RallyConnector connector = new RallyConnector();
        connector.setRallyConfiguration(this.config.getRally());
        connector.setRallyApiInstance((RallyApi) this.restApi);
        connector.setScmConnector(createScmConnector());
        connector.setAdvancedConfiguration(this.config.getAdvanced());

        return connector;
    }

    private ScmConnector createScmConnector() {
        ScmConnector connector = new JenkinsConnector();
        connector.setScmConfiguration(this.config.getScm());

        return connector;
    }

    public RallyPluginConfiguration getConfig() {
        return config;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This get displayed at 'Add build step' button.
         */
        public String getDisplayName() {
            return "Update Rally Task and ChangeSet";
        }
    }
}