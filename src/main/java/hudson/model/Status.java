public enum Status {
	CANCELED  			("canceled"),
	CREATED   			("created"),
	FAILED    			("failed"),
	MANUAL    			("manual"),
	RUNNING   			("running"),
	SCHEDULED 			("schedule"),
	SKIPPED   			("skipped"),
	SUCCESS   			("success"),
	SUCCESS_WARNING  	("passwd with warnings");

    private final String text;

    Status(final String text) {
        this.text = text;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return text;
    }
}