package checks;

import org.springframework.transaction.annotation.Transactional;

class AvoidSwallowingExceptionInTransactionalMethodCheck {

  @Transactional // Noncompliant {{Do not swallow exceptions in @Transactional methods without rethrowing or setting rollback-only.}}
  public boolean noncompliantSwallowAndSuccess() {
    try {
      return doInsert();
    } catch (Exception e) {
      return true;
    }
  }

  @Transactional // Noncompliant {{Do not swallow exceptions in @Transactional methods without rethrowing or setting rollback-only.}}
  public void noncompliantSwallowWithoutReturnSuccess() {
    try {
      doInsert();
    } catch (Throwable e) {
      log(e);
    }
  }

  @Transactional
  public boolean compliantRethrow() {
    try {
      return doInsert();
    } catch (Exception e) {
      throw e;
    }
  }

  @Transactional
  public boolean compliantSetRollbackOnly() {
    try {
      return doInsert();
    } catch (Exception e) {
      setRollbackOnly();
      return false;
    }
  }

  private boolean doInsert() {
    return true;
  }

  private void setRollbackOnly() {
    // simulate TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
  }

  private void log(Throwable e) {
    // no-op
  }
}
