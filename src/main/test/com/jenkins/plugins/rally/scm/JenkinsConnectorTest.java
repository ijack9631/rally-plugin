package com.jenkins.plugins.rally.scm;

import com.jenkins.plugins.rally.config.AdvancedConfiguration;
import com.jenkins.plugins.rally.config.BuildConfiguration;
import com.jenkins.plugins.rally.config.ScmConfiguration;
import com.jenkins.plugins.rally.connector.RallyDetailsDTO;
import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JenkinsConnectorTest {
    @Test
    public void shouldCaptureChangesSinceLastBuild() throws Exception {
        String timestamp = "1970-01-01 00:00:00+0000";

        final ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        when(entry.getCommitId()).thenReturn("12345");

        @SuppressWarnings("unchecked")
        ChangeLogSet changeLogSet = new ChangeLogSet(null) {
            @Override
            public boolean isEmptySet() {
                return false;
            }

            public Iterator iterator() {
                return Collections.singletonList(entry).iterator();
            }
        };

        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getPreviousBuild()).thenReturn(null);
        when(build.getNumber()).thenReturn(5);
        when(build.getTimestampString2()).thenReturn(timestamp);
        when(build.getChangeSet()).thenReturn(changeLogSet);

        ScmConnector connector = new JenkinsConnector();
        connector.setScmConfiguration(new ScmConfiguration(null, null));
        connector.setBuildConfiguration(new BuildConfiguration("SinceLastBuild"));
        connector.setAdvancedConfiguration(new AdvancedConfiguration("http://some.url/", "false"));

        List<RallyDetailsDTO> detailsList = connector.getChanges(build, null);

        assertThat(detailsList, hasSize(1));
    }

    @Test
    public void shouldCaptureChangesSinceLastSuccessfulBuild() throws Exception {
        String timestamp = "1970-01-01 00:00:00+0000";

        final ChangeLogSet.Entry firstEntry = mock(ChangeLogSet.Entry.class);
        when(firstEntry.getCommitId()).thenReturn("12345");

        final ChangeLogSet.Entry secondEntry = mock(ChangeLogSet.Entry.class);
        when(secondEntry.getCommitId()).thenReturn("12345");

        @SuppressWarnings("unchecked")
        ChangeLogSet firstChangeLogSet = new ChangeLogSet(null) {
            @Override
            public boolean isEmptySet() {
                return false;
            }

            public Iterator iterator() {
                return Collections.singletonList(firstEntry).iterator();
            }
        };

        @SuppressWarnings("unchecked")
        ChangeLogSet secondChangeLogSet = new ChangeLogSet(null) {
            @Override
            public boolean isEmptySet() {
                return false;
            }

            public Iterator iterator() {
                return Collections.singletonList(secondEntry).iterator();
            }
        };

        AbstractBuild lastSuccessfulBuild = mock(AbstractBuild.class);
        when(lastSuccessfulBuild.getPreviousBuild()).thenReturn(null);
        when(lastSuccessfulBuild.getNumber()).thenReturn(5);
        when(lastSuccessfulBuild.getTimestampString2()).thenReturn(timestamp);
        when(lastSuccessfulBuild.getChangeSet()).thenReturn(secondChangeLogSet);

        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getPreviousBuild()).thenReturn(lastSuccessfulBuild);
        when(build.getNumber()).thenReturn(5);
        when(build.getTimestampString2()).thenReturn(timestamp);
        when(build.getChangeSet()).thenReturn(firstChangeLogSet);

        ScmConnector connector = new JenkinsConnector();
        connector.setScmConfiguration(new ScmConfiguration(null, null));
        connector.setBuildConfiguration(new BuildConfiguration("SinceLastSuccessfulBuild"));
        connector.setAdvancedConfiguration(new AdvancedConfiguration("http://some.url/", "false"));

        List<RallyDetailsDTO> detailsList = connector.getChanges(build, null);

        assertThat(detailsList, hasSize(2));
    }
}