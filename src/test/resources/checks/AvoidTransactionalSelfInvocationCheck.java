package checks;

import org.springframework.transaction.annotation.Transactional;

class AvoidTransactionalSelfInvocationCheck {

  @Transactional
  public void txMethod() {
    // write op
  }

  public void caller() {
    txMethod(); // Noncompliant {{Self-invocation bypasses Spring proxy; @Transactional on the called method may not take effect.}}
    this.txMethod(); // Noncompliant {{Self-invocation bypasses Spring proxy; @Transactional on the called method may not take effect.}}
  }
}
