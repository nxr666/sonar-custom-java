package checks;

import org.slf4j.Logger;

class LogExceptionWithStackTraceCheck {

  private Logger log;

  void noncompliant_case_message_only() {
    try {
      process();
    } catch (Exception e) {
      log.error("process failed: {}", e.getMessage()); // Noncompliant {{When catching Exception, log the stack trace by passing the exception object (for example: log.error("...", e)).}}
    }
  }

  void compliant_case_with_stack() {
    try {
      process();
    } catch (Exception e) {
      log.error("process failed", e);
    }
  }

  void ignored_non_exception_catch() {
    try {
      process();
    } catch (RuntimeException e) {
      log.error("runtime failed: {}", e.getMessage());
    }
  }

  private void process() {
    // no-op
  }
}
