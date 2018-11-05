# GitLab-pipeline Jenkins Plug-In

The GitLab-pipeline Jenkins Plug-in is designed to allow Jenkins to act as a front-end dashboard for GitLab CI.  GitLab has very limited dashboard capabilities and exposing GitLab CI pipelines to Jenkins allows the richness of the Jenkins dashboard world to be applied to GitLab CI projects.

## Design Overview

The basis for this work is the [External Monitor Job Type][extjob] plug-in and their work is greatfully aknowledged.

Since Jenkins is open-source but GitLab CI is not, the design molds Jenkions around the GitLab concepts and it is worth indicating the mapping between a few of these.


| Jenkins | GitLab CI   | Description                                              |
|---------|-------------|----------------------------------------------------------|
| Job     | Pipeline    | A definition of a pipeline which is run on each check-in |
| Run     | Job (build) | A single instance of a pipeline being run                |

### Jenkins Processing

- Data is HTTP POSTed from GitLab CI to Jenkins via the GitLab CI 'pipeline' webhook
- Jenkins exposes a known HTTP interface at **http(s):/<server-name>/gitlab/pipeline**
- A plug-in configuration option determines whether GitLab CI pipelines define themselves as jobs to Jenkins or whether this has to be done manually.  The reason for this is that Jenkins job names need to be unique but Jenkins cannot stop GitLab CI from definining a pipeline with a name that clashes with an existing Jenkins job.
- GitLab CI pipeline events are POSTed to Jenkins which then creates 'runs' to track the active pipeline.
- Jenkins trackes runs using the GitLab CI checkin ID which is assumed to be unique.
- The following pipeline events are tracked
  - Pipeline creation (depending on configuration)
  - Run being started
  - Run ending.

[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job. There is no need to format nicely because it shouldn't be seen. Thanks SO - http://stackoverflow.com/questions/4823468/store-comments-in-markdown-syntax)

   [extjob]: <https://plugins.jenkins.io/external-monitor-job>
