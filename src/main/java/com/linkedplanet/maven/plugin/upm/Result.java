package com.linkedplanet.maven.plugin.upm;

interface Result {
    String toMessage();

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    class Success implements Result {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public String toMessage() {
            return "Plugin verification succeeded.";
        }
    }

    class Failure implements Result {
        private final String hint;
        private final Throwable cause;

        Failure(String hint, Throwable cause) {
            this.hint = hint;
            this.cause = cause;
        }

        Failure(String hint) {
            this(hint, null);
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public String toMessage() {
            return cause == null
                    ? hint
                    : String.format("%s - Caused by: %s", hint, cause.getMessage());
        }
    }
}
