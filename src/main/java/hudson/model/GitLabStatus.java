package hudson.model;

/*
 * I don't like this class but I want to be able to group these strings together
 * and access them link 'GitLabStatus.CANCELED since they are all GitLab status
 * strings.
 */
public class GitLabStatus {

    public static final String CANCELED = "canceled";
    public static final String CREATED = "created";
    public static final String EXTENDED = "extended";
    public static final String FACTORY = "factory";
    public static final String FAILED = "failed";
    public static final String MANUAL = "manual";
    public static final String PENDING = "pending";
    public static final String RUNNING = "running";
    public static final String SCHEDULED = "schedule";
    public static final String SKIPPED = "skipped";
    public static final String SUCCESS = "success";
    public static final String SUCCESS_WARNING = "passed with warnings";
}