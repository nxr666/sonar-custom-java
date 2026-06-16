package checks;

import org.springframework.transaction.annotation.Transactional;

@Transactional // Noncompliant {{Add rollback rule to @Transactional (for example: rollbackFor = Exception.class) to avoid missing rollback on checked exceptions.}}
class RequireRollbackForExceptionInTransactionalCheck {

  @Transactional
  public void noncompliantMethod() { // Noncompliant {{Add rollback rule to @Transactional (for example: rollbackFor = Exception.class) to avoid missing rollback on checked exceptions.}}
    // no-op
  }

  @Transactional(rollbackFor = Exception.class)
  public void compliantWithException() {
    // no-op
  }

  @Transactional(rollbackFor = Throwable.class)
  public void compliantWithThrowable() {
    // no-op
  }
}
